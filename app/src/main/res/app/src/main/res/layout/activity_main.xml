<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#090D16">

    <!-- Player Video -->
    <androidx.media3.ui.PlayerView
        android:id="@+id/playerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:surface_type="texture_view"
        app:use_controller="false" />

    <!-- Logo central când nu rulează niciun canal -->
    <LinearLayout
        android:id="@+id/centerBrandLogo"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:gravity="center"
        android:orientation="vertical">

        <ImageView
            android:id="@+id/ivCenterLogo"
            android:layout_width="110dp"
            android:layout_height="110dp"
            android:contentDescription="Logo"
            android:src="@drawable/app_logo" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="10dp"
            android:text="Smart M3U Player"
            android:textColor="#E2E8F0"
            android:textSize="18sp"
            android:textStyle="bold" />
    </LinearLayout>

    <!-- HUD Stânga Sus -->
    <LinearLayout
        android:id="@+id/topLeftHud"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="top|start"
        android:layout_margin="16dp"
        android:background="#AA0F172A"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:padding="8dp">

        <ImageView
            android:id="@+id/ivTopLogo"
            android:layout_width="28dp"
            android:layout_height="28dp"
            android:contentDescription="Logo"
            android:src="@drawable/app_logo" />

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:orientation="vertical">

            <TextView
                android:id="@+id/tvTopChannelMini"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="TV ONLINE"
                android:textColor="#FFFFFF"
                android:textSize="13sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvPlaylistBadge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Niciun playlist încărcat"
                android:textColor="#38BDF8"
                android:textSize="11sp" />
        </LinearLayout>
    </LinearLayout>

    <!-- HUD Dreapta Sus: DOAR butonul Meniu -->
    <LinearLayout
        android:id="@+id/topRightHud"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="top|end"
        android:layout_margin="16dp"
        android:orientation="horizontal">

        <Button
            android:id="@+id/btnQuickMenu"
            android:layout_width="wrap_content"
            android:layout_height="40dp"
            android:backgroundTint="#0E7490"
            android:paddingHorizontal="18dp"
            android:text="Meniu"
            android:textAllCaps="false"
            android:textColor="#FFFFFF"
            android:textStyle="bold" />
    </LinearLayout>

    <!-- HUD Gesturi (Volum / Luminozitate / Aspect) -->
    <TextView
        android:id="@+id/tvGestureHud"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:background="#CC0F172A"
        android:paddingHorizontal="20dp"
        android:paddingVertical="10dp"
        android:textColor="#FFFFFF"
        android:textSize="16sp"
        android:textStyle="bold"
        android:visibility="gone" />

    <!-- Card Informații Canal (Jos Stânga) -->
    <LinearLayout
        android:id="@+id/channelInfoCard"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|start"
        android:layout_margin="16dp"
        android:background="#CC0F172A"
        android:orientation="vertical"
        android:padding="12dp">

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/tvCardChannelNumber"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="#38BDF8"
                android:textSize="18sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvCardChannelName"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="10dp"
                android:textColor="#FFFFFF"
                android:textSize="18sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvGroupBadge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="12dp"
                android:textColor="#94A3B8"
                android:textSize="12sp" />
        </LinearLayout>

        <TextView
            android:id="@+id/tvEpgNow"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textColor="#E2E8F0"
            android:textSize="13sp"
            android:visibility="gone" />

        <TextView
            android:id="@+id/tvEpgNext"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="2dp"
            android:textColor="#94A3B8"
            android:textSize="12sp"
            android:visibility="gone" />
    </LinearLayout>

    <!-- Buton Play/Pause (Jos Dreapta) -->
    <Button
        android:id="@+id/btnPlaybackControls"
        android:layout_width="wrap_content"
        android:layout_height="40dp"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:backgroundTint="#CC1E293B"
        android:text="Playback controls"
        android:textAllCaps="false"
        android:textColor="#FFFFFF" />

    <!-- Panou Stânga: Listă Canale (se deschide prin swipe din stânga sau din Meniu) -->
    <LinearLayout
        android:id="@+id/leftChannelsPanel"
        android:layout_width="320dp"
        android:layout_height="match_parent"
        android:layout_gravity="start"
        android:background="#EE0F172A"
        android:orientation="vertical"
        android:padding="12dp"
        android:visibility="gone">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/tvDrawerPlaylistName"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:ellipsize="end"
                android:maxLines="1"
                android:text="Niciun playlist încărcat"
                android:textColor="#38BDF8"
                android:textSize="15sp"
                android:textStyle="bold" />

            <Button
                android:id="@+id/btnCloseLeftPanel"
                android:layout_width="36dp"
                android:layout_height="36dp"
                android:backgroundTint="#334155"
                android:text="✕"
                android:textColor="#FFFFFF" />
        </LinearLayout>

        <EditText
            android:id="@+id/etSearchChannel"
            android:layout_width="match_parent"
            android:layout_height="40dp"
            android:layout_marginTop="8dp"
            android:background="#1E293B"
            android:hint="🔍 Caută canal..."
            android:inputType="text"
            android:paddingHorizontal="10dp"
            android:textColor="#FFFFFF"
            android:textColorHint="#94A3B8"
            android:textSize="13sp" />

        <HorizontalScrollView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:scrollbars="none">

            <LinearLayout
                android:id="@+id/groupsContainer"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal" />
        </HorizontalScrollView>

        <ListView
            android:id="@+id/lvChannels"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_marginTop="8dp"
            android:layout_weight="1"
            android:divider="#1E293B"
            android:dividerHeight="1dp" />
    </LinearLayout>

    <!-- Panou Dreapta: Meniu & Setări (inclusiv Setări Playlist) -->
    <ScrollView
        android:id="@+id/rightMenuPanel"
        android:layout_width="350dp"
        android:layout_height="match_parent"
        android:layout_gravity="end"
        android:background="#F20F172A"
        android:padding="14dp"
        android:visibility="gone">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <!-- Header Meniu -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <TextView
                    android:id="@+id/tvRightMenuTitle"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Meniu &amp; Setări"
                    android:textColor="#FFFFFF"
                    android:textSize="17sp"
                    android:textStyle="bold" />

                <Button
                    android:id="@+id/btnCloseRightPanel"
                    android:layout_width="36dp"
                    android:layout_height="36dp"
                    android:backgroundTint="#334155"
                    android:text="✕"
                    android:textColor="#FFFFFF" />
            </LinearLayout>

            <!-- Acces rapid la lista de canale -->
            <Button
                android:id="@+id/btnOpenChannelsFromMenu"
                android:layout_width="match_parent"
                android:layout_height="40dp"
                android:layout_marginTop="10dp"
                android:backgroundTint="#0E7490"
                android:text="📺 Listă canale"
                android:textAllCaps="false"
                android:textColor="#FFFFFF" />

            <!-- SECȚIUNE SETĂRI PLAYLIST -->
            <View
                android:layout_width="match_parent"
                android:layout_height="1dp"
                android:layout_marginVertical="12dp"
                android:background="#334155" />

            <TextView
                android:id="@+id/tvPlaylistHeading"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Setări Playlist"
                android:textColor="#38BDF8"
                android:textSize="15sp"
                android:textStyle="bold" />

            <EditText
                android:id="@+id/etPlaylistName"
                android:layout_width="match_parent"
                android:layout_height="40dp"
                android:layout_marginTop="8dp"
                android:background="#1E293B"
                android:hint="Nume playlist (opțional)"
                android:inputType="text"
                android:paddingHorizontal="10dp"
                android:textColor="#FFFFFF"
                android:textColorHint="#94A3B8"
                android:textSize="13sp" />

            <EditText
                android:id="@+id/etM3uUrl"
                android:layout_width="match_parent"
                android:layout_height="40dp"
                android:layout_marginTop="6dp"
                android:background="#1E293B"
                android:hint="Link M3U / M3U8 / stream"
                android:inputType="textUri"
                android:paddingHorizontal="10dp"
                android:textColor="#FFFFFF"
                android:textColorHint="#94A3B8"
                android:textSize="13sp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnSaveOnlyPlaylist"
                    android:layout_width="0dp"
                    android:layout_height="38dp"
                    android:layout_marginEnd="4dp"
                    android:layout_weight="1"
                    android:backgroundTint="#334155"
                    android:text="Salvează"
                    android:textAllCaps="false"
                    android:textColor="#FFFFFF"
                    android:textSize="12sp" />

                <Button
                    android:id="@+id/btnLoadPlay"
                    android:layout_width="0dp"
                    android:layout_height="38dp"
                    android:layout_marginStart="4dp"
                    android:layout_weight="1.3"
                    android:backgroundTint="#0284C7"
                    android:text="Salvează &amp; Încarcă"
                    android:textAllCaps="false"
                    android:textColor="#FFFFFF"
                    android:textSize="12sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <TextView
                android:id="@+id/tvSavedPlaylistsHeading"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:text="Playlist-uri salvate"
                android:textColor="#CBD5E1"
                android:textSize="13sp"
                android:textStyle="bold" />

            <LinearLayout
                android:id="@+id/savedPlaylistsContainer"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:orientation="vertical" />

            <!-- EPG XMLTV -->
            <TextView
                android:id="@+id/tvEpgHeading"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:text="Ghid EPG (XMLTV)"
                android:textColor="#CBD5E1"
                android:textSize="13sp"
                android:textStyle="bold" />

            <EditText
                android:id="@+id/etEpgUrl"
                android:layout_width="match_parent"
                android:layout_height="40dp"
                android:layout_marginTop="6dp"
                android:background="#1E293B"
                android:hint="Link EPG XML / XMLTV"
                android:inputType="textUri"
                android:paddingHorizontal="10dp"
                android:textColor="#FFFFFF"
                android:textColorHint="#94A3B8"
                android:textSize="13sp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnLoadEpg"
                    android:layout_width="wrap_content"
                    android:layout_height="36dp"
                    android:backgroundTint="#334155"
                    android:text="Actualizează EPG"
                    android:textAllCaps="false"
                    android:textColor="#FFFFFF"
                    android:textSize="12sp" />

                <TextView
                    android:id="@+id/tvEpgStatus"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="10dp"
                    android:textColor="#94A3B8"
                    android:textSize="12sp" />
            </LinearLayout>

            <!-- SECȚIUNE PLAYER & IMAGINE -->
            <View
                android:layout_width="match_parent"
                android:layout_height="1dp"
                android:layout_marginVertical="12dp"
                android:background="#334155" />

            <Button
                android:id="@+id/btnOptAudioTrack"
                android:layout_width="match_parent"
                android:layout_height="38dp"
                android:backgroundTint="#1E293B"
                android:text="Audio / Limbi"
                android:textAllCaps="false"
                android:textColor="#FFFFFF"
                android:textSize="13sp" />

            <Button
                android:id="@+id/btnOptFavorite"
                android:layout_width="match_parent"
                android:layout_height="38dp"
                android:layout_marginTop="6dp"
                android:backgroundTint="#1E293B"
                android:text="☆ Add to Favorites"
                android:textAllCaps="false"
                android:textColor="#FFFFFF"
                android:textSize="13sp" />

            <TextView
                android:id="@+id/tvLanguageLabel"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="10dp"
                android:text="Limba"
                android:textColor="#CBD5E1"
                android:textSize="13sp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnLangRo"
                    android:layout_width="0dp"
                    android:layout_height="36dp"
                    android:layout_marginEnd="4dp"
                    android:layout_weight="1"
                    android:backgroundTint="#334155"
                    android:text="RO"
                    android:textColor="#FFFFFF" />

                <Button
                    android:id="@+id/btnLangEn"
                    android:layout_width="0dp"
                    android:layout_height="36dp"
                    android:layout_marginHorizontal="2dp"
                    android:layout_weight="1"
                    android:backgroundTint="#334155"
                    android:text="EN"
                    android:textColor="#FFFFFF" />

                <Button
                    android:id="@+id/btnLangRu"
                    android:layout_width="0dp"
                    android:layout_height="36dp"
                    android:layout_marginStart="4dp"
                    android:layout_weight="1"
                    android:backgroundTint="#334155"
                    android:text="RU"
                    android:textColor="#FFFFFF" />
            </LinearLayout>

            <TextView
                android:id="@+id/tvAspectLabel"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="10dp"
                android:text="Format ecran"
                android:textColor="#CBD5E1"
                android:textSize="13sp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnAspectAuto"
                    android:layout_width="0dp"
                    android:layout_height="36dp"
                    android:layout_marginEnd="4dp"
                    android:layout_weight="1"
                    android:backgroundTint="#334155"
                    android:text="Auto"
                    android:textAllCaps="false"
                    android:textColor="#FFFFFF"
                    android:textSize="12sp" />

                <Button
                    android:id="@+id/btnAspect43"
                    android:layout_width="0dp"
                    android:layout_height="36dp"
                    android:layout_marginHorizontal="2dp"
                    android:layout_weight="1"
                    android:backgroundTint="#334155"
                    android:text="4:3"
                    android:textColor="#FFFFFF"
                    android:textSize="12sp" />

                <Button
                    android:id="@+id/btnAspectFull"
                    android:layout_width="0dp"
                    android:layout_height="36dp"
                    android:layout_marginStart="4dp"
                    android:layout_weight="1"
                    android:backgroundTint="#334155"
                    android:text="Full ecran"
                    android:textAllCaps="false"
                    android:textColor="#FFFFFF"
                    android:textSize="12sp" />
            </LinearLayout>

            <TextView
                android:id="@+id/tvContrastLabel"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="10dp"
                android:text="Contrast"
                android:textColor="#CBD5E1"
                android:textSize="12sp" />

            <SeekBar
                android:id="@+id/seekContrast"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:max="200"
                android:progress="100" />

            <TextView
                android:id="@+id/tvSaturationLabel"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:text="Saturație"
                android:textColor="#CBD5E1"
                android:textSize="12sp" />

            <SeekBar
                android:id="@+id/seekSaturation"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:max="200"
                android:progress="100" />

            <TextView
                android:id="@+id/tvWarmthLabel"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:text="Căldură culori"
                android:textColor="#CBD5E1"
                android:textSize="12sp" />

            <SeekBar
                android:id="@+id/seekWarmth"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:max="100"
                android:progress="50" />

            <Button
                android:id="@+id/btnResetColors"
                android:layout_width="match_parent"
                android:layout_height="36dp"
                android:layout_marginTop="8dp"
                android:marginBottom="16dp"
                android:backgroundTint="#334155"
                android:text="Reset culori"
                android:textAllCaps="false"
                android:textColor="#FFFFFF"
                android:textSize="12sp" />
        </LinearLayout>
    </ScrollView>

</FrameLayout>
