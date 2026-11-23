package com.example.smartm3uplayer

import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

data class M3uChannel(
    val name: String,
    val group: String,
    val url: String
)

class MainActivity : AppCompatActivity() {

    private val defaultPlaylistUrl = "https://raw.githubusercontent.com/slava-67/TV/refs/heads/IPTV/kino-plus.m3u"

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var tvGestureHud: TextView
    private lateinit var tvNowPlaying: TextView
    private lateinit var btnOpenDrawer: Button

    private lateinit var drawerPanel: LinearLayout
    private lateinit var sectionChannels: LinearLayout
    private lateinit var sectionMenu: LinearLayout
    private lateinit var sectionSettings: ScrollView

    private lateinit var btnTabChannels: Button
    private lateinit var btnTabMenu: Button
    private lateinit var btnTabSettings: Button
    private lateinit var btnCloseDrawer: Button

    private lateinit var tvGroupsLabel: TextView
    private lateinit var groupsContainer: LinearLayout
    private lateinit var tvChannelsLabel: TextView
    private lateinit var lvChannels: ListView

    private lateinit var tvMenuHeading: TextView
    private lateinit var etM3uUrl: EditText
    private lateinit var btnLoadPlay: Button

    private lateinit var tvSettingsHeading: TextView
    private lateinit var tvLanguageLabel: TextView
    private lateinit var tvQualityLabel: TextView
    private lateinit var tvContrastLabel: TextView
    private lateinit var tvSaturationLabel: TextView
    private lateinit var tvWarmthLabel: TextView
    private lateinit var btnResetColors: Button

    private lateinit var seekContrast: SeekBar
    private lateinit var seekSaturation: SeekBar
    private lateinit var seekWarmth: SeekBar

    private lateinit var audioManager: AudioManager
    private var maxVolume = 15
    private var startVolume = 0
    private var gestureStartX = 0f
    private var gestureStartY = 0f
    private var gestureMode: String? = null

    private val hudHandler = Handler(Looper.getMainLooper())
    private val hideHudRunnable = Runnable { tvGestureHud.visibility = View.GONE }

    private var currentLang: String = "ro"
    private var allChannels: List<M3uChannel> = emptyList()
    private var visibleChannels: List<M3uChannel> = emptyList()
    private var selectedGroup: String = "ALL"
    private var currentStreamIndex: Int = 0
    private var isLoadingPlaylist: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        tvGestureHud = findViewById(R.id.tvGestureHud)
        tvNowPlaying = findViewById(R.id.tvNowPlaying)
        btnOpenDrawer = findViewById(R.id.btnOpenDrawer)

        drawerPanel = findViewById(R.id.drawerPanel)
        sectionChannels = findViewById(R.id.sectionChannels)
        sectionMenu = findViewById(R.id.sectionMenu)
        sectionSettings = findViewById(R.id.sectionSettings)

        btnTabChannels = findViewById(R.id.btnTabChannels)
        btnTabMenu = findViewById(R.id.btnTabMenu)
        btnTabSettings = findViewById(R.id.btnTabSettings)
        btnCloseDrawer = findViewById(R.id.btnCloseDrawer)

        tvGroupsLabel = findViewById(R.id.tvGroupsLabel)
        groupsContainer = findViewById(R.id.groupsContainer)
        tvChannelsLabel = findViewById(R.id.tvChannelsLabel)
        lvChannels = findViewById(R.id.lvChannels)

        tvMenuHeading = findViewById(R.id.tvMenuHeading)
        etM3uUrl = findViewById(R.id.etM3uUrl)
        btnLoadPlay = findViewById(R.id.btnLoadPlay)

        tvSettingsHeading = findViewById(R.id.tvSettingsHeading)
        tvLanguageLabel = findViewById(R.id.tvLanguageLabel)
        tvQualityLabel = findViewById(R.id.tvQualityLabel)
        tvContrastLabel = findViewById(R.id.tvContrastLabel)
        tvSaturationLabel = findViewById(R.id.tvSaturationLabel)
        tvWarmthLabel = findViewById(R.id.tvWarmthLabel)
        btnResetColors = findViewById(R.id.btnResetColors)

