package com.example.smartm3uplayer

import android.content.Context
import android.content.SharedPreferences
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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class M3uChannel(
    val number: Int,
    val name: String,
    val group: String,
    val url: String,
    val tvgId: String = "",
    val tvgName: String = ""
)

data class EpgProgram(
    val channelId: String,
    val startMillis: Long,
    val stopMillis: Long,
    val title: String
)

data class SavedPlaylistEntry(
    val name: String,
    val url: String
)

private data class AudioTrackOption(
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String
)

class MainActivity : AppCompatActivity() {

    private val defaultPlaylistUrl = "https://raw.githubusercontent.com/slava-67/TV/refs/heads/IPTV/kino-plus.m3u"

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var tvTopChannelMini: TextView? = null
    private var tvPlaylistBadge: TextView? = null
    private var tvGroupBadge: TextView? = null
    private var tvCardChannelName: TextView? = null
    private var tvCardChannelNumber: TextView? = null
    private var tvEpgNow: TextView? = null
    private var tvEpgNext: TextView? = null
    private var channelInfoCard: LinearLayout? = null
    private var topRightHud: LinearLayout? = null
    private var btnPlaybackControls: Button? = null
    private var btnQuickChannels: Button? = null
    private var btnQuickPlaylist: Button? = null
    private var btnQuickMenu: Button? = null
    private var tvGestureHud: TextView? = null

    private var leftChannelsPanel: LinearLayout? = null
    private var tvDrawerPlaylistName: TextView? = null
    private var btnCloseLeftPanel: Button? = null
    private var etSearchChannel: EditText? = null
    private var groupsContainer: LinearLayout? = null
    private var lvChannels: ListView? = null

    private var playlistMenuPanel: ScrollView? = null
    private var tvPlaylistMenuTitle: TextView? = null
    private var btnClosePlaylistPanel: Button? = null
    private var tvPlaylistHeading: TextView? = null
    private var etPlaylistName: EditText? = null
    private var etM3uUrl: EditText? = null
    private var btnSaveOnlyPlaylist: Button? = null
    private var btnLoadPlay: Button? = null
    private var tvSavedPlaylistsHeading: TextView? = null
    private var savedPlaylistsContainer: LinearLayout? = null
    private var tvEpgHeading: TextView? = null
    private var etEpgUrl: EditText? = null
    private var btnLoadEpg: Button? = null
    private var tvEpgStatus: TextView? = null

