package com.example.jeudifferences

import android.media.MediaRecorder
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class VoiceRecorderActivity {

    private var mediaRecorder: MediaRecorder? = null
    private var tempFile: File? = null

    fun startRecording() {
        tempFile = File.createTempFile("temp_audio", ".mp3")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            Log.i("asdf","asdf")
            setOutputFile(tempFile?.absolutePath)
            Log.i("1234","1234")

            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("VoiceRecorderActivity", "Error starting recording", e)
            }
        }
    }

    fun stopRecordingAndGetAudioData(): ByteArray? {
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: RuntimeException) {
                Log.e("VoiceRecorderActivity", "Error stopping recording", e)
            }
        }
        mediaRecorder = null

        // Read the temporary file into a byte array
        return tempFile?.let { file ->
            val bytes = file.readBytes()
            file.delete() // Delete the temporary file
            return@let bytes
        }
    }
}
