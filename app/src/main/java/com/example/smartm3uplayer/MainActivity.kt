package com.example.smartm3uplayer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var etStreamUrl: EditText
    private lateinit var btnPlay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        etStreamUrl = findViewById(R.id.etStreamUrl)
        btnPlay = findViewById(R.id.btnPlay)

        initPlayer()

        btnPlay.setOnClickListener {
            val input = etStreamUrl.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(this, "Introdu un link M3U sau stream", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (input.endsWith(".m3u", ignoreCase = true) || input.endsWith(".m3u8", ignoreCase = true)) {
                loadM3uAndPlayFirst(input)
            } else {
                playDirectUrl(input)
            }
        }
    }

    private fun initPlayer() {
        val exo = ExoPlayer.Builder(this).build()
        player = exo
        playerView.player = exo
    }

    private fun playDirectUrl(url: String) {
        val exo = player ?: return
        exo.setMediaItem(MediaItem.fromUri(url))
        exo.prepare()
        exo.playWhenReady = true
    }

    private fun loadM3uAndPlayFirst(m3uUrl: String) {
        Thread {
            try {
                val content = URL(m3uUrl).readText()
                val streamUrl = content.lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.isNotEmpty() && !it.startsWith("#") }

                runOnUiThread {
                    if (streamUrl != null) {
                        etStreamUrl.setText(streamUrl)
                        playDirectUrl(streamUrl)
                    } else {
                        Toast.makeText(this, "Nu am găsit niciun stream în lista M3U", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Nu pot descărca lista M3U", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