    private var rightMenuPanel: ScrollView? = null
    private var tvRightMenuTitle: TextView? = null
    private var btnCloseRightPanel: Button? = null
    private var btnOptAudioTrack: Button? = null
    private var btnOptFavorite: Button? = null
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
        if (!anySidePanelVisible()) {
            channelInfoCard?.visibility = View.GONE
            topRightHud?.visibility = View.GONE
            btnPlaybackControls?.visibility = View.GONE
            hideSystemBars()
        }
    }

    private val panelAutoCloseMs = 6000L
    private val autoClosePanelRunnable = Runnable {
        closeAllSidePanels()
        showOverlayTemporarily(2500L)
    }

    private var currentLang: String = "ro"
    private var aspectMode: String = "auto"
    private var allChannels: List<M3uChannel> = emptyList()
    private var visibleChannels: List<M3uChannel> = emptyList()
    private var epgByChannel: Map<String, List<EpgProgram>> = emptyMap()
    private var favorites: MutableSet<String> = mutableSetOf()
    private var savedPlaylists: MutableList<SavedPlaylistEntry> = mutableListOf()
    private var activePlaylistUrl: String = ""
    private var selectedGroup: String = "ALL"
    private var currentChannel: M3uChannel? = null
    private var currentStreamIndex: Int = 0
    private var isLoadingPlaylist: Boolean = false
    private var audioTrackCycleIndex: Int = 0
    private var prefsRef: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            hideSystemBars()
            bindViews()

            audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            maxVolume = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)

            val prefs = getSharedPreferences("smart_m3u_prefs", Context.MODE_PRIVATE)
            prefsRef = prefs
            currentLang = prefs.getString("lang", "ro") ?: "ro"
            aspectMode = prefs.getString("aspect_mode", "auto") ?: "auto"
            favorites = (prefs.getStringSet("favorites", emptySet()) ?: emptySet()).toMutableSet()

            savedPlaylists = loadSavedPlaylistsFromPrefs(prefs)
            if (savedPlaylists.isEmpty()) {
                savedPlaylists.add(SavedPlaylistEntry(derivePlaylistLabel(defaultPlaylistUrl), defaultPlaylistUrl))
                savePlaylistsToPrefs(prefs)
            }

            val savedActive = prefs.getString("active_playlist_url", null)
                ?.takeIf { it.isNotBlank() }
                ?: prefs.getString("last_url", null)?.takeIf { it.isNotBlank() }
                ?: savedPlaylists.first().url

            activePlaylistUrl = savedActive
            val savedEpgUrl = prefs.getString("last_epg_url", "").orEmpty()

            etM3uUrl?.setText(activePlaylistUrl)
            etPlaylistName?.setText(findPlaylistLabel(activePlaylistUrl))
            etEpgUrl?.setText(savedEpgUrl)
            updatePlaylistLabels(activePlaylistUrl)
            rebuildSavedPlaylistsUi(prefs)

            seekContrast?.progress = prefs.getInt("contrast", 100)
            seekSaturation?.progress = prefs.getInt("saturation", 100)
            seekWarmth?.progress = prefs.getInt("warmth", 50)

            applyLanguage(currentLang)
            initPlayer()
            applyAspectMode(aspectMode)
            applyColorMatrix()
            setupListeners(prefs)
            showOverlayTemporarily(2500L)

            if (savedEpgUrl.isNotBlank()) {
                loadEpgXmltv(savedEpgUrl)
            }
            activateAndLoadPlaylist(activePlaylistUrl, findPlaylistLabel(activePlaylistUrl), saveToList = false, showToast = false)
        } catch (_: Exception) {
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

    private fun anySidePanelVisible(): Boolean {
        return leftChannelsPanel?.visibility == View.VISIBLE ||
            playlistMenuPanel?.visibility == View.VISIBLE ||
            rightMenuPanel?.visibility == View.VISIBLE
    }

    private fun closeAllSidePanels() {
        leftChannelsPanel?.visibility = View.GONE
        playlistMenuPanel?.visibility = View.GONE
        rightMenuPanel?.visibility = View.GONE
    }

    private fun resetPanelAutoCloseTimer() {
        uiHandler.removeCallbacks(autoClosePanelRunnable)
        if (anySidePanelVisible()) {
            uiHandler.postDelayed(autoClosePanelRunnable, panelAutoCloseMs)
        }
    }

    private fun bindViews() {
        playerView = findViewById(R.id.playerView)
        tvTopChannelMini = findViewById(R.id.tvTopChannelMini)
        tvPlaylistBadge = findViewById(R.id.tvPlaylistBadge)
        tvGroupBadge = findViewById(R.id.tvGroupBadge)
        tvCardChannelName = findViewById(R.id.tvCardChannelName)
        tvCardChannelNumber = findViewById(R.id.tvCardChannelNumber)
        tvEpgNow = findViewById(R.id.tvEpgNow)
        tvEpgNext = findViewById(R.id.tvEpgNext)
        channelInfoCard = findViewById(R.id.channelInfoCard)
        topRightHud = findViewById(R.id.topRightHud)
        btnPlaybackControls = findViewById(R.id.btnPlaybackControls)
        btnQuickChannels = findViewById(R.id.btnQuickChannels)
        btnQuickPlaylist = findViewById(R.id.btnQuickPlaylist)
        btnQuickMenu = findViewById(R.id.btnQuickMenu)
        tvGestureHud = findViewById(R.id.tvGestureHud)

        leftChannelsPanel = findViewById(R.id.leftChannelsPanel)
        tvDrawerPlaylistName = findViewById(R.id.tvDrawerPlaylistName)
        btnCloseLeftPanel = findViewById(R.id.btnCloseLeftPanel)
        etSearchChannel = findViewById(R.id.etSearchChannel)
        groupsContainer = findViewById(R.id.groupsContainer)
        lvChannels = findViewById(R.id.lvChannels)

        playlistMenuPanel = findViewById(R.id.playlistMenuPanel)
        tvPlaylistMenuTitle = findViewById(R.id.tvPlaylistMenuTitle)
        btnClosePlaylistPanel = findViewById(R.id.btnClosePlaylistPanel)
        tvPlaylistHeading = findViewById(R.id.tvPlaylistHeading)
        etPlaylistName = findViewById(R.id.etPlaylistName)
        etM3uUrl = findViewById(R.id.etM3uUrl)
        btnSaveOnlyPlaylist = findViewById(R.id.btnSaveOnlyPlaylist)
        btnLoadPlay = findViewById(R.id.btnLoadPlay)
        tvSavedPlaylistsHeading = findViewById(R.id.tvSavedPlaylistsHeading)
        savedPlaylistsContainer = findViewById(R.id.savedPlaylistsContainer)
        tvEpgHeading = findViewById(R.id.tvEpgHeading)
        etEpgUrl = findViewById(R.id.etEpgUrl)
        btnLoadEpg = findViewById(R.id.btnLoadEpg)
        tvEpgStatus = findViewById(R.id.tvEpgStatus)

        rightMenuPanel = findViewById(R.id.rightMenuPanel)
        tvRightMenuTitle = findViewById(R.id.tvRightMenuTitle)
        btnCloseRightPanel = findViewById(R.id.btnCloseRightPanel)
        btnOptAudioTrack = findViewById(R.id.btnOptAudioTrack)
        btnOptFavorite = findViewById(R.id.btnOptFavorite)
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

    private fun derivePlaylistLabel(url: String): String {
        val clean = url.substringBefore('?').substringAfterLast('/').trim()
        return clean.ifEmpty { "Playlist" }
    }

    private fun findPlaylistLabel(url: String): String {
        return savedPlaylists.firstOrNull { it.url == url }?.name ?: derivePlaylistLabel(url)
    }

    private fun loadSavedPlaylistsFromPrefs(prefs: SharedPreferences): MutableList<SavedPlaylistEntry> {
        val raw = prefs.getString("saved_playlists_json", null)
        val result = mutableListOf<SavedPlaylistEntry>()
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val u = obj.optString("url", "").trim()
                    if (u.isNotEmpty()) {
                        val n = obj.optString("name", "").trim().ifEmpty { derivePlaylistLabel(u) }
                        result.add(SavedPlaylistEntry(n, u))
                    }
                }
            } catch (_: Exception) {
            }
        }
        return result
    }

    private fun savePlaylistsToPrefs(prefs: SharedPreferences) {
        val arr = JSONArray()
        for (item in savedPlaylists) {
            val obj = JSONObject().apply {
                put("name", item.name)
                put("url", item.url)
            }
            arr.put(obj)
        }
        prefs.edit().putString("saved_playlists_json", arr.toString()).apply()
    }

    private fun upsertPlaylist(nameInput: String, urlInput: String): SavedPlaylistEntry {
        val cleanUrl = urlInput.trim()
        val cleanName = nameInput.trim().ifEmpty { derivePlaylistLabel(cleanUrl) }
        val existingIdx = savedPlaylists.indexOfFirst { it.url.equals(cleanUrl, ignoreCase = true) }
        val entry = SavedPlaylistEntry(cleanName, cleanUrl)
        if (existingIdx >= 0) {
            savedPlaylists[existingIdx] = entry
        } else {
            savedPlaylists.add(0, entry)
        }
        prefsRef?.let { savePlaylistsToPrefs(it) }
        return entry
    }

    private fun deletePlaylistAt(index: Int, prefs: SharedPreferences) {
        if (index !in savedPlaylists.indices) return
        val removed = savedPlaylists.removeAt(index)
        savePlaylistsToPrefs(prefs)
        if (activePlaylistUrl.equals(removed.url, ignoreCase = true)) {
            val nextUrl = savedPlaylists.firstOrNull()?.url ?: ""
            activePlaylistUrl = nextUrl
            prefs.edit().putString("active_playlist_url", nextUrl).putString("last_url", nextUrl).apply()
            etM3uUrl?.setText(nextUrl)
            etPlaylistName?.setText(if (nextUrl.isNotEmpty()) findPlaylistLabel(nextUrl) else "")
            if (nextUrl.isNotEmpty()) {
                activateAndLoadPlaylist(nextUrl, findPlaylistLabel(nextUrl), saveToList = false, showToast = false)
            } else {
                allChannels = emptyList()
                visibleChannels = emptyList()
                lvChannels?.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, emptyList<String>())
                player?.stop()
                updatePlaylistLabels("")
            }
        }
        rebuildSavedPlaylistsUi(prefs)
    }

    private fun rebuildSavedPlaylistsUi(prefs: SharedPreferences) {
        val container = savedPlaylistsContainer ?: return
        container.removeAllViews()
        if (savedPlaylists.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = msgNoSavedPlaylists()
                textColor = Color.parseColor("#94A3B8")
                textSize = 12f
            }
            container.addView(emptyTv)
            return
        }

        savedPlaylists.forEachIndexed { idx, entry ->
            val isActive = entry.url.equals(activePlaylistUrl, ignoreCase = true)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundColor(
                    if (isActive) Color.parseColor("#660E7490")
                    else Color.parseColor("#22FFFFFF")
                )
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 6
                }
                layoutParams = lp
                setPadding(10, 6, 8, 6)
            }

            val btnSelect = Button(this).apply {
                text = if (isActive) "▶ ${entry.name}" else entry.name
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.TRANSPARENT)
                gravity = android.view.Gravity.LEFT or android.view.Gravity.CENTER_VERTICAL
                isAllCaps = false
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnClickListener {
                    resetPanelAutoCloseTimer()
                    etPlaylistName?.setText(entry.name)
                    etM3uUrl?.setText(entry.url)
                    activateAndLoadPlaylist(entry.url, entry.name, saveToList = true, showToast = true)
                    playlistMenuPanel?.visibility = View.GONE
                }
            }

            val btnDelete = Button(this).apply {
                text = "✕"
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#44EF4444"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    36
                ).apply {
                    leftMargin = 8
                }
                setOnClickListener {
                    resetPanelAutoCloseTimer()
                    deletePlaylistAt(idx, prefs)
                }
            }

            row.addView(btnSelect)
            row.addView(btnDelete)
            container.addView(row)
        }
    }

    private fun setupListeners(prefs: SharedPreferences) {
        btnQuickChannels?.setOnClickListener { openLeftChannelsPanel() }
        btnCloseLeftPanel?.setOnClickListener {
            uiHandler.removeCallbacks(autoClosePanelRunnable)
            leftChannelsPanel?.visibility = View.GONE
            showOverlayTemporarily(2500L)
        }

        btnQuickPlaylist?.setOnClickListener { openPlaylistMenuPanel() }
        btnClosePlaylistPanel?.setOnClickListener {
            uiHandler.removeCallbacks(autoClosePanelRunnable)
            playlistMenuPanel?.visibility = View.GONE
            showOverlayTemporarily(2500L)
        }

        btnQuickMenu?.setOnClickListener { openRightMenuPanel() }
        btnCloseRightPanel?.setOnClickListener {
            uiHandler.removeCallbacks(autoClosePanelRunnable)
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

        btnOptAudioTrack?.setOnClickListener {
            resetPanelAutoCloseTimer()
            cycleAudioTrack()
        }

        btnOptFavorite?.setOnClickListener {
            resetPanelAutoCloseTimer()
            val ch = currentChannel ?: return@setOnClickListener
            if (favorites.contains(ch.url)) {
                favorites.remove(ch.url)
            } else {
                favorites.add(ch.url)
            }
            prefs.edit().putStringSet("favorites", favorites).apply()
            updateFavoriteButtonText()
            rebuildGroupButtons()
        }

        findViewById<Button>(R.id.btnAspectAuto)?.setOnClickListener {
            resetPanelAutoCloseTimer()
            saveAndApplyAspect(prefs, "auto", "Auto")
        }
        findViewById<Button>(R.id.btnAspect43)?.setOnClickListener {
            resetPanelAutoCloseTimer()
            saveAndApplyAspect(prefs, "4_3", "4:3")
        }
        findViewById<Button>(R.id.btnAspectFull)?.setOnClickListener {
            resetPanelAutoCloseTimer()
            saveAndApplyAspect(prefs, "full", "Full ecran")
        }

        etSearchChannel?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                resetPanelAutoCloseTimer()
                filterChannels(selectedGroup, s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        lvChannels?.setOnItemClickListener { _, _, position, _ ->
            uiHandler.removeCallbacks(autoClosePanelRunnable)
            if (position in visibleChannels.indices) {
                currentStreamIndex = position
                val ch = visibleChannels[position]
                leftChannelsPanel?.visibility = View.GONE
                playChannel(ch)
            }
        }

        findViewById<Button>(R.id.btnLangRo)?.setOnClickListener {
            resetPanelAutoCloseTimer()
            setLanguage("ro")
        }
        findViewById<Button>(R.id.btnLangEn)?.setOnClickListener {
            resetPanelAutoCloseTimer()
            setLanguage("en")
        }
        findViewById<Button>(R.id.btnLangRu)?.setOnClickListener {
            resetPanelAutoCloseTimer()
            setLanguage("ru")
        }

        val colorChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                resetPanelAutoCloseTimer()
                prefs.edit()
                    .putInt("contrast", seekContrast?.progress ?: 100)
                    .putInt("saturation", seekSaturation?.progress ?: 100)
                    .putInt("warmth", seekWarmth?.progress ?: 50)
                    .apply()
                applyColorMatrix()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                resetPanelAutoCloseTimer()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                resetPanelAutoCloseTimer()
            }
        }

        seekContrast?.setOnSeekBarChangeListener(colorChangeListener)
        seekSaturation?.setOnSeekBarChangeListener(colorChangeListener)
        seekWarmth?.setOnSeekBarChangeListener(colorChangeListener)

        btnResetColors?.setOnClickListener {
            resetPanelAutoCloseTimer()
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

        btnSaveOnlyPlaylist?.setOnClickListener {
            resetPanelAutoCloseTimer()
            val urlInput = etM3uUrl?.text?.toString()?.trim().orEmpty()
            if (urlInput.isEmpty()) {
                Toast.makeText(this, msgEnterUrl(), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val nameInput = etPlaylistName?.text?.toString()?.trim().orEmpty()
            upsertPlaylist(nameInput, urlInput)
            rebuildSavedPlaylistsUi(prefs)
            Toast.makeText(this, msgPlaylistSaved(), Toast.LENGTH_SHORT).show()
        }

        btnLoadPlay?.setOnClickListener {
            uiHandler.removeCallbacks(autoClosePanelRunnable)
            val urlInput = etM3uUrl?.text?.toString()?.trim().orEmpty()
            if (urlInput.isEmpty()) {
                Toast.makeText(this, msgEnterUrl(), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val nameInput = etPlaylistName?.text?.toString()?.trim().orEmpty()
            playlistMenuPanel?.visibility = View.GONE
            activateAndLoadPlaylist(urlInput, nameInput, saveToList = true, showToast = true)
        }

        btnLoadEpg?.setOnClickListener {
            resetPanelAutoCloseTimer()
            val epgInput = etEpgUrl?.text?.toString()?.trim().orEmpty()
            prefs.edit().putString("last_epg_url", epgInput).apply()
            if (epgInput.isNotBlank()) {
                loadEpgXmltv(epgInput)
            } else {
                epgByChannel = emptyMap()
                tvEpgStatus?.text = ""
                updateEpgForChannel(currentChannel)
            }
        }
    }

    private fun activateAndLoadPlaylist(
        rawUrl: String,
        customName: String,
        saveToList: Boolean,
        showToast: Boolean
    ) {
        val cleanUrl = rawUrl.trim()
        if (cleanUrl.isEmpty()) return

        val entry = if (saveToList) {
            upsertPlaylist(customName, cleanUrl)
        } else {
            SavedPlaylistEntry(customName.ifEmpty { derivePlaylistLabel(cleanUrl) }, cleanUrl)
        }

        activePlaylistUrl = entry.url
        prefsRef?.edit()
            ?.putString("active_playlist_url", entry.url)
            ?.putString("last_url", entry.url)
            ?.apply()

        etM3uUrl?.setText(entry.url)
        etPlaylistName?.setText(entry.name)
        updatePlaylistLabels(entry.url, entry.name)
        prefsRef?.let { rebuildSavedPlaylistsUi(it) }

        if (showToast) {
            Toast.makeText(this, "${msgLoadingPlaylist()}: ${entry.name}", Toast.LENGTH_SHORT).show()
        }
        startSource(entry.url, entry.name)
    }

    private fun saveAndApplyAspect(prefs: SharedPreferences, mode: String, label: String) {
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

    private fun updatePlaylistLabels(url: String, customName: String? = null) {
        val label = customName?.trim()?.takeIf { it.isNotEmpty() }
            ?: findPlaylistLabel(url).takeIf { it.isNotEmpty() }
            ?: if (url.isNotEmpty()) derivePlaylistLabel(url) else "Playlist"
        tvPlaylistBadge?.text = label
        tvDrawerPlaylistName?.text = label
    }

    private fun openLeftChannelsPanel() {
        uiHandler.removeCallbacks(hideOverlayRunnable)
        closeAllSidePanels()
        leftChannelsPanel?.visibility = View.VISIBLE
        resetPanelAutoCloseTimer()
    }

    private fun openPlaylistMenuPanel() {
        uiHandler.removeCallbacks(hideOverlayRunnable)
        closeAllSidePanels()
        playlistMenuPanel?.visibility = View.VISIBLE
        prefsRef?.let { rebuildSavedPlaylistsUi(it) }
        resetPanelAutoCloseTimer()
    }

    private fun openRightMenuPanel() {
        uiHandler.removeCallbacks(hideOverlayRunnable)
        closeAllSidePanels()
        rightMenuPanel?.visibility = View.VISIBLE
        updateFavoriteButtonText()
        resetPanelAutoCloseTimer()
    }

    private fun updateFavoriteButtonText() {
        val ch = currentChannel
        val isFav = ch != null && favorites.contains(ch.url)
        btnOptFavorite?.text = if (isFav) "★ Favorite" else "☆ Add to Favorites"
    }

    private fun collectAudioTrackOptions(exo: ExoPlayer): List<AudioTrackOption> {
        val options = mutableListOf<AudioTrackOption>()
        var idx = 1
        for (group in exo.currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val fmt: Format = group.getTrackFormat(i)
                    val lang = fmt.language?.takeIf { it.isNotBlank() && it != "und" }
                    val labelPart = fmt.label?.takeIf { it.isNotBlank() }
                    val name = when {
                        lang != null && labelPart != null -> "${lang.uppercase()} ($labelPart)"
                        lang != null -> lang.uppercase()
                        labelPart != null -> labelPart
                        else -> "Audio $idx"
                    }
                    options.add(AudioTrackOption(group, i, name))
                    idx++
                }
            }
        }
        return options
    }

    private fun cycleAudioTrack() {
        val exo = player ?: return
        val options = collectAudioTrackOptions(exo)
        if (options.size <= 1) {
            showHud("Audio: Auto")
            return
        }
        audioTrackCycleIndex = (audioTrackCycleIndex + 1) % (options.size + 1)
        val builder = exo.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)

        if (audioTrackCycleIndex < options.size) {
            val chosen = options[audioTrackCycleIndex]
            builder.setOverrideForType(TrackSelectionOverride(chosen.group.mediaTrackGroup, chosen.trackIndex))
            showHud("Audio: ${chosen.label}")
        } else {
            showHud("Audio: Auto")
        }
        exo.trackSelectionParameters = builder.build()
    }

    private fun updateEpgForChannel(channel: M3uChannel?) {
        if (channel == null || epgByChannel.isEmpty()) {
            tvEpgNow?.text = ""
            tvEpgNext?.text = ""
            tvEpgNow?.visibility = View.GONE
            tvEpgNext?.visibility = View.GONE
            return
        }

        val keys = listOfNotNull(
            channel.tvgId.takeIf { it.isNotBlank() },
            channel.tvgName.takeIf { it.isNotBlank() },
            channel.name.takeIf { it.isNotBlank() }
        ).map { it.lowercase(Locale.ROOT) }

        val programs = keys.firstNotNullOfOrNull { key -> epgByChannel[key] }.orEmpty()
        if (programs.isEmpty()) {
            tvEpgNow?.text = ""
            tvEpgNext?.text = ""
            tvEpgNow?.visibility = View.GONE
            tvEpgNext?.visibility = View.GONE
            return
        }

        val now = System.currentTimeMillis()
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentIdx = programs.indexOfFirst { now >= it.startMillis && now < it.stopMillis }
            .takeIf { it >= 0 }
            ?: programs.indexOfFirst { it.startMillis >= now }

        if (currentIdx >= 0) {
            val cur = programs[currentIdx]
            val startStr = timeFmt.format(Date(cur.startMillis))
            val stopStr = timeFmt.format(Date(cur.stopMillis))
            tvEpgNow?.text = "EPG $startStr-$stopStr  ${cur.title}"
            tvEpgNow?.visibility = View.VISIBLE

            val next = programs.getOrNull(currentIdx + 1)
            if (next != null) {
                val nextStart = timeFmt.format(Date(next.startMillis))
                tvEpgNext?.text = "Urmează $nextStart  ${next.title}"
                tvEpgNext?.visibility = View.VISIBLE
            } else {
                tvEpgNext?.text = ""
                tvEpgNext?.visibility = View.GONE
            }
        } else {
            tvEpgNow?.text = ""
            tvEpgNext?.text = ""
            tvEpgNow?.visibility = View.GONE
            tvEpgNext?.visibility = View.GONE
        }
    }

    private fun startSource(source: String, playlistLabel: String) {
        updatePlaylistLabels(source, playlistLabel)
        loadM3uOrStream(source, playlistLabel)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val width = resources.displayMetrics.widthPixels.toFloat().coerceAtLeast(1f)
        val height = resources.displayMetrics.heightPixels.toFloat().coerceAtLeast(1f)

        if (anySidePanelVisible()) {
            resetPanelAutoCloseTimer()
            return super.dispatchTouchEvent(ev)
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartX = ev.x
                gestureStartY = ev.y
                gestureMode = null
                movedDuringGesture = false
                startVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            }

            MotionEvent.ACTION_MOVE -> {
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
                if (gestureMode == null && !movedDuringGesture) {
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
        updateEpgForChannel(currentChannel)
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
                    resetPanelAutoCloseTimer()
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
        audioTrackCycleIndex = 0
        tvTopChannelMini?.text = channel.name
        tvGroupBadge?.text = "▶ ${channel.group}"
        tvCardChannelName?.text = channel.name
        tvCardChannelNumber?.text = channel.number.toString()
        updateFavoriteButtonText()
        updateEpgForChannel(channel)
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
        prefsRef?.let { rebuildSavedPlaylistsUi(it) }
    }

    private fun applyLanguage(lang: String) {
        when (lang) {
            "en" -> {
                btnQuickChannels?.text = "Channels"
                btnQuickPlaylist?.text = "Playlist"
                btnQuickMenu?.text = "Menu"
                btnPlaybackControls?.text = "Playback controls"
                etSearchChannel?.hint = "🔍 Search channel..."
                tvPlaylistMenuTitle?.text = "Playlist & EPG"
                tvPlaylistHeading?.text = "Add or load Playlist"
                etPlaylistName?.hint = "Playlist name (optional)"
                etM3uUrl?.hint = "M3U / M3U8 / stream URL"
                btnSaveOnlyPlaylist?.text = "Save"
                btnLoadPlay?.text = "Save & Load"
                tvSavedPlaylistsHeading?.text = "Saved playlists"
                tvEpgHeading?.text = "EPG Guide (XMLTV)"
                etEpgUrl?.hint = "EPG XML / XMLTV URL"
                btnLoadEpg?.text = "Update EPG"
                tvRightMenuTitle?.text = "Menu & Settings"
                btnOptAudioTrack?.text = "Audio / Languages"
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
                btnQuickPlaylist?.text = "Плейлист"
                btnQuickMenu?.text = "Меню"
                btnPlaybackControls?.text = "Управление"
                etSearchChannel?.hint = "🔍 Поиск канала..."
                tvPlaylistMenuTitle?.text = "Плейлист и EPG"
                tvPlaylistHeading?.text = "Добавить или загрузить плейлист"
                etPlaylistName?.hint = "Название плейлиста (необязательно)"
                etM3uUrl?.hint = "Ссылка M3U / M3U8 / поток"
                btnSaveOnlyPlaylist?.text = "Сохранить"
                btnLoadPlay?.text = "Сохр. и загрузить"
                tvSavedPlaylistsHeading?.text = "Сохранённые плейлисты"
                tvEpgHeading?.text = "Телепрограмма EPG (XMLTV)"
                etEpgUrl?.hint = "Ссылка EPG XML / XMLTV"
                btnLoadEpg?.text = "Обновить EPG"
                tvRightMenuTitle?.text = "Меню и настройки"
                btnOptAudioTrack?.text = "Аудио / Языки"
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
                btnQuickPlaylist?.text = "Playlist"
                btnQuickMenu?.text = "Meniu"
                btnPlaybackControls?.text = "Playback controls"
                etSearchChannel?.hint = "🔍 Caută canal..."
                tvPlaylistMenuTitle?.text = "Playlist & EPG"
                tvPlaylistHeading?.text = "Adaugă sau încarcă Playlist"
                etPlaylistName?.hint = "Nume playlist (opțional)"
                etM3uUrl?.hint = "Link M3U / M3U8 / stream"
                btnSaveOnlyPlaylist?.text = "Salvează"
                btnLoadPlay?.text = "Salvează & Încarcă"
                tvSavedPlaylistsHeading?.text = "Playlist-uri salvate"
                tvEpgHeading?.text = "Ghid EPG (XMLTV)"
                etEpgUrl?.hint = "Link EPG XML / XMLTV"
                btnLoadEpg?.text = "Actualizează EPG"
                tvRightMenuTitle?.text = "Meniu & Setări"
                btnOptAudioTrack?.text = "Audio / Limbi"
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

    private fun msgEnterUrl(): String = when (currentLang) {
        "en" -> "Enter a playlist or stream URL"
        "ru" -> "Введите ссылку на плейлист или поток"
        else -> "Introdu un link de playlist sau stream"
    }

    private fun msgPlaylistSaved(): String = when (currentLang) {
        "en" -> "Playlist saved"
        "ru" -> "Плейлист сохранён"
        else -> "Playlist salvat"
    }

    private fun msgLoadingPlaylist(): String = when (currentLang) {
        "en" -> "Loading playlist"
        "ru" -> "Загрузка плейлиста"
        else -> "Încărcare playlist"
    }

    private fun msgPlaylistError(): String = when (currentLang) {
        "en" -> "Failed to load playlist"
        "ru" -> "Ошибка загрузки плейлиста"
        else -> "Eroare la încărcarea playlist-ului"
    }

    private fun msgNoSavedPlaylists(): String = when (currentLang) {
        "en" -> "No saved playlists"
        "ru" -> "Нет сохранённых плейлистов"
        else -> "Niciun playlist salvat"
    }

    private fun initPlayer() {
        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(
                buildUponParameters()
                    .setForceHighestSupportedBitrate(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
            )
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(8000, 30000, 1500, 3000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val exo = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()

        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                runOnUiThread {
                    if (!isLoadingPlaylist && currentStreamIndex + 1 < visibleChannels.size) {
                        currentStreamIndex++
                        val next = visibleChannels[currentStreamIndex]
                        playChannel(next)
                    } else {
                        player?.stop()
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                runOnUiThread {
                    applyAspectMode(aspectMode)
                    applyColorMatrix()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                runOnUiThread {
                    audioTrackCycleIndex = 0
                }
            }
        })
        player = exo
        playerView?.player = exo
    }

    private fun extractAttribute(line: String, attrName: String): String? {
        val match = Regex("""$attrName="([^"]*)"""", RegexOption.IGNORE_CASE).find(line)
        return match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun loadM3uOrStream(playlistUrl: String, playlistLabel: String) {
        isLoadingPlaylist = true
        val looksLikeDirectStream = playlistUrl.contains(".m3u8", ignoreCase = true) &&
            !playlistUrl.contains(".m3u?", ignoreCase = true) &&
            !playlistUrl.contains("playlist", ignoreCase = true) &&
            !playlistUrl.contains("get.php", ignoreCase = true)

        if (looksLikeDirectStream) {
            isLoadingPlaylist = false
            val single = listOf(M3uChannel(1, playlistLabel, msgGeneralGroup(), playlistUrl))
            setPlaylistData(single)
            currentStreamIndex = 0
            playChannel(single.first())
            return
        }

        Thread {
            try {
                val conn = (URL(playlistUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12000
                    readTimeout = 18000
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
                var pendingTvgId: String = ""
                var pendingTvgName: String = ""
                var discoveredEpgUrl: String? = null
                var sawM3uMarkers = false

                BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim().removePrefix("\uFEFF")
                        if (trimmed.startsWith("#EXTM3U", ignoreCase = true)) {
                            sawM3uMarkers = true
                            if (discoveredEpgUrl == null) {
                                discoveredEpgUrl = extractAttribute(trimmed, "x-tvg-url")
                                    ?: extractAttribute(trimmed, "url-tvg")
                            }
                        } else if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                            sawM3uMarkers = true
                            pendingGroup = extractAttribute(trimmed, "group-title") ?: msgGeneralGroup()
                            pendingTvgId = extractAttribute(trimmed, "tvg-id").orEmpty()
                            pendingTvgName = extractAttribute(trimmed, "tvg-name").orEmpty()
                            val commaIdx = trimmed.lastIndexOf(',')
                            pendingName = if (commaIdx != -1 && commaIdx + 1 < trimmed.length) {
                                trimmed.substring(commaIdx + 1).trim().ifEmpty { null }
                            } else {
                                null
                            }
                        } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            val channelNum = parsed.size + 1
                            val channelName = pendingName ?: "Channel $channelNum"
                            parsed.add(
                                M3uChannel(
                                    number = channelNum,
                                    name = channelName,
                                    group = pendingGroup,
                                    url = trimmed,
                                    tvgId = pendingTvgId,
                                    tvgName = pendingTvgName
                                )
                            )
                            pendingName = null
                            pendingGroup = msgGeneralGroup()
                            pendingTvgId = ""
                            pendingTvgName = ""
                        }
                        line = reader.readLine()
                    }
                }
                conn.disconnect()

                runOnUiThread {
                    isLoadingPlaylist = false
                    val manualEpg = etEpgUrl?.text?.toString()?.trim().orEmpty()
                    if (manualEpg.isBlank() && !discoveredEpgUrl.isNullOrBlank()) {
                        etEpgUrl?.setText(discoveredEpgUrl)
                        loadEpgXmltv(discoveredEpgUrl!!)
                    }

                    val finalList = if (parsed.isNotEmpty()) {
                        parsed
                    } else if (!sawM3uMarkers && (playlistUrl.startsWith("http://", true) || playlistUrl.startsWith("https://", true) || playlistUrl.startsWith("rtsp://", true))) {
                        listOf(M3uChannel(1, playlistLabel, msgGeneralGroup(), playlistUrl))
                    } else {
                        emptyList()
                    }

                    if (finalList.isNotEmpty()) {
                        setPlaylistData(finalList)
                        currentStreamIndex = 0
                        playChannel(finalList.first())
                    } else {
                        Toast.makeText(this, msgPlaylistError(), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    isLoadingPlaylist = false
                    Toast.makeText(this, msgPlaylistError(), Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun parseXmltvTimestamp(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val clean = raw.trim().substringBefore(' ')
        val pattern = when (clean.length) {
            14 -> "yyyyMMddHHmmss"
            12 -> "yyyyMMddHHmm"
            8 -> "yyyyMMdd"
            else -> return 0L
        }
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
            }
            sdf.parse(clean)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun loadEpgXmltv(epgUrl: String) {
        Thread {
            try {
                val conn = (URL(epgUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 15000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                conn.connect()
                if (conn.responseCode !in 200..299) {
                    throw Exception("HTTP ${conn.responseCode}")
                }

                val map = mutableMapOf<String, MutableList<EpgProgram>>()
                val factory = XmlPullParserFactory.newInstance().apply {
                    isNamespaceAware = false
                }
                val parser = factory.newPullParser()
                parser.setInput(InputStreamReader(conn.inputStream, "UTF-8"))

                var eventType = parser.eventType
                var curChannelId = ""
                var curStart = 0L
                var curStop = 0L
                var curTitle = ""
                var inProgramme = false
                var inTitle = false

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name.lowercase(Locale.ROOT)) {
                                "programme" -> {
                                    inProgramme = true
                                    curChannelId = parser.getAttributeValue(null, "channel")?.trim().orEmpty()
                                    curStart = parseXmltvTimestamp(parser.getAttributeValue(null, "start"))
                                    curStop = parseXmltvTimestamp(parser.getAttributeValue(null, "stop"))
                                    curTitle = ""
                                }
                                "title" -> if (inProgramme) {
                                    inTitle = true
                                }
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inProgramme && inTitle && curTitle.isEmpty()) {
                                curTitle = parser.text?.trim().orEmpty()
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            when (parser.name.lowercase(Locale.ROOT)) {
                                "title" -> inTitle = false
                                "programme" -> {
                                    if (inProgramme && curChannelId.isNotEmpty() && curTitle.isNotEmpty() && curStart > 0L) {
                                        val effectiveStop = if (curStop > curStart) curStop else curStart + 3600_000L
                                        val key = curChannelId.lowercase(Locale.ROOT)
                                        map.getOrPut(key) { mutableListOf() }
                                            .add(EpgProgram(curChannelId, curStart, effectiveStop, curTitle))
                                    }
                                    inProgramme = false
                                    inTitle = false
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                conn.disconnect()

                val sortedMap: Map<String, List<EpgProgram>> = map.mapValues { (_, list) ->
                    list.sortedBy { it.startMillis }
                }

                runOnUiThread {
                    epgByChannel = sortedMap
                    tvEpgStatus?.text = "EPG: ${sortedMap.size}"
                    updateEpgForChannel(currentChannel)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    tvEpgStatus?.text = "EPG: -"
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
        uiHandler.removeCallbacks(autoClosePanelRunnable)
        player?.release()
        player = null
    }
}
