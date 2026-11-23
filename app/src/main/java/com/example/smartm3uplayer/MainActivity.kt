package com.example.smartm3uplayer

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var panelMenu: LinearLayout
    private lateinit var panelSettings: LinearLayout

    private lateinit var tvAppTitle: TextView
    private lateinit var btnMenu: Button
    private lateinit var btnSettings: Button
    private lateinit var tvMenuHeading: TextView
    private lateinit var etM3uUrl: EditText
    private lateinit var btnLoadPlay: Button
    private lateinit var btnCloseMenu: Button
    private lateinit var tvSettingsHeading: TextView
    private lateinit var tvLanguageLabel: TextView
    private lateinit var btnCloseSettings: Button

    private var currentLang: String = "ro"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        panelMenu = findViewById(R.id.panelMenu)
        panelSettings = findViewById(R.id.panelSettings)

        tvAppTitle = findViewById(R.id.tvAppTitle)
        btnMenu = findViewById(R.id.btnMenu)
        btnSettings = findViewById(R.id.btnSettings)
        tvMenuHeading = findViewById(R.id.tvMenuHeading)
        etM3uUrl = findViewById(R.id.etM3uUrl)
        btnLoadPlay = findViewById(R.id.btnLoadPlay)
        btnCloseMenu = findViewById(R.id.btnCloseMenu)
        tvSettingsHeading = findViewById(R.id.tvSettingsHeading)
        tvLanguageLabel = findViewById(R.id.tvLanguageLabel)
        btnCloseSettings = findViewById(R.id.btnCloseSettings)

        val prefs = getSharedPreferences("smart_m3u_prefs", Context.MODE_PRIVATE)
        currentLang = prefs.getString("lang", "ro") ?: "ro"
        etM3uUrl.setText(prefs.getString("last_url", "") ?: "")

        applyLanguage(currentLang)
        initPlayer()

        btnMenu.setOnClickListener {
            panelSettings.visibility = View.GONE
            panelMenu.visibility = if (panelMenu.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        btnSettings.setOnClickListener {
            panelMenu.visibility = View.GONE
            panelSettings.visibility = if (panelSettings.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        btnCloseMenu.setOnClickListener { panelMenu.visibility = View.GONE }
        btnCloseSettings.setOnClickListener { panelSettings.visibility = View.GONE }

        findViewById<Button>(R.id.btnLangRo).setOnClickListener { setLanguage("ro") }
        findViewById<Button>(R.id.btnLangEn).setOnClickListener { setLanguage("en") }
        findViewById<Button>(R.id.btnLangRu).setOnClickListener { setLanguage("ru") }

        btnLoadPlay.setOnClickListener {
            val input = etM3uUrl.text.toString().trim()
            if (input.isEmpty()) {
                showToast(msgEnterUrl())
                return@setOnClickListener
            }
            prefs.edit().putString("last_url", input).apply()
            panelMenu.visibility = View.GONE

            // .m3u8 este de obicei stream HLS direct; .m3u este playlist text
            if (input.contains(".m3u8", ignoreCase = true)) {
                playDirectUrl(input)
            } else if (input.contains(".m3u", ignoreCase = true)) {
                loadM3uPlaylist(input)
            } else {
                playDirectUrl(input)
            }
        }
    }

    private fun setLanguage(lang: String) {
        currentLang = lang
        getSharedPreferences("smart_m3u_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("lang", lang)
            .apply()
        applyLanguage(lang)
    }

    private fun applyLanguage(lang: String) {
        when (lang) {
            "en" -> {
                tvAppTitle.text = "Smart M3U Player"
                btnMenu.text = "Menu"
                btnSettings.text = "Settings"
                tvMenuHeading.text = "Menu"
                etM3uUrl.hint = "Paste M3U / M3U8 URL"
                btnLoadPlay.text = "Load & Play"
                btnCloseMenu.text = "Close"
                tvSettingsHeading.text = "Settings"
                tvLanguageLabel.text = "Language"
                btnCloseSettings.text = "Close"
            }
            "ru" -> {
                tvAppTitle.text = "Smart M3U Player"
                btnMenu.text = "Меню"
                btnSettings.text = "Настройки"
                tvMenuHeading.text = "Меню"
                etM3uUrl.hint = "Вставьте ссылку M3U / M3U8"
                btnLoadPlay.text = "Загрузить и играть"
                btnCloseMenu.text = "Закрыть"
                tvSettingsHeading.text = "Настройки"
                tvLanguageLabel.text = "Язык"
                btnCloseSettings.text = "Закрыть"
            }
            else -> {
                tvAppTitle.text = "Smart M3U Player"
                btnMenu.text = "Meniu"
                btnSettings.text = "Setări"
                tvMenuHeading.text = "Meniu"
                etM3uUrl.hint = "Lipește link M3U / M3U8"
                btnLoadPlay.text = "Încarcă și redă"
                btnCloseMenu.text = "Închide"
                tvSettingsHeading.text = "Setări"
                tvLanguageLabel.text = "Limba"
                btnCloseSettings.text = "Închide"
            }
        }
    }

    private fun msgEnterUrl(): String = when (currentLang) {
        "en" -> "Enter an M3U or M3U8 URL"
        "ru" -> "Введите ссылку M3U или M3U8"
        else -> "Introdu un link M3U sau M3U8"
    }

    private fun msgNoStreams(): String = when (currentLang) {
        "en" -> "No stream found in playlist"
        "ru" -> "Поток в плейлисте не найден"
        else -> "Nu am găsit niciun stream în listă"
    }

    private fun msgDownloadError(): String = when (currentLang) {
        "en" -> "Cannot load link / playlist"
        "ru" -> "Не удалось загрузить ссылку / плейлист"
        else -> "Nu pot încărca linkul / lista"
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun initPlayer() {
        val exo = ExoPlayer.Builder(this).build()
        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                runOnUiThread {
                    showToast("${msgDownloadError()}: ${error.errorCodeName}")
                }
            }
        })
        player = exo
        playerView.player = exo
    }

    private fun playDirectUrl(url: String) {
        val exo = player ?: return
        exo.setMediaItem(MediaItem.fromUri(url))
        exo.prepare()
        exo.playWhenReady = true
    }

    private fun loadM3uPlaylist(playlistUrl: String) {
        Thread {
            try {
                val url = URL(playlistUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 15000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                conn.connect()
                if (conn.responseCode !in 200..299) {
                    throw Exception("HTTP ${conn.responseCode}")
                }

                val urls = mutableListOf<String>()
                BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            urls.add(trimmed)
                        }
                        line = reader.readLine()
                    }
                }
                conn.disconnect()

                runOnUiThread {
                    if (urls.isNotEmpty()) {
                        playDirectUrl(urls.first())
                    } else {
                        showToast(msgNoStreams())
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    showToast(msgDownloadError())
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