        seekContrast = findViewById(R.id.seekContrast)
        seekSaturation = findViewById(R.id.seekSaturation)
        seekWarmth = findViewById(R.id.seekWarmth)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

        val prefs = getSharedPreferences("smart_m3u_prefs", Context.MODE_PRIVATE)
        currentLang = prefs.getString("lang", "ro") ?: "ro"
        val savedUrl = prefs.getString("last_url", defaultPlaylistUrl)
            ?.takeIf { it.isNotBlank() } ?: defaultPlaylistUrl
        etM3uUrl.setText(savedUrl)

        seekContrast.progress = prefs.getInt("contrast", 100)
        seekSaturation.progress = prefs.getInt("saturation", 100)
        seekWarmth.progress = prefs.getInt("warmth", 50)

        applyLanguage(currentLang)
        initPlayer()
        applyColorMatrix()

        btnOpenDrawer.setOnClickListener {
            drawerPanel.visibility = if (drawerPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (drawerPanel.visibility == View.VISIBLE) {
                showDrawerTab("channels")
            }
        }

        btnTabChannels.setOnClickListener { showDrawerTab("channels") }
        btnTabMenu.setOnClickListener { showDrawerTab("menu") }
        btnTabSettings.setOnClickListener { showDrawerTab("settings") }
        btnCloseDrawer.setOnClickListener { drawerPanel.visibility = View.GONE }

        lvChannels.setOnItemClickListener { _, _, position, _ ->
            if (position in visibleChannels.indices) {
                currentStreamIndex = position
                val ch = visibleChannels[position]
                drawerPanel.visibility = View.GONE
                playChannel(ch)
            }
        }

        findViewById<Button>(R.id.btnLangRo).setOnClickListener { setLanguage("ro") }
        findViewById<Button>(R.id.btnLangEn).setOnClickListener { setLanguage("en") }
        findViewById<Button>(R.id.btnLangRu).setOnClickListener { setLanguage("ru") }

        findViewById<Button>(R.id.btnQualAuto).setOnClickListener { setMaxVideoHeight(Int.MAX_VALUE, "Auto") }
        findViewById<Button>(R.id.btnQual1080).setOnClickListener { setMaxVideoHeight(1080, "1080p") }
        findViewById<Button>(R.id.btnQual720).setOnClickListener { setMaxVideoHeight(720, "720p") }
        findViewById<Button>(R.id.btnQual480).setOnClickListener { setMaxVideoHeight(480, "480p") }

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
            val input = etM3uUrl.text.toString().trim().ifEmpty { defaultPlaylistUrl }
            etM3uUrl.setText(input)
            prefs.edit().putString("last_url", input).apply()
            showDrawerTab("channels")
            startSource(input)
        }

