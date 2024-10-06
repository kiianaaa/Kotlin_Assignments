package com.example.lab13

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lab13.ui.theme.Lab13Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class MyViewModel : ViewModel() {
    companion object GattAttributes {
        const val SCAN_PERIOD: Long = 5000  // Increased scan period to 5 seconds for more thorough scanning
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        val UUID_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val UUID_HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val UUID_CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    val scanResults = MutableLiveData<List<ScanResult>>(null)
    val fScanning = MutableLiveData<Boolean>(false)
    private val mResults = java.util.HashMap<String, ScanResult>()
    val mConnectionState = MutableLiveData<Int>(-1)
    val mBPM = MutableLiveData<Int>(0)
    private var mBluetoothGatt: BluetoothGatt? = null

    fun scanDevices(scanner: BluetoothLeScanner) {
        viewModelScope.launch(Dispatchers.IO) {
            fScanning.postValue(true)
            mResults.clear()
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()
            scanner.startScan(null, settings, leScanCallback)
            delay(SCAN_PERIOD)
            scanner.stopScan(leScanCallback)
            scanResults.postValue(mResults.values.toList())
            fScanning.postValue(false)
        }
    }

    fun connectDevice(context: Context, device: BluetoothDevice) {
        Log.d("DBG", "Connect attempt to ${device.name} ${device.address}")
        mConnectionState.postValue(STATE_CONNECTING)
        mBluetoothGatt = device.connectGatt(context, false, mGattCallback)
    }

    fun disconnectDevice() {
        Log.d("DBG", "Disconnect device")
        mBluetoothGatt?.disconnect()
    }

    val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            val deviceAddress = device.address
            mResults[deviceAddress] = result
            Log.d("DBG", "Device address: $deviceAddress (${result.isConnectable})")
        }
    }

    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState.postValue(STATE_CONNECTED)
                Log.i("DBG", "Connected to GATT server")
                Log.i("DBG", "Attempting to start service discovery")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState.postValue(STATE_DISCONNECTED)
                mBPM.postValue(0)
                gatt.disconnect()
                gatt.close()
                Log.i("DBG", "Disconnected from GATT server")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("DBG", "GATT FAILED")
                return
            }

            Log.d("DBG", "onServicesDiscovered()")

            for (gattService in gatt.services) {
                Log.d("DBG", "Service ${gattService.uuid}")
                if (gattService.uuid == UUID_HEART_RATE_SERVICE) {
                    Log.d("DBG", "FOUND Heart Rate Service")

                    val characteristic = gattService.getCharacteristic(UUID_HEART_RATE_MEASUREMENT)
                    if (gatt.setCharacteristicNotification(characteristic, true)) {
                        val descriptor = characteristic.getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG)
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == UUID_HEART_RATE_MEASUREMENT) {
                mBPM.postValue(extractHeartRateValue(characteristic))
            }
        }

        fun extractHeartRateValue(characteristic: BluetoothGattCharacteristic): Int {
            val flag = characteristic.properties
            val format = if (flag and 0x01 != 0) {
                BluetoothGattCharacteristic.FORMAT_UINT16
            } else {
                BluetoothGattCharacteristic.FORMAT_UINT8
            }
            return characteristic.getIntValue(format, 1) ?: -1
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ShowDevices(mBluetoothAdapter: BluetoothAdapter, model: MyViewModel = viewModel()) {
    val context = LocalContext.current
    val value: List<ScanResult>? by model.scanResults.observeAsState(null)
    val fScanning: Boolean by model.fScanning.observeAsState(false)
    val connectionState: Int by model.mConnectionState.observeAsState(-1)
    val bpm: Int by model.mBPM.observeAsState(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { model.scanDevices(mBluetoothAdapter.bluetoothLeScanner) },
            enabled = !fScanning,
            modifier = Modifier
                .padding(8.dp)
                .height(64.dp)
                .width(144.dp)
        ) {
            Text(if (fScanning) "Scanning" else "Scan Now")
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.Gray)

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
            Text(
                text = when (connectionState) {
                    MyViewModel.STATE_CONNECTED -> "Connected"
                    MyViewModel.STATE_CONNECTING -> "Connecting..."
                    MyViewModel.STATE_DISCONNECTED -> "Disconnected"
                    else -> ""
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = when (connectionState) {
                    MyViewModel.STATE_CONNECTED -> Color.Green
                    MyViewModel.STATE_CONNECTING -> Color.Yellow
                    MyViewModel.STATE_DISCONNECTED -> Color.Red
                    else -> Color.Gray
                }
            )

            if (connectionState == MyViewModel.STATE_CONNECTED) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = if (bpm != 0) "$bpm" else "--", fontSize = 36.sp)
                    Text(text = "BPM", fontSize = 12.sp)
                }

                Button(
                    onClick = { model.disconnectDevice() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                ) {
                    Text("Disconnect")
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.Gray)

        if (value.isNullOrEmpty()) {
            Text(text = "No devices found", modifier = Modifier.padding(8.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(value ?: emptyList()) { result ->
                    val deviceName = result.device.name ?: "UNKNOWN"
                    val deviceAddress = result.device.address
                    val deviceStrength = result.rssi

                    Row(
                        modifier = Modifier
                            .padding(2.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.fillMaxWidth().clickable {
                            model.connectDevice(context, result.device)
                        }) {
                            Text(text = deviceName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "$deviceAddress, RSSI: $deviceStrength",
                                fontSize = 14.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private val REQUEST_ENABLE_BT = 1

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Bluetooth
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        setContent {
            Lab13Theme {
                // Check permissions
                if (hasPermissions()) {
                    mBluetoothAdapter?.let {
                        ShowDevices(it)
                    }
                } else {
                    // Permissions denied UI or request handled in hasPermissions()
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Please grant required permissions to use Bluetooth", color = Color.Red)
                    }
                }
            }
        }
    }

    private fun hasPermissions(): Boolean {
        if (mBluetoothAdapter == null || !mBluetoothAdapter!!.isEnabled) {
            Log.e("DBG", "No Bluetooth LE capability")
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ), 1
                )
                return false
            }
        } else {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ), 1
                )
                return false
            }
        }

        return true
    }
}
