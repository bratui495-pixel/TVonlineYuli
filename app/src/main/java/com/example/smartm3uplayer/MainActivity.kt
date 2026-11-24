package com.example.smartm3uplayer

import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
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
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

data class M3uChannel(
    val number: Int,
    val name: String,
    val group: String,
    val url: String
)

class MainActivity : AppCompatActivity() {

    private val defaultPlaylistUrl = "https://raw.githubusercontent.com/slava-67/TV/refs/heads/IPTV/kino-plus.m3u"

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var tvStreamInfo: TextView? = null
    private var tvTopChannelMini: TextView? = null
    private var tvPlaylistBadge: TextView? = null
    private var tvGroupBadge: TextView? = null
    private var tvCardChannelName: TextView? = null
    private var tvCardChannelNumber: TextView? = null
    private var channelInfoCard: LinearLayout? = null
    private var topRightHud: LinearLayout? = null
    private var btnPlaybackControls: Button? = null
    private var btnQuickChannels: Button? = null
    private var btnQuickMenu: Button? = null
    private var tvGestureHud: TextView? = null

    private var leftChannelsPanel: LinearLayout? = null
    private var tvDrawerPlaylistName: TextView? = null
    private var btnCloseLeftPanel: Button? = null
    private var etSearchChannel: EditText? = null
    private var groupsContainer: LinearLayout? = null
    private var lvChannels: ListView? = null

    private var rightMenuPanel: ScrollView? = null
    private var tvRightMenuTitle: TextView? = null
    private var btnCloseRightPanel: Button? = null
    private var btnOptAudioTrack: Button? = null
    private var btnOptFavorite: Button? = null
    private var btnOptStreamInfo: Button? = null
    private var tvMenuHeading: TextView? = null
    private var etM3uUrl: EditText? = null
    private var btnLoadPlay: Button? = null
    private var tvLanguageLabel: TextView? = null
    private var tvAspectLabel: TextView? = null
    private var tvContrastLabel: TextView? = null
    private var tvSaturationLabel: TextView? = null
    private var tvWarmthLabel: TextView? = null
    private var btnResetColors: Button? = null

    private var seekContrast: SeekBar? = null
    private var seekSaturation: SeekBar? = null
    private var seekWarmth: SeekBar? = null

    private var audioManager: AudioManager? = null
    private var maxVolume = 15
    private var startVolume = 0
    private var gestureStartX = 0f
    private var gestureStartY = 0f
    private var gestureMode: String? = null
    private var movedDuringGesture = false

    private val uiHandler = Handler(Looper.getMainLooper())
    private val hideHudRunnable = Runnable { tvGestureHud?.visibility = View.GONE }
    private val hideOverlayRunnable = Runnable {
        if (leftChannelsPanel?.visibility != View.VISIBLE && rightMenuPanel?.visibility != View.VISIBLE) {
            channelInfoCard?.visibility = View.GONE
            topRightHud?.visibility = View.GONE
            btnPlaybackControls?.visibility = View.GONE
            hideSystemBars()
        }
    }

    private var currentLang: String = "ro"
    private var aspectMode: String = "auto" // "auto", "4_3", "full"
    private var allChannels: List<M3uChannel> = emptyList()
    private var visibleChannels: List<M3uChannel> = emptyList()
    private var favorites: MutableSet<String> = mutableSetOf()
    private var selectedGroup: String = "ALL"
    private var currentChannel: M3uChannel? = null
    private var currentStreamIndex: Int = 0
    private var isLoadingPlaylist: Boolean = false
    private var audioTrackToggleIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            hideSystemBars()
            bindViews()

            audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            maxVolume = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)

            val prefs = getSharedPreferences("smart_m3u_prefs", Context.MODE_PRIVATE)
            currentLang = prefs.getString("lang", "ro") ?: "ro"
            aspectMode = prefs.getString("aspect_mode", "auto") ?: "auto"
            favorites = (prefs.getStringSet("favorites", emptySet()) ?: emptySet()).toMutableSet()
            val savedUrl = prefs.getString("last_url", defaultPlaylistUrl)
                ?.takeIf { it.isNotBlank() } ?: defaultPlaylistUrl
            etM3uUrl?.setText(savedUrl)
            updatePlaylistLabels(savedUrl)

            seekContrast?.progress = prefs.getInt("contrast", 100)
            seekSaturation?.progress = prefs.getInt("saturation", 100)
            seekWarmth?.progress = prefs.getInt("warmth", 50)

            applyLanguage(currentLang)
            initPlayer()
            applyAspectMode(aspectMode)
            applyColorMatrix()
            setupListeners(prefs)
            showOverlayTemporarily(2500L)
            startSource(savedUrl)
        } catch (ex: Exception) {
            showToast("Init error: ${ex.message?.take(50) ?: "unknown"}")
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
            }
        } catch (_: Exception) {
        }
    }

    private fun bindViews() {
        playerView = findViewById(R.id.playerView)
        tvStreamInfo = findViewById(R.id.tvStreamInfo)
        tvTopChannelMini = findViewById(R.id.tvTopChannelMini)
        tvPlaylistBadge = findViewById(R.id.tvPlaylistBadge)
        tvGroupBadge = findViewById(R.id.tvGroupBadge)
        tvCardChannelName = findViewById(R.id.tvCardChannelName)
        tvCardChannelNumber = findViewById(R.id.tvCardChannelNumber)
        channelInfoCard = findViewById(R.id.channelInfoCard)
        topRightHud = findViewById(R.id.topRightHud)
        btnPlaybackControls = findViewById(R.id.btnPlaybackControls)
        btnQuickChannels = findViewById(R.id.btnQuickChannels)
        btnQuickMenu = findViewById(R.id.btnQuickMenu)
        tvGestureHud = findViewById(R.id.tvGestureHud)

        leftChannelsPanel = findViewById(R.id.leftChannelsPanel)
        tvDrawerPlaylistName = findViewById(R.id.tvDrawerPlaylistName)
        btnCloseLeftPanel = findViewById(R.id.btnCloseLeftPanel)
        etSearchChannel = findViewById(R.id.etSearchChannel)
        groupsContainer = findViewById(R.id.groupsContainer)
        lvChannels = findViewById(R.id.lvChannels)

        rightMenuPanel = findViewById(R.id.rightMenuPanel)
        tvRightMenuTitle = findViewById(R.id.tvRightMenuTitle)
        btnCloseRightPanel = findViewById(R.id.btnCloseRightPanel)
        btnOptAudioTrack = findViewById(R.id.btnOptAudioTrack)
        btnOptFavorite = findViewById(R.id.btnOptFavorite)
        btnOptStreamInfo = findViewById(R.id.btnOptStreamInfo)
        tvMenuHeading = findViewById(R.id.tvMenuHeading)
        etM3uUrl = findViewById(R.id.etM3uUrl)
        btnLoadPlay = findViewById(R.id.btnLoadPlay)
        tvLanguageLabel = findViewById(R.id.tvLanguageLabel)
        tvAspectLabel = findViewById(R.id.tvAspectLabel)
        tvContrastLabel = findViewById(R.id.tvContrastLabel)
        tvSaturationLabel = findViewById(R.id.tvSaturationLabel)
        tvWarmthLabel = findViewById(R.id.tvWarmthLabel)
        btnResetColors = findViewById(R.id.btnResetColors)

        seekContrast = findViewById(R.id.seekContrast)
        seekSaturation = findViewById(R.id.seekSaturation)
        seekWarmth = findViewById(R.id.seekWarmth)
    }

    private fun setupListeners(prefs: android.content.SharedPreferences) {
        btnQuickChannels?.setOnClickListener { openLeftChannelsPanel() }
        btnCloseLeftPanel?.setOnClickListener {
            leftChannelsPanel?.visibility = View.GONE
            showOverlayTemporarily(2500L)
        }

        btnQuickMenu?.setOnClickListener { openRightMenuPanel() }
        btnCloseRightPanel?.setOnClickListener {
            rightMenuPanel?.visibility = View.GONE
            showOverlayTemporarily(2500L)
        }

        btnPlaybackControls?.setOnClickListener {
            val exo = player ?: return@setOnClickListener
            if (exo.isPlaying) {
                exo.pause()
                btnPlaybackControls?.text = "▶ Play"
                showHud("Pause")
            } else {
                exo.play()
                btnPlaybackControls?.text = "Playback controls"
                showHud("Play")
            }
            showOverlayTemporarily(2500L)
        }

        btnOptAudioTrack?.setOnClickListener { cycleAudioTrack() }

        btnOptFavorite?.setOnClickListener {
            val ch = currentChannel ?: return@setOnClickListener
            if (favorites.contains(ch.url)) {
                favorites.remove(ch.url)
                showToast("Eliminat din favorite: ${ch.name}")
            } else {
                favorites.add(ch.url)
                showToast("Adăugat la favorite: ${ch.name}")
            }
            prefs.edit().putStringSet("favorites", favorites).apply()
            updateFavoriteButtonText()
            rebuildGroupButtons()
        }

        btnOptStreamInfo?.setOnClickListener {
            val infoView = tvStreamInfo ?: return@setOnClickListener
            infoView.visibility = if (infoView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            updateStreamInfoText()
        }

        findViewById<Button>(R.id.btnAspectAuto)?.setOnClickListener {
            saveAndApplyAspect(prefs, "auto", "Auto")
        }
        findViewById<Button>(R.id.btnAspect43)?.setOnClickListener {
            saveAndApplyAspect(prefs, "4_3", "4:3")
        }
        findViewById<Button>(R.id.btnAspectFull)?.setOnClickListener {
            saveAndApplyAspect(prefs, "full", "Full ecran")
        }

        etSearchChannel?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterChannels(selectedGroup, s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        lvChannels?.setOnItemClickListener { _, _, position, _ ->
            if (position in visibleChannels.indices) {
                currentStreamIndex = position
                val ch = visibleChannels[position]
                leftChannelsPanel?.visibility = View.GONE
                playChannel(ch)
            }
        }

        findViewById<Button>(R.id.btnLangRo)?.setOnClickListener { setLanguage("ro") }
        findViewById<Button>(R.id.btnLangEn)?.setOnClickListener { setLanguage("en") }
        findViewById<Button>(R.id.btnLangRu)?.setOnClickListener { setLanguage("ru") }

        val colorChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit()
                    .putInt("contrast", seekContrast?.progress ?: 100)
                    .putInt("saturation", seekSaturation?.progress ?: 100)
                    .putInt("warmth", seekWarmth?.progress ?: 50)
                    .apply()
                applyColorMatrix()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekContrast?.setOnSeekBarChangeListener(colorChangeListener)
        seekSaturation?.setOnSeekBarChangeListener(colorChangeListener)
        seekWarmth?.setOnSeekBarChangeListener(colorChangeListener)

        btnResetColors?.setOnClickListener {
            seekContrast?.progress = 100
            seekSaturation?.progress = 100
            seekWarmth?.progress = 50
            prefs.edit()
                .putInt("contrast", 100)
                .putInt("saturation", 100)
                .putInt("warmth", 50)
                .apply()
            applyColorMatrix()
            showHud(msgColorsReset())
        }

        btnLoadPlay?.setOnClickListener {
            val input = etM3uUrl?.text?.toString()?.trim().orEmpty().ifEmpty { defaultPlaylistUrl }
            etM3uUrl?.setText(input)
            prefs.edit().putString("last_url", input).apply()
            updatePlaylistLabels(input)
            rightMenuPanel?.visibility = View.GONE
            startSource(input)
        }
    }

    private fun saveAndApplyAspect(prefs: android.content.SharedPreferences, mode: String, label: String) {
        aspectMode = mode
        prefs.edit().putString("aspect_mode", mode).apply()
        applyAspectMode(mode)
        showHud(label)
    }

    private fun applyAspectMode(mode: String) {
        val pv = playerView ?: return
        val surface = pv.videoSurfaceView as? TextureView
        when (mode) {
            "4_3" -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                val viewW = pv.width.toFloat().coerceAtLeast(1f)
                val viewH = pv.height.toFloat().coerceAtLeast(1f)
                val targetRatio = 4f / 3f
                val currentRatio = viewW / viewH
                if (currentRatio > targetRatio) {
                    surface?.scaleX = targetRatio / currentRatio
                    surface?.scaleY = 1.0f
                } else {
                    surface?.scaleX = 1.0f
                    surface?.scaleY = currentRatio / targetRatio
                }
            }
            "full" -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                surface?.scaleX = 1.0f
                surface?.scaleY = 1.0f
            }
            else -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                surface?.scaleX = 1.0f
                surface?.scaleY = 1.0f
            }
        }
    }

    private fun updatePlaylistLabels(url: String) {
        val shortName = url.substringAfterLast('/').substringBefore('?').ifEmpty { "playlist.m3u" }
        tvPlaylistBadge?.text = shortName
        tvDrawerPlaylistName?.text = shortName
    }

    private fun openLeftChannelsPanel() {
        uiHandler.removeCallbacks(hideOverlayRunnable)
        rightMenuPanel?.visibility = View.GONE
        leftChannelsPanel?.visibility = View.VISIBLE
    }

    private fun openRightMenuPanel() {
        uiHandler.removeCallbacks(hideOverlayRunnable)
        leftChannelsPanel?.visibility = View.GONE
        rightMenuPanel?.visibility = View.VISIBLE
        updateFavoriteButtonText()
    }

    private fun updateFavoriteButtonText() {
        val ch = currentChannel
        val isFav = ch != null && favorites.contains(ch.url)
        btnOptFavorite?.text = if (isFav) "★ Favorite" else "☆ Add to Favorites"
    }

    private fun cycleAudioTrack() {
        val exo = player ?: return
        val audioGroups = exo.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (audioGroups.isEmpty()) {
            showToast("O singură pistă audio")
            return
        }
        val total = audioGroups.size
        audioTrackToggleIndex = (audioTrackToggleIndex + 1) % (total + 1)
        val builder = exo.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        if (audioTrackToggleIndex < total) {
            val chosen = audioGroups[audioTrackToggleIndex]
            builder.setOverrideForType(TrackSelectionOverride(chosen.mediaTrackGroup, 0))
            val lang = chosen.mediaTrackGroup.getFormat(0).language ?: "Audio ${audioTrackToggleIndex + 1}"
            showHud("Audio: $lang")
        } else {
            showHud("Audio: Auto")
        }
        exo.trackSelectionParameters = builder.build()
    }

    private fun updateStreamInfoText() {
        val exo = player ?: return
        val videoFormat: Format? = exo.videoFormat
        val audioFormat: Format? = exo.audioFormat
        val vCodec = videoFormat?.codecs ?: videoFormat?.sampleMimeType ?: "-"
        val aCodec = audioFormat?.codecs ?: audioFormat?.sampleMimeType ?: "-"
        val w = videoFormat?.width ?: 0
        val h = videoFormat?.height ?: 0
        val resText = if (w > 0 && h > 0) "${w}x${h}" else "-"
        val bitrateMbps = if ((videoFormat?.bitrate ?: 0) > 0) {
            String.format("%.2f Mbit/s", (videoFormat!!.bitrate) / 1_000_000f)
        } else {
            "-"
        }
        tvStreamInfo?.text = "codecs: $vCodec, $aCodec\nresolution: $resText, bandwidth: $bitrateMbps"
    }

    private fun startSource(source: String) {
        updatePlaylistLabels(source)
        if (source.contains(".m3u8", ignoreCase = true) && !source.contains(".m3u?", ignoreCase = true)) {
            val single = listOf(M3uChannel(1, source, msgGeneralGroup(), source))
            setPlaylistData(single)
            playChannel(single.first())
        } else if (source.contains(".m3u", ignoreCase = true)) {
            loadM3uPlaylist(source)
        } else {
            val single = listOf(M3uChannel(1, source, msgGeneralGroup(), source))
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
                movedDuringGesture = false
                startVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            }

            MotionEvent.ACTION_MOVE -> {
                if (leftChannelsPanel?.visibility == View.VISIBLE || rightMenuPanel?.visibility == View.VISIBLE) {
                    return super.dispatchTouchEvent(ev)
                }

                val dx = ev.x - gestureStartX
                val dy = gestureStartY - ev.y
                if (abs(dx) > 20f || abs(dy) > 20f) {
                    movedDuringGesture = true
                }

                if (gestureMode == null && gestureStartX <= 64f && dx > 45f && abs(dx) > abs(dy) * 1.25f) {
                    gestureMode = "drawer"
                    openLeftChannelsPanel()
                    return true
                }

                if (gestureMode == null && abs(dy) > 28f && abs(dy) > abs(dx)) {
                    gestureMode = if (gestureStartX >= width * 0.5f) "volume" else "brightness"
                }

                if (gestureMode == "volume") {
                    val deltaSteps = ((dy / (height * 0.5f)) * maxVolume).toInt()
                    val newVol = (startVolume + deltaSteps).coerceIn(0, maxVolume)
                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
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
                if (gestureMode == null && !movedDuringGesture &&
                    leftChannelsPanel?.visibility != View.VISIBLE && rightMenuPanel?.visibility != View.VISIBLE) {
                    showOverlayTemporarily(2500L)
                }
                gestureMode = null
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun showOverlayTemporarily(durationMs: Long) {
        hideSystemBars()
        channelInfoCard?.visibility = View.VISIBLE
        topRightHud?.visibility = View.VISIBLE
        btnPlaybackControls?.visibility = View.VISIBLE
        uiHandler.removeCallbacks(hideOverlayRunnable)
        uiHandler.postDelayed(hideOverlayRunnable, durationMs)
    }

    private fun setPlaylistData(channels: List<M3uChannel>) {
        allChannels = channels
        selectedGroup = "ALL"
        rebuildGroupButtons()
        filterChannels("ALL", etSearchChannel?.text?.toString().orEmpty())
    }

    private fun rebuildGroupButtons() {
        val container = groupsContainer ?: return
        container.removeAllViews()
        val groups = buildList {
            add("ALL")
            if (favorites.isNotEmpty()) add("FAV")
            addAll(allChannels.map { it.group }.distinct())
        }
        for (grp in groups) {
            val label = when (grp) {
                "ALL" -> msgAllGroups()
                "FAV" -> "★"
                else -> grp
            }
            val btn = Button(this).apply {
                text = label
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundColor(
                    if (grp == selectedGroup) Color.parseColor("#0E7490")
                    else Color.parseColor("#23272F")
                )
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
                    filterChannels(grp, etSearchChannel?.text?.toString().orEmpty())
                }
            }
            container.addView(btn)
        }
    }

    private fun filterChannels(group: String, query: String) {
        val byGroup = when (group) {
            "ALL" -> allChannels
            "FAV" -> allChannels.filter { favorites.contains(it.url) }
            else -> allChannels.filter { it.group == group }
        }
        val trimmedQuery = query.trim()
        visibleChannels = if (trimmedQuery.isEmpty()) {
            byGroup
        } else {
            byGroup.filter {
                it.name.contains(trimmedQuery, ignoreCase = true) ||
                    it.number.toString() == trimmedQuery
            }
        }
        val rows = visibleChannels.map { ch ->
            val star = if (favorites.contains(ch.url)) " ★" else ""
            "${ch.number}    ${ch.name}$star"
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
        lvChannels?.adapter = adapter
    }

    private fun playChannel(channel: M3uChannel) {
        currentChannel = channel
        tvTopChannelMini?.text = channel.name
        tvGroupBadge?.text = "▶ ${channel.group}"
        tvCardChannelName?.text = channel.name
        tvCardChannelNumber?.text = channel.number.toString()
        updateFavoriteButtonText()
        showOverlayTemporarily(2500L)

        val exo = player ?: return
        exo.setMediaItem(MediaItem.fromUri(channel.url))
        exo.prepare()
        exo.playWhenReady = true
    }

    private fun applyColorMatrix() {
        val videoSurface = playerView?.videoSurfaceView as? TextureView ?: return

        val contrast = (seekContrast?.progress ?: 100) / 100f
        val saturation = (seekSaturation?.progress ?: 100) / 100f
        val warmth = ((seekWarmth?.progress ?: 50) - 50) / 50f

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
        val hud = tvGestureHud ?: return
        hud.text = text
        hud.visibility = View.VISIBLE
        uiHandler.removeCallbacks(hideHudRunnable)
        uiHandler.postDelayed(hideHudRunnable, 1000L)
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
                btnQuickChannels?.text = "Channels"
                btnQuickMenu?.text = "Menu"
                btnPlaybackControls?.text = "Playback controls"
                etSearchChannel?.hint = "🔍 Search channel..."
                tvRightMenuTitle?.text = "Menu & Settings"
                tvMenuHeading?.text = "Playlist M3U / M3U8"
                etM3uUrl?.hint = "M3U / M3U8 URL"
                btnLoadPlay?.text = "Load playlist"
                tvLanguageLabel?.text = "Language"
                tvAspectLabel?.text = "Screen format"
                findViewById<Button>(R.id.btnAspectFull)?.text = "Full screen"
                tvContrastLabel?.text = "Contrast"
                tvSaturationLabel?.text = "Saturation"
                tvWarmthLabel?.text = "Color warmth"
                btnResetColors?.text = "Reset colors"
            }
            "ru" -> {
                btnQuickChannels?.text = "Каналы"
                btnQuickMenu?.text = "Меню"
                btnPlaybackControls?.text = "Управление"
                etSearchChannel?.hint = "🔍 Поиск канала..."
                tvRightMenuTitle?.text = "Меню и настройки"
                tvMenuHeading?.text = "Плейлист M3U / M3U8"
                etM3uUrl?.hint = "Ссылка M3U / M3U8"
                btnLoadPlay?.text = "Загрузить список"
                tvLanguageLabel?.text = "Язык"
                tvAspectLabel?.text = "Формат экрана"
                findViewById<Button>(R.id.btnAspectFull)?.text = "Весь экран"
                tvContrastLabel?.text = "Контрастность"
                tvSaturationLabel?.text = "Насыщенность"
                tvWarmthLabel?.text = "Теплота цвета"
                btnResetColors?.text = "Сброс цветов"
            }
            else -> {
                btnQuickChannels?.text = "Canale"
                btnQuickMenu?.text = "Meniu"
                btnPlaybackControls?.text = "Playback controls"
                etSearchChannel?.hint = "🔍 Caută canal..."
                tvRightMenuTitle?.text = "Meniu & Setări"
                tvMenuHeading?.text = "Playlist M3U / M3U8"
                etM3uUrl?.hint = "Link M3U / M3U8"
                btnLoadPlay?.text = "Încarcă lista"
                tvLanguageLabel?.text = "Limba"
                tvAspectLabel?.text = "Format ecran"
                findViewById<Button>(R.id.btnAspectFull)?.text = "Full ecran"
                tvContrastLabel?.text = "Contrast"
                tvSaturationLabel?.text = "Saturație"
                tvWarmthLabel?.text = "Căldură culori"
                btnResetColors?.text = "Reset culori"
            }
        }
        updateFavoriteButtonText()
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
                        showToast("#${next.number} (${error.errorCodeName})")
                        playChannel(next)
                    } else {
                        showToast("${msgDownloadError()}: ${error.errorCodeName}")
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                runOnUiThread {
                    applyAspectMode(aspectMode)
                    applyColorMatrix()
                    updateStreamInfoText()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                runOnUiThread {
                    updateStreamInfoText()
                }
            }
        })
        player = exo
        playerView?.player = exo
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
                            val channelNum = parsed.size + 1
                            val channelName = pendingName ?: "Channel $channelNum"
                            parsed.add(M3uChannel(channelNum, channelName, pendingGroup, trimmed))
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
        uiHandler.removeCallbacks(hideHudRunnable)
        uiHandler.removeCallbacks(hideOverlayRunnable)
        player?.release()
        player = null
    }
}
