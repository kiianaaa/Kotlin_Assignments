package com.example.Lab15

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.livedata.observeAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.*
import java.util.*

class AudioViewModel : ViewModel() {
    val currentAudio = MutableLiveData<String?>(null)
    val isAudioPlaying = MutableLiveData<Boolean>(false)
    val isAudioRecording = MutableLiveData<Boolean>(false)
    val audioFileList = MutableLiveData<List<String>?>(null)

    private val fileExtension = ".raw"
    private val bufferSize = AudioRecord.getMinBufferSize(
        44100,
        AudioFormat.CHANNEL_IN_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private val audioSettings = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(44100)
        .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
        .build()

    private lateinit var audioRecorder: AudioRecord
    private lateinit var audioPlayer: AudioTrack

    fun playAudio(context: Context, fileName: String) {
        isAudioPlaying.postValue(true)
        currentAudio.postValue(fileName)

        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val file = File(storageDir, "$fileName$fileExtension")

        if (!file.exists()) {
            Log.e("AudioApp", "Audio file $fileName$fileExtension not found.")
            return
        }

        val audioData = file.readBytes()
        audioPlayer = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(audioSettings)
            .setBufferSizeInBytes(audioData.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioPlayer.write(audioData, 0, audioData.size)

        viewModelScope.launch(Dispatchers.IO) {
            audioPlayer.play()

            val totalSamples =
                audioData.size / 4 // 2 bytes per sample, stereo has 4 bytes per frame

            while (isAudioPlaying.value == true) {
                val currentPosition = audioPlayer.playbackHeadPosition

                if (currentPosition >= totalSamples) {
                    stopAudio()
                    break
                }
                delay(100) // Check playback progress every 100ms
            }
        }
    }

    fun stopAudio() {
        audioPlayer.stop()
        audioPlayer.release()
        currentAudio.postValue(null)
        isAudioPlaying.postValue(false)
    }

    fun updateAudioFiles(context: Context) {
        audioFileList.value = null
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)

        storageDir?.let {
            val files = it.listFiles()

            files?.let { fileArray ->
                val fileNames = fileArray
                    .map { file -> file.nameWithoutExtension }
                    .sortedBy { it.substringAfter("recording_").toIntOrNull() ?: Int.MAX_VALUE }

                audioFileList.postValue(fileNames)
            }
        }
    }

    private fun generateRecordingFileName(): String {
        val dateFormat = SimpleDateFormat("ddMMyyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())

        return "recording_${dateFormat.format(Date())}${timeFormat.format(Date())}$fileExtension"
    }

    fun deleteAudioFile(context: Context, fileName: String): Boolean {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)

        storageDir?.let {
            val file = File(it, "$fileName$fileExtension")
            val deleted = file.delete()

            if (deleted) {
                updateAudioFiles(context)
                Log.d("AudioApp", "Deleted file: $fileName")
            } else {
                Log.e("AudioApp", "Failed to delete file: $fileName")
            }

            return deleted
        }

        return false
    }

    @SuppressLint("MissingPermission")
    fun startRecording(context: Context) {
        val fileName = generateRecordingFileName()
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val audioFile = File(storageDir, fileName)

        val audioData = ByteArray(bufferSize)
        val fileOutputStream = FileOutputStream(audioFile)
        val dataOutputStream = DataOutputStream(BufferedOutputStream(fileOutputStream))

        audioRecorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )


        if (audioRecorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioApp", "AudioRecord initialization failed.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            audioRecorder.startRecording()
            isAudioRecording.postValue(true)

            try {
                while (audioRecorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val bytesRead = audioRecorder.read(audioData, 0, audioData.size)
                    if (bytesRead > 0) dataOutputStream.write(audioData, 0, bytesRead)
                }
            } catch (e: IOException) {
                Log.e("AudioApp", "Recording error: $e")
            } finally {
                dataOutputStream.close()
            }
        }
    }

    fun stopRecording() {
        audioRecorder.stop()
        audioRecorder.release()
        isAudioRecording.postValue(false)
    }
}

@Composable
fun AudioApp(viewModel: AudioViewModel) {
    val context = LocalContext.current
    val audioFiles by viewModel.audioFileList.observeAsState(emptyList())
    val currentPlaying by viewModel.currentAudio.observeAsState(null)
    val isPlaying by viewModel.isAudioPlaying.observeAsState(false)
    val isRecording by viewModel.isAudioRecording.observeAsState(false)

    LaunchedEffect(Unit) { viewModel.updateAudioFiles(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)) {
            items(audioFiles) { file ->
                AudioFileItem(
                    file = file.toString(),
                    isPlaying = currentPlaying == file && isPlaying,
                    onPlayPauseClick = {
                        if (isPlaying) viewModel.stopAudio() else viewModel.playAudio(context, file)
                    },
                    onDeleteClick = { viewModel.deleteAudioFile(context, file) }
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = Color.Gray
        )

        Button(
            onClick = {
                if (isRecording) viewModel.stopRecording() else viewModel.startRecording(
                    context
                )
            },
            enabled = !isPlaying,
            colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) Color.Red else Color.Gray)
        ) {
            Text(if (isRecording) "Stop Recording" else "Start Recording")
        }
    }
}

@Composable
fun AudioFileItem(
    file: String,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(file, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.DarkGray,
            modifier = Modifier
                .size(36.dp)
                .clickable { onPlayPauseClick() }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "Delete",
            tint = Color.Red,
            modifier = Modifier
                .size(28.dp)
                .clickable { onDeleteClick() }
        )
    }
}

class MainActivity : ComponentActivity() {
    private fun checkPermissions(): Boolean {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return false
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if (checkPermissions()) {
                AudioApp(viewModel())
            }
        }
    }
}
