package com.example.lab14

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.livedata.observeAsState
import com.example.lab14.ui.theme.Lab14Theme

class DeviceScanViewModel : ViewModel() {
    companion object GattAttributes {
        const val SCAN_PERIOD: Long = 3000
        val HEART_RATE_SERVICE_UUID =
            java.util.UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID =
            java.util.UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID =
            java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    val scanResults = MutableLiveData<List<ScanResult>>(null)
    val isScanning = MutableLiveData<Boolean>(false)
    private val scannedDevices = HashMap<String, ScanResult>()
    private var bluetoothGatt: BluetoothGatt? = null

    // Heart rate measurements
    private val _heartRateData = mutableListOf<Int>()
    val heartRateData: List<Int> get() = _heartRateData

    fun startDeviceScan(scanner: BluetoothLeScanner, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                try {
                    isScanning.postValue(true)
                    val settings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setReportDelay(0)
                        .build()
                    scanner.startScan(null, settings, scanCallback)
                    delay(SCAN_PERIOD)
                    scanner.stopScan(scanCallback)
                    scanResults.postValue(scannedDevices.values.toList())
                    isScanning.postValue(false)
                } catch (e: SecurityException) {
                    Log.e("BT_ERROR", "Permission not granted for Bluetooth scan", e)
                }
            } else {
                Log.e("BT_ERROR", "Bluetooth scan permission not granted")
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice, context: Context) {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback(context))
        } else {
            Log.e("BT_ERROR", "BLUETOOTH_CONNECT permission not granted")
        }
    }

    private fun bluetoothGattCallback(context: Context) = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i("BT_INFO", "Connected to GATT server.")
                        try {
                            gatt.discoverServices()
                        } catch (e: SecurityException) {
                            Log.e("BT_ERROR", "Permission not granted for discoverServices", e)
                        }
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i("BT_INFO", "Disconnected from GATT server.")
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                    }
                }
            } else {
                Log.e("BT_ERROR", "BLUETOOTH_CONNECT permission not granted")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID)
                val heartRateCharacteristic =
                    heartRateService?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)

                heartRateCharacteristic?.let { characteristic ->
                    if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.setCharacteristicNotification(characteristic, true)

                        // Enable notifications
                        val descriptor =
                            characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    } else {
                        Log.e("BT_ERROR", "BLUETOOTH_CONNECT permission not granted")
                    }
                }
            } else {
                Log.w("BT_WARNING", "onServicesDiscovered received: $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                val bpm = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1)
                Log.d("BT_INFO", "BPM: $bpm")
                // Add the received BPM to the list
                _heartRateData.add(bpm)
            }
        }
    }

    val scanCallback: ScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device: BluetoothDevice = result.device
            val deviceAddress = device.address
            scannedDevices[deviceAddress] = result
            Log.d("BT_INFO", "Device address: $deviceAddress (${result.isConnectable})")
        }
    }
}

class MainBluetoothActivity : ComponentActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null

    @RequiresApi(Build.VERSION_CODES.S)
    private fun hasRequiredPermissions(): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e("BT_ERROR", "No Bluetooth LE capability")
            return false
        } else if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADMIN
                ), 1
            )
        }
        return true
    }


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        setContent {
            Lab14Theme  {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        if (hasRequiredPermissions()) {
                            if (bluetoothAdapter == null) {
                                Text("Bluetooth is not supported on this device")
                            } else if (!bluetoothAdapter!!.isEnabled) {
                                Text("Bluetooth is turned OFF")
                            } else {
                                ShowDeviceList(bluetoothAdapter!!)
                            }
                        } else {
                            Text("Please grant Bluetooth permissions.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeartRateDataScreen(heartRateData: List<Int>, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Heart Rate Measurements", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (heartRateData.isEmpty()) {
            Text("No heart rate data available.")
        } else {
            heartRateData.forEach { heartRate ->
                Text("BPM: $heartRate")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
fun WelcomeMessage(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Welcome $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun WelcomeMessagePreview() {
    Lab14Theme {
        WelcomeMessage("User")
    }
}
