package com.example.smartm3uplayer

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var tvGestureHud: TextView
    private lateinit var panelMenu: LinearLayout
    private lateinit var panelSettings: ScrollView

    private lateinit var tvAppTitle: TextView
    private lateinit var btnMenu: Button
    private lateinit var btnSettings: Button
    private lateinit var tvMenuHeading: TextView
    private lateinit var etM3uUrl: EditText
    private lateinit var btnLoadPlay: Button
    private lateinit var btnCloseMenu: Button
    private lateinit var tvSettingsHeading: TextView
    private lateinit var tvLanguageLabel: TextView
    private lateinit var tvQualityLabel: TextView
    private lateinit var tvZoomLabel: TextView
    private lateinit var tvContrastLabel: TextView
    private lateinit var tvSaturationLabel: TextView
    private lateinit var tvWarmthLabel: TextView
    private lateinit var btnResetColors: Button
    private lateinit var btnCloseSettings: Button

    private lateinit var seekContrast: SeekBar
    private lateinit var seekSaturation: SeekBar
    private lateinit var seekWarmth: SeekBar

    private lateinit var audioManager: AudioManager
    private var maxVolume = 15
    private var startVolume = 0
    private var startBrightness = 0.5f
    private var gestureStartX = 0f
    private var gestureStartY = 0f
    private var gestureMode: String? = null // "volume" sau "brightness"

    private val hudHandler = Handler(Looper.getMainLooper())
    private val hideHudRunnable = Runnable { tvGestureHud.visibility = View.GONE }

    private var currentLang: String = "ro"
    private var zoomMode: String = "1.0" // "1.0", "1.25", "1.5", "fill"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        tvGestureHud = findViewById(R.id.tvGestureHud)
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
        tvQualityLabel = findViewById(R.id.tvQualityLabel)
        tvZoomLabel = findViewById(R.id.tvZoomLabel)
        tvContrastLabel = findViewById(R.id.tvContrastLabel)
        tvSaturationLabel = findViewById(R.id.tvSaturationLabel)
        tvWarmthLabel = findViewById(R.id.tvWarmthLabel)
        btnResetColors = findViewById(R.id.btnResetColors)
        btnCloseSettings = findViewById(R.id.btnCloseSettings)

        seekContrast = findViewById(R.id.seekContrast)
        seekSaturation = findViewById(R.id.seekSaturation)
        seekWarmth = findViewById(R.id.seekWarmth)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

        val currentWinBrightness = window.attributes.screenBrightness
        if (currentWinBrightness in 0f..1f) {
            startBrightness = currentWinBrightness
        }

        val prefs = getSharedPreferences("smart_m3u_prefs", Context.MODE_PRIVATE)
        currentLang = prefs.getString("lang", "ro") ?: "ro"
        etM3uUrl.setText(prefs.getString("last_url", "") ?: "")
        zoomMode = prefs.getString("zoom_mode", "1.0") ?: "1.0"

        seekContrast.progress = prefs.getInt("contrast", 100)
        seekSaturation.progress = prefs.getInt("saturation", 100)
        seekWarmth.progress = prefs.getInt("warmth", 50)

        applyLanguage(currentLang)
        initPlayer()
        applyColorMatrix()
        applyZoom(zoomMode)

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

        // Calitate video
        findViewById<Button>(R.id.btnQualAuto).setOnClickListener { setMaxVideoHeight(Int.MAX_VALUE, "Auto") }
        findViewById<Button>(R.id.btnQual1080).setOnClickListener { setMaxVideoHeight(1080, "1080p") }
        findViewById<Button>(R.id.btnQual720).setOnClickListener { setMaxVideoHeight(720, "720p") }
        findViewById<Button>(R.id.btnQual480).setOnClickListener { setMaxVideoHeight(480, "480p") }

        // Zoom
        findViewById<Button>(R.id.btnZoom100).setOnClickListener { saveAndApplyZoom("1.0") }
        findViewById<Button>(R.id.btnZoom125).setOnClickListener { saveAndApplyZoom("1.25") }
        findViewById<Button>(R.id.btnZoom150).setOnClickListener { saveAndApplyZoom("1.5") }
        findViewById<Button>(R.id.btnZoomFill).setOnClickListener { saveAndApplyZoom("fill") }

        // Culori
        val colorChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit()
                    .putInt("contrast", seekContrast.progress)
                    .putInt("saturation", seekSaturation.progress)
                    .putInt("warmth", seekWarmth.progress)
                    .apply()
                applyColorMatrix()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekContrast.setOnSeekBarChangeListener(colorChangeListener)
        seekSaturation.setOnSeekBarChangeListener(colorChangeListener)
        seekWarmth.setOnSeekBarChangeListener(colorChangeListener)

        btnResetColors.setOnClickListener {
            seekContrast.progress = 100
            seekSaturation.progress = 100
            seekWarmth.progress = 50
            prefs.edit()
                .putInt("contrast", 100)
                .putInt("saturation", 100)
                .putInt("warmth", 50)
                .apply()
            applyColorMatrix()
            showHud(msgColorsReset())
        }

        btnLoadPlay.setOnClickListener {
            val input = etM3uUrl.text.toString().trim()
            if (input.isEmpty()) {
                showToast(msgEnterUrl())
                return@setOnClickListener
            }
            prefs.edit().putString("last_url", input).apply()
            panelMenu.visibility = View.GONE

            if (input.contains(".m3u8", ignoreCase = true)) {
                playDirectUrl(input)
            } else if (input.contains(".m3u", ignoreCase = true)) {
                loadM3uPlaylist(input)
            } else {
                playDirectUrl(input)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (panelMenu.visibility == View.VISIBLE || panelSettings.visibility == View.VISIBLE) {
            return super.dispatchTouchEvent(ev)
        }

        val width = resources.displayMetrics.widthPixels.toFloat().coerceAtLeast(1f)
        val height = resources.displayMetrics.heightPixels.toFloat().coerceAtLeast(1f)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartX = ev.x
                gestureStartY = ev.y
                gestureMode = null
                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val winBright = window.attributes.screenBrightness
                startBrightness = if (winBright in 0f..1f) winBright else 0.5f
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = gestureStartY - ev.y // pozitiv când glisezi în sus
                val dx = abs(ev.x - gestureStartX)

                if (gestureMode == null && abs(dy) > 28f && abs(dy) > dx) {
                    gestureMode = if (gestureStartX >= width * 0.5f) "volume" else "brightness"
                }

                if (gestureMode == "volume") {
                    val deltaSteps = ((dy / (height * 0.5f)) * maxVolume).toInt()
                    val newVol = (startVolume + deltaSteps).coerceIn(0, maxVolume)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                    val pct = (newVol * 100) / maxVolume
                    showHud("${msgVolume()}: $pct%")
                    return true
                } else if (gestureMode == "brightness") {
                    val deltaBright = dy / (height * 0.5f)
                    val newBright = (startBrightness + deltaBright).coerceIn(0.05f, 1.0f)
                    val attrs = window.attributes
                    attrs.screenBrightness = newBright
                    window.attributes = attrs
                    val pct = (newBright * 100).toInt()
                    showHud("${msgBrightness()}: $pct%")
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                gestureMode = null
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun setMaxVideoHeight(maxHeight: Int, label: String) {
        val exo = player ?: return
        val builder = exo.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_VIDEO)

        if (maxHeight == Int.MAX_VALUE) {
            builder.clearVideoSizeConstraints()
        } else {
            builder.setMaxVideoSize(Int.MAX_VALUE, maxHeight)
        }

        exo.trackSelectionParameters = builder.build()
        showHud("${msgQuality()}: $label")
    }

    private fun saveAndApplyZoom(mode: String) {
        zoomMode = mode
        getSharedPreferences("smart_m3u_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("zoom_mode", mode)
            .apply()
        applyZoom(mode)
        showHud("Zoom: $mode")
    }

    private fun applyZoom(mode: String) {
        val videoSurface = playerView.videoSurfaceView as? TextureView ?: return
        when (mode) {
            "1.25" -> {
                videoSurface.scaleX = 1.25f
                videoSurface.scaleY = 1.25f
            }
            "1.5" -> {
                videoSurface.scaleX = 1.5f
                videoSurface.scaleY = 1.5f
            }
            "fill" -> {
                videoSurface.scaleX = 1.35f
                videoSurface.scaleY = 1.35f
            }
            else -> {
                videoSurface.scaleX = 1.0f
                videoSurface.scaleY = 1.0f
            }
        }
    }

    private fun applyColorMatrix() {
        val videoSurface = playerView.videoSurfaceView as? TextureView ?: return

        val contrast = seekContrast.progress / 100f // 0.0 .. 2.0 (1.0 = normal)
        val saturation = seekSaturation.progress / 100f // 0.0 .. 2.0 (1.0 = normal)
        val warmth = (seekWarmth.progress - 50) / 50f // -1.0 (rece) .. +1.0 (cald)

        val satMatrix = ColorMatrix().apply {
            setSaturation(saturation)
        }

        val translate = (1f - contrast) * 128f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val redShift = warmth * 25f
        val blueShift = -warmth * 25f
        val warmthMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, redShift,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, blueShift,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val combined = ColorMatrix().apply {
            postConcat(satMatrix)
            postConcat(contrastMatrix)
            postConcat(warmthMatrix)
        }

        videoSurface.setLayerPaint(android.graphics.Paint().apply {
            colorFilter = ColorMatrixColorFilter(combined)
        })
    }

    private fun showHud(text: String) {
        tvGestureHud.text = text
        tvGestureHud.visibility = View.VISIBLE
        hudHandler.removeCallbacks(hideHudRunnable)
        hudHandler.postDelayed(hideHudRunnable, 1200L)
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
                tvQualityLabel.text = "Video quality"
                tvZoomLabel.text = "Zoom"
                tvContrastLabel.text = "Contrast"
                tvSaturationLabel.text = "Saturation"
                tvWarmthLabel.text = "Color warmth"
                btnResetColors.text = "Reset colors"
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
                tvQualityLabel.text = "Качество видео"
                tvZoomLabel.text = "Масштаб (Zoom)"
                tvContrastLabel.text = "Контрастность"
                tvSaturationLabel.text = "Насыщенность"
                tvWarmthLabel.text = "Теплота цвета"
                btnResetColors.text = "Сброс цветов"
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
                tvQualityLabel.text = "Calitate video"
                tvZoomLabel.text = "Zoom"
                tvContrastLabel.text = "Contrast"
                tvSaturationLabel.text = "Saturație"
                tvWarmthLabel.text = "Căldură culori"
                btnResetColors.text = "Reset culori"
                btnCloseSettings.text = "Închide"
            }
        }
    }

    private fun msgVolume(): String = when (currentLang) {
        "en" -> "Volume"
        "ru" -> "Громкость"
        else -> "Volum"
    }

    private fun msgBrightness(): String = when (currentLang) {
        "en" -> "Brightness"
        "ru" -> "Яркость"
        else -> "Luminozitate"
    }

    private fun msgQuality(): String = when (currentLang) {
        "en" -> "Quality"
        "ru" -> "Качество"
        else -> "Calitate"
    }

    private fun msgColorsReset(): String = when (currentLang) {
        "en" -> "Colors reset"
        "ru" -> "Цвета сброшены"
        else -> "Culori resetate"
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

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                runOnUiThread {
                    applyZoom(zoomMode)
                    applyColorMatrix()
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
        hudHandler.removeCallbacks(hideHudRunnable)
        player?.release()
        player = null
    }
}