        startSource(savedUrl)
    }

    private fun showDrawerTab(tab: String) {
        sectionChannels.visibility = if (tab == "channels") View.VISIBLE else View.GONE
        sectionMenu.visibility = if (tab == "menu") View.VISIBLE else View.GONE
        sectionSettings.visibility = if (tab == "settings") View.VISIBLE else View.GONE
    }

    private fun startSource(source: String) {
        if (source.contains(".m3u8", ignoreCase = true) && !source.contains(".m3u?", ignoreCase = true)) {
            val single = listOf(M3uChannel(source, msgGeneralGroup(), source))
            setPlaylistData(single)
            playChannel(single.first())
        } else if (source.contains(".m3u", ignoreCase = true)) {
            loadM3uPlaylist(source)
        } else {
            val single = listOf(M3uChannel(source, msgGeneralGroup(), source))
            setPlaylistData(single)
            playChannel(single.first())
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val width = resources.displayMetrics.widthPixels.toFloat().coerceAtLeast(1f)
        val height = resources.displayMetrics.heightPixels.toFloat().coerceAtLeast(1f)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartX = ev.x
                gestureStartY = ev.y
                gestureMode = null
                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }

            MotionEvent.ACTION_MOVE -> {
                if (drawerPanel.visibility == View.VISIBLE) {
                    return super.dispatchTouchEvent(ev)
                }

                val dx = ev.x - gestureStartX
                val dy = gestureStartY - ev.y

                if (gestureMode == null && gestureStartX <= 48f && dx > 45f && abs(dx) > abs(dy) * 1.3f) {
                    gestureMode = "drawer"
                    drawerPanel.visibility = View.VISIBLE
                    showDrawerTab("channels")
                    return true
                }

                if (gestureMode == null && abs(dy) > 28f && abs(dy) > abs(dx)) {
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
                    val attrs = window.attributes
                    val baseBright = if (attrs.screenBrightness in 0f..1f) attrs.screenBrightness else 0.5f
                    val newBright = (baseBright + deltaBright).coerceIn(0.05f, 1.0f)
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

    private fun setPlaylistData(channels: List<M3uChannel>) {
        allChannels = channels
        selectedGroup = "ALL"
        rebuildGroupButtons()
        filterChannelsByGroup("ALL")
    }

    private fun rebuildGroupButtons() {
        groupsContainer.removeAllViews()
        val groups = listOf("ALL") + allChannels.map { it.group }.distinct()
        for (grp in groups) {
            val btn = Button(this).apply {
                text = if (grp == "ALL") msgAllGroups() else grp
                textSize = 12f
                setBackgroundColor(if (grp == selectedGroup) Color.parseColor("#2E7D32") else Color.parseColor("#262626"))
                setTextColor(Color.WHITE)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = 8
                }
                layoutParams = params
                setOnClickListener {
                    selectedGroup = grp
                    rebuildGroupButtons()
                    filterChannelsByGroup(grp)
                }
            }
            groupsContainer.addView(btn)
        }
    }

    private fun filterChannelsByGroup(group: String) {
        visibleChannels = if (group == "ALL") {
            allChannels
        } else {
            allChannels.filter { it.group == group }
        }
        val names = visibleChannels.mapIndexed { idx, ch -> "${idx + 1}. ${ch.name}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        lvChannels.adapter = adapter
    }

    private fun playChannel(channel: M3uChannel) {
        tvNowPlaying.text = channel.name
        val exo = player ?: return
        exo.setMediaItem(MediaItem.fromUri(channel.url))
        exo.prepare()
        exo.playWhenReady = true
    }

    private fun setMaxVideoHeight(maxHeight: Int, label: String) {
        val exo = player ?: return
        val builder = exo.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)

        if (maxHeight == Int.MAX_VALUE) {
            builder.clearVideoSizeConstraints()
        } else {
            builder.setMaxVideoSize(Int.MAX_VALUE, maxHeight)
        }

        exo.trackSelectionParameters = builder.build()
        showHud("${msgQuality()}: $label")
    }

    private fun applyColorMatrix() {
        val videoSurface = playerView.videoSurfaceView as? TextureView ?: return

        val contrast = seekContrast.progress / 100f
        val saturation = seekSaturation.progress / 100f
        val warmth = (seekWarmth.progress - 50) / 50f

        val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
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
        videoSurface.setLayerPaint(Paint().apply {
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
        rebuildGroupButtons()
    }

    private fun applyLanguage(lang: String) {
        when (lang) {
            "en" -> {
                btnOpenDrawer.text = "Menu"
                btnTabChannels.text = "Channels"
                btnTabMenu.text = "Menu"
                btnTabSettings.text = "Settings"
                tvGroupsLabel.text = "Channel groups"
                tvChannelsLabel.text = "Channel list"
                tvMenuHeading.text = "Menu"
                etM3uUrl.hint = "Paste M3U / M3U8 URL"
                btnLoadPlay.text = "Load & Play"
                tvSettingsHeading.text = "Settings"
                tvLanguageLabel.text = "Language"
                tvQualityLabel.text = "Video quality"
                tvContrastLabel.text = "Contrast"
                tvSaturationLabel.text = "Saturation"
                tvWarmthLabel.text = "Color warmth"
                btnResetColors.text = "Reset colors"
            }
            "ru" -> {
                btnOpenDrawer.text = "Меню"
                btnTabChannels.text = "Каналы"
                btnTabMenu.text = "Меню"
                btnTabSettings.text = "Настройки"
                tvGroupsLabel.text = "Группы каналов"
                tvChannelsLabel.text = "Список каналов"
                tvMenuHeading.text = "Меню"
                etM3uUrl.hint = "Вставьте ссылку M3U / M3U8"
                btnLoadPlay.text = "Загрузить и играть"
                tvSettingsHeading.text = "Настройки"
                tvLanguageLabel.text = "Язык"
                tvQualityLabel.text = "Качество видео"
                tvContrastLabel.text = "Контрастность"
                tvSaturationLabel.text = "Насыщенность"
                tvWarmthLabel.text = "Теплота цвета"
                btnResetColors.text = "Сброс цветов"
            }
            else -> {
                btnOpenDrawer.text = "Meniu"
                btnTabChannels.text = "Canale"
                btnTabMenu.text = "Meniu"
                btnTabSettings.text = "Setări"
                tvGroupsLabel.text = "Grupe canale"
                tvChannelsLabel.text = "Lista canale"
                tvMenuHeading.text = "Meniu"
                etM3uUrl.hint = "Lipește link M3U / M3U8"
                btnLoadPlay.text = "Încarcă și redă"
                tvSettingsHeading.text = "Setări"
                tvLanguageLabel.text = "Limba"
                tvQualityLabel.text = "Calitate video"
                tvContrastLabel.text = "Contrast"
                tvSaturationLabel.text = "Saturație"
                tvWarmthLabel.text = "Căldură culori"
                btnResetColors.text = "Reset culori"
            }
        }
    }

    private fun msgAllGroups(): String = when (currentLang) {
        "en" -> "All"
        "ru" -> "Все"
        else -> "Toate"
    }

    private fun msgGeneralGroup(): String = when (currentLang) {
        "en" -> "General"
        "ru" -> "Общие"
        else -> "General"
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
                    if (!isLoadingPlaylist && currentStreamIndex + 1 < visibleChannels.size) {
                        currentStreamIndex++
                        val next = visibleChannels[currentStreamIndex]
                        showToast("#${currentStreamIndex + 1}/${visibleChannels.size} (${error.errorCodeName})")
                        playChannel(next)
                    } else {
                        showToast("${msgDownloadError()}: ${error.errorCodeName}")
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                runOnUiThread {
                    applyColorMatrix()
                }
            }
        })
        player = exo
        playerView.player = exo
    }

    private fun loadM3uPlaylist(playlistUrl: String) {
        isLoadingPlaylist = true
        Thread {
            try {
                val conn = (URL(playlistUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 15000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                conn.connect()
                if (conn.responseCode !in 200..299) {
                    throw Exception("HTTP ${conn.responseCode}")
                }

                val parsed = mutableListOf<M3uChannel>()
                var pendingName: String? = null
                var pendingGroup: String = msgGeneralGroup()

                BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                            val groupMatch = Regex("""group-title="([^"]*)"""").find(trimmed)
                            pendingGroup = groupMatch?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
                                ?: msgGeneralGroup()
                            val commaIdx = trimmed.lastIndexOf(',')
                            pendingName = if (commaIdx != -1 && commaIdx + 1 < trimmed.length) {
                                trimmed.substring(commaIdx + 1).trim().ifEmpty { null }
                            } else {
                                null
                            }
                        } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            val channelName = pendingName ?: "Channel ${parsed.size + 1}"
                            parsed.add(M3uChannel(channelName, pendingGroup, trimmed))
                            pendingName = null
                            pendingGroup = msgGeneralGroup()
                        }
                        line = reader.readLine()
                    }
                }
                conn.disconnect()

                runOnUiThread {
                    isLoadingPlaylist = false
                    if (parsed.isNotEmpty()) {
                        setPlaylistData(parsed)
                        currentStreamIndex = 0
                        showToast("OK: ${parsed.size}")
                        playChannel(parsed.first())
                    } else {
                        showToast(msgNoStreams())
                    }
                }
            } catch (ex: Exception) {
                runOnUiThread {
                    isLoadingPlaylist = false
                    val reason = ex.message?.take(40) ?: "Error"
                    showToast("${msgDownloadError()} ($reason)")
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
