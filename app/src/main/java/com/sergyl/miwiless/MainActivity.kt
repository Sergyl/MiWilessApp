package com.sergyl.miwiless

import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


class SoftOption {
    //Dirección IP del servidor
    var remoteHost: String = "192.168.0.15"
    var remotePort: Int = 5000
}

val Settings = SoftOption()

class MainActivity : AppCompatActivity() {
    private val mSampleRates = intArrayOf(8000, 22050, 11025, 44100)
    private var recorder : AudioRecord? = null
    //CAMBIAR ESTA VARIABLE A OTRA DINAMICA
    private var bufferSize = AudioRecord.getMinBufferSize(44100, 16, 3);

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
            Log.d("INFO", "Internet NOT granted")
        }else{
            Log.d("INFO", "Internet granted")
        }

        if(ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.RECORD_AUDIO), 100 )
        }

        var connectButton = findViewById<Button>(R.id.button)
        connectButton.setOnClickListener {
            sendAudio()
            Toast.makeText(this@MainActivity, "Click...", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendAudio(){
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try{
            var socket = DatagramSocket()
            Log.d("INFO", "Correctly created")

            recorder = findAudioRecord()
            recorder?.startRecording()

            while(true) {
               var sendData = ByteArray(bufferSize)
                recorder?.read(sendData, 0, sendData.size)

                val sendPacket = DatagramPacket(sendData, sendData.size, InetAddress.getByName(Settings.remoteHost), Settings.remotePort)
                Log.d("INFO", sendPacket.length.toString())

                socket.send(sendPacket)
            }

        } catch(e: IOException){
            Log.d("INFO", e.toString())
        }
    }

    private fun findAudioRecord(): AudioRecord? {
        for (rate in mSampleRates) {
            for (audioFormat in shortArrayOf(AudioFormat.ENCODING_PCM_16BIT.toShort(), AudioFormat.ENCODING_PCM_8BIT.toShort())) {
                for (channelConfig in shortArrayOf(AudioFormat.CHANNEL_IN_STEREO.toShort(), AudioFormat.CHANNEL_IN_MONO.toShort())) {
                    try {
                        Log.d("INFO", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig)
                        val bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig.toInt(), audioFormat.toInt())

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            val recorder = AudioRecord(AudioSource.MIC, rate, channelConfig.toInt(), audioFormat.toInt(), bufferSize)

                            if (recorder.state == AudioRecord.STATE_INITIALIZED){
                                Log.d("INFO", "Audio created successfully")
                                return recorder

                                }
                        }
                    } catch (e: Exception) {
                        Log.e("ERROR", rate.toString() + "Exception, keep trying.", e)
                    }
                }
            }
        }
        return null
    }
}
