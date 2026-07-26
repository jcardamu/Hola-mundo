package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val database = AppDatabase.getDatabase(application)
    val trackRepository = TrackRepository(application, database.downloadedTrackDao())
    
    private val attributionContext = application

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(attributionContext)
        .setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val noisyAudioReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            val action = intent?.action
            if (action == android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY ||
                action == android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                }
            }
        }
    }
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState = _authState.asStateFlow()
    
    val downloadedTracks = trackRepository.allDownloadedTracks.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    private val _items = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _playlists = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val playlists = _playlists.asStateFlow()

    private val _albums = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val albums = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val artists = _artists.asStateFlow()

    private val _containerItems = MutableStateFlow<List<JellyfinItem>?>(null)
    val containerItems = _containerItems.asStateFlow()

    private val _containerStack = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val containerStack = _containerStack.asStateFlow()

    private val _selectedContainer = MutableStateFlow<JellyfinItem?>(null)
    val selectedContainer = _selectedContainer.asStateFlow()

    private val _isShuffleModeEnabled = MutableStateFlow(false)
    val isShuffleModeEnabled = _isShuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(androidx.media3.common.Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()

    fun toggleRepeatMode() {
        val nextMode = when (exoPlayer.repeatMode) {
            androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
            androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
            else -> androidx.media3.common.Player.REPEAT_MODE_OFF
        }
        exoPlayer.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    private val _playQueue = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val playQueue = _playQueue.asStateFlow()
    
    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex = _currentQueueIndex.asStateFlow()
    
    private val _currentTrack = MutableStateFlow<JellyfinItem?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition = _playbackPosition.asStateFlow()

    private val _trackDuration = MutableStateFlow(0L)
    val trackDuration = _trackDuration.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isLoadingTracks = MutableStateFlow(false)
    val isLoadingTracks = _isLoadingTracks.asStateFlow()
    
    private val _showFullScreenPlayer = MutableStateFlow(false)
    val showFullScreenPlayer = _showFullScreenPlayer.asStateFlow()

    private val playHistoryPrefs = application.getSharedPreferences("user_play_history", android.content.Context.MODE_PRIVATE)

    private val _playCounts = MutableStateFlow<Map<String, Int>>(loadPlayCounts())
    val playCounts = _playCounts.asStateFlow()

    private val _lastPlayedTimes = MutableStateFlow<Map<String, Long>>(loadLastPlayedTimes())

    private fun loadPlayCounts(): Map<String, Int> {
        val json = playHistoryPrefs.getString("counts", null) ?: return emptyMap()
        return try {
            val type = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)
            val adapter = com.squareup.moshi.Moshi.Builder().build().adapter<Map<String, Int>>(type)
            adapter.fromJson(json) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun loadLastPlayedTimes(): Map<String, Long> {
        val json = playHistoryPrefs.getString("last_played", null) ?: return emptyMap()
        return try {
            val type = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Long::class.javaObjectType)
            val adapter = com.squareup.moshi.Moshi.Builder().build().adapter<Map<String, Long>>(type)
            adapter.fromJson(json) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun savePlayHistory(counts: Map<String, Int>, times: Map<String, Long>) {
        try {
            val moshi = com.squareup.moshi.Moshi.Builder().build()
            val countsType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)
            val timesType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Long::class.javaObjectType)
            
            val countsJson = moshi.adapter<Map<String, Int>>(countsType).toJson(counts)
            val timesJson = moshi.adapter<Map<String, Long>>(timesType).toJson(times)

            playHistoryPrefs.edit()
                .putString("counts", countsJson)
                .putString("last_played", timesJson)
                .apply()
        } catch (_: Exception) {}
    }

    fun recordTrackPlayed(item: JellyfinItem) {
        val currentCounts = _playCounts.value.toMutableMap()
        val currentTimes = _lastPlayedTimes.value.toMutableMap()
        
        val newCount = (currentCounts[item.id] ?: 0) + 1
        currentCounts[item.id] = newCount
        currentTimes[item.id] = System.currentTimeMillis()

        _playCounts.value = currentCounts
        _lastPlayedTimes.value = currentTimes

        savePlayHistory(currentCounts, currentTimes)
    }

    fun getRecommendedForYouTracks(): List<JellyfinItem> {
        val allSongs = _items.value
        if (allSongs.isEmpty()) return emptyList()

        val countsMap = _playCounts.value
        val timesMap = _lastPlayedTimes.value

        val todayEpochDay = java.time.LocalDate.now().toEpochDay()
        val dailyRandom = java.util.Random(todayEpochDay)

        // 1. Calculate listening weights for artists and albums based on play counts & favorites
        val artistWeights = mutableMapOf<String, Int>()
        val albumWeights = mutableMapOf<String, Int>()

        allSongs.forEach { song ->
            val localCount = countsMap[song.id] ?: 0
            val jellyCount = song.userData?.playCount ?: 0
            val favBonus = if (song.userData?.isFavorite == true) 5 else 0
            val score = (localCount * 3) + jellyCount + favBonus

            if (score > 0) {
                song.artists?.forEach { artist ->
                    artistWeights[artist] = (artistWeights[artist] ?: 0) + score
                }
                if (!song.album.isNullOrEmpty()) {
                    albumWeights[song.album] = (albumWeights[song.album] ?: 0) + score
                }
            }
        }

        // 2. Separate recent plays vs older history vs unplayed
        val recentPlays = allSongs.filter { song ->
            (timesMap[song.id] ?: 0L) > 0L || (countsMap[song.id] ?: 0) > 0 || song.userData?.played == true
        }.sortedByDescending { timesMap[it.id] ?: 0L }

        val oldFavorites = allSongs.filter { song ->
            val totalCount = (countsMap[song.id] ?: 0) + (song.userData?.playCount ?: 0)
            val isFav = song.userData?.isFavorite == true
            (totalCount > 0 || isFav) && !recentPlays.take(10).contains(song)
        }

        // Pick 6-8 recent songs (shuffled with today's seed for daily variation)
        val selectedRecent = recentPlays.shuffled(dailyRandom).take(7)

        // Pick 6-8 older favorites / rediscovery songs (shuffled with today's seed)
        val selectedOldFavs = oldFavorites.shuffled(dailyRandom).take(7)

        // 3. Affinity recommendations from Jellyfin library (matching artist & album tastes)
        val pickedSoFar = (selectedRecent + selectedOldFavs).toSet()
        val discoveryCandidates = allSongs.filter { !pickedSoFar.contains(it) }.map { song ->
            var affinity = 0
            song.artists?.forEach { artist ->
                affinity += (artistWeights[artist] ?: 0) * 2
            }
            if (!song.album.isNullOrEmpty()) {
                affinity += (albumWeights[song.album] ?: 0)
            }
            song to affinity
        }

        val topDiscovery = discoveryCandidates
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .shuffled(dailyRandom)
            .take(12)

        val combinedPicks = (selectedRecent + selectedOldFavs + topDiscovery).distinct().toMutableList()

        // 4. Fill up to 25 songs if necessary with other Jellyfin songs
        if (combinedPicks.size < 25) {
            val remaining = allSongs.filter { !combinedPicks.contains(it) }.shuffled(dailyRandom)
            combinedPicks.addAll(remaining.take(25 - combinedPicks.size))
        }

        // 5. Shuffle final order deterministically per day so the playlist layout rotates daily
        return combinedPicks.shuffled(java.util.Random(todayEpochDay + 42))
    }

    fun generateAiPlaylist(prompt: String): Pair<JellyfinItem, List<JellyfinItem>>? {
        val allSongs = _items.value
        if (allSongs.isEmpty() || prompt.isBlank()) return null

        fun normalize(str: String): String {
            return str.lowercase()
                .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                .replace("ñ", "n")
        }

        val cleanPrompt = normalize(prompt)

        // Get all unique artist names from songs and artist list
        val libraryArtists = (allSongs.flatMap { it.artists ?: emptyList() } + _artists.value.map { it.name })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        // Match artists mentioned in prompt
        val matchedArtistNames = libraryArtists.filter { artist ->
            val cleanArtist = normalize(artist)
            cleanArtist.length >= 3 && cleanPrompt.contains(cleanArtist)
        }

        val matchedSongs = if (matchedArtistNames.isNotEmpty()) {
            allSongs.filter { song ->
                song.artists?.any { songArtist ->
                    matchedArtistNames.any { target -> normalize(songArtist).contains(normalize(target)) }
                } == true
            }
        } else {
            // Fallback: search for tokens in song titles, albums, genres or artists
            val tokens = cleanPrompt.split(" ", ",", "y", "con", "de", "para", "los", "las", "un", "una", "playlist")
                .map { normalize(it.trim()) }
                .filter { it.length > 2 }

            allSongs.filter { song ->
                tokens.any { token ->
                    normalize(song.name).contains(token) ||
                    song.artists?.any { normalize(it).contains(token) } == true ||
                    (song.album != null && normalize(song.album).contains(token))
                }
            }
        }

        if (matchedSongs.isEmpty()) return null

        val displayArtistName = if (matchedArtistNames.isNotEmpty()) {
            matchedArtistNames.joinToString(", ")
        } else {
            prompt.take(30)
        }

        val coverId = matchedSongs.firstOrNull()?.let { it.albumId ?: it.id }
        val container = JellyfinItem(
            id = "mock_ai_playlist_${System.currentTimeMillis()}",
            name = "Playlist IA: $displayArtistName",
            type = "Playlist",
            album = "Playlist temporal por IA",
            albumId = coverId
        )

        return container to matchedSongs.shuffled()
    }

    fun setShowFullScreenPlayer(show: Boolean) {
        _showFullScreenPlayer.value = show
    }
    
    init {
        val noisyFilter = android.content.IntentFilter().apply {
            addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        androidx.core.content.ContextCompat.registerReceiver(
            getApplication<Application>(),
            noisyAudioReceiver,
            noisyFilter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )

        val stopCommandButton = androidx.media3.session.CommandButton.Builder()
            .setDisplayName("Stop")
            .setIconResId(com.example.R.drawable.ic_stop)
            .setSessionCommand(androidx.media3.session.SessionCommand("ACTION_STOP", android.os.Bundle.EMPTY))
            .build()
            
        val sessionCallback = object : androidx.media3.session.MediaSession.Callback {
            override fun onConnect(
                session: androidx.media3.session.MediaSession,
                controller: androidx.media3.session.MediaSession.ControllerInfo
            ): androidx.media3.session.MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(androidx.media3.session.SessionCommand("ACTION_STOP", android.os.Bundle.EMPTY))
                    .build()
                return androidx.media3.session.MediaSession.ConnectionResult.accept(
                    availableSessionCommands,
                    connectionResult.availablePlayerCommands
                )
            }

            override fun onCustomCommand(
                session: androidx.media3.session.MediaSession,
                controller: androidx.media3.session.MediaSession.ControllerInfo,
                customCommand: androidx.media3.session.SessionCommand,
                args: android.os.Bundle
            ): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
                if (customCommand.customAction == "ACTION_STOP") {
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    _isPlaying.value = false
                    _currentTrack.value = null
                }
                return com.google.common.util.concurrent.Futures.immediateFuture(
                    androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS)
                )
            }
        }

        val intent = android.content.Intent(application, com.example.MainActivity::class.java).apply {
            action = android.content.Intent.ACTION_MAIN
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            putExtra("OPEN_PLAYER", true)
        }
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        } else {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            application,
            0,
            intent,
            flags
        )

        PlayerRepository.player = exoPlayer
        PlayerRepository.session = androidx.media3.session.MediaSession.Builder(attributionContext, exoPlayer)
            .setCallback(sessionCallback)
            .setSessionActivity(pendingIntent)
            .setCustomLayout(listOf(stopCommandButton))
            .build()
        
        // Bind to PlaybackService so it starts and handles the notification
        val sessionToken = androidx.media3.session.SessionToken(attributionContext, android.content.ComponentName(attributionContext, PlaybackService::class.java))
        androidx.media3.session.MediaController.Builder(attributionContext, sessionToken).buildAsync()
        
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    val index = exoPlayer.currentMediaItemIndex
                    _currentQueueIndex.value = index
                    val track = _playQueue.value.getOrNull(index)
                    _currentTrack.value = track
                    track?.let { recordTrackPlayed(it) }
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                }
            }
        })

        viewModelScope.launch {
            while (true) {
                if (_isPlaying.value) {
                    _playbackPosition.value = exoPlayer.currentPosition
                    _trackDuration.value = exoPlayer.duration.coerceAtLeast(0L)
                }
                kotlinx.coroutines.delay(1000L)
            }
        }
        
        viewModelScope.launch {
            val url = prefs.serverUrl.first()
            val token = prefs.accessToken.first()
            val id = prefs.userId.first()
            val hasLaunched = prefs.hasLaunched.first() ?: false
            
            if (!url.isNullOrEmpty() && !token.isNullOrEmpty() && !id.isNullOrEmpty()) {
                _authState.value = AuthState.LoggedIn(url, token, id)
                loadItems(url, token, id)
            } else {
                prefs.setHasLaunched()
                _authState.value = AuthState.LoggedOut
            }
        }
    }
    
    fun loginDemoMode() {
        if (_items.value.isEmpty()) {
            _items.value = getDemoSongs()
        }
        _authState.value = AuthState.LoggedIn("https://demo.jellyfin.local", "demo_token", "demo_user")
    }

    fun getDemoSongs(): List<JellyfinItem> {
        return listOf(
            JellyfinItem("demo_1", "Columbia", type = "Audio", artists = listOf("Quevedo"), album = "DONDE QUIERO ESTAR"),
            JellyfinItem("demo_2", "LALA", type = "Audio", artists = listOf("Myke Towers"), album = "LA VIDA ES UNA"),
            JellyfinItem("demo_3", "Despecha", type = "Audio", artists = listOf("Rosalía"), album = "MOTOMAMI"),
            JellyfinItem("demo_4", "Mon Amour (Remix)", type = "Audio", artists = listOf("zoilo", "Aitana"), album = "Mon Amour"),
            JellyfinItem("demo_5", "Playa Del Inglés", type = "Audio", artists = listOf("Quevedo", "Myke Towers"), album = "DONDE QUIERO ESTAR"),
            JellyfinItem("demo_6", "Night Changes", type = "Audio", artists = listOf("One Direction"), album = "FOUR"),
            JellyfinItem("demo_7", "Starboy", type = "Audio", artists = listOf("The Weeknd", "Daft Punk"), album = "Starboy"),
            JellyfinItem("demo_8", "As It Was", type = "Audio", artists = listOf("Harry Styles"), album = "Harry's House"),
            JellyfinItem("demo_9", "Flowers", type = "Audio", artists = listOf("Miley Cyrus"), album = "Endless Summer Vacation"),
            JellyfinItem("demo_10", "Blinding Lights", type = "Audio", artists = listOf("The Weeknd"), album = "After Hours"),
            JellyfinItem("demo_11", "Pepas", type = "Audio", artists = listOf("Farruko"), album = "La 167"),
            JellyfinItem("demo_12", "Tacones Rojos", type = "Audio", artists = listOf("Sebastián Yatra"), album = "Dharma"),
            JellyfinItem("demo_13", "Shape of You", type = "Audio", artists = listOf("Ed Sheeran"), album = "÷ (Divide)"),
            JellyfinItem("demo_14", "DÁKITI", type = "Audio", artists = listOf("Bad Bunny", "Jhayco"), album = "EL ÚLTIMO TOUR DEL MUNDO"),
            JellyfinItem("demo_15", "Bzrp Music Sessions, Vol. 52", type = "Audio", artists = listOf("Bizarrap", "Quevedo"), album = "Vol. 52")
        )
    }

    fun login(server: String, user: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Checking
            val urlsToTry = mutableListOf(server)
            
            // If the user provided a simple host, add common Jellyfin ports as fallbacks
            if (server.contains("cartrianajellyfin.duckdns.org")) {
                urlsToTry.add("http://cartrianajellyfin.duckdns.org:8096")
                urlsToTry.add("https://cartrianajellyfin.duckdns.org:8920")
                urlsToTry.add("http://cartrianajellyfin.duckdns.org")
            }

            var lastError: Exception? = null
            var successUrl: String? = null
            var response: AuthResponse? = null

            for (testUrl in urlsToTry) {
                try {
                    val api = ApiClient.create(testUrl)
                    val deviceId = UUID.randomUUID().toString()
                    val authString = "MediaBrowser Client=\"JellyfinMusic\", Device=\"Android\", DeviceId=\"$deviceId\", Version=\"1.0.0\""
                    
                    response = api.authenticate(authString, AuthRequest(user, pass))
                    successUrl = testUrl
                    break // Success!
                } catch (e: Exception) {
                    lastError = e
                    Log.e("AppViewModel", "Login failed for $testUrl", e)
                }
            }

            if (successUrl != null && response != null) {
                val token = response.accessToken
                val userId = response.user.id
                
                prefs.saveAuthData(successUrl, token, userId)
                _authState.value = AuthState.LoggedIn(successUrl, token, userId)
                loadItems(successUrl, token, userId)
            } else {
                val e = lastError
                val msg = if (e is retrofit2.HttpException) {
                    "HTTP Error: ${e.code()} - ${e.message}"
                } else if (e is java.net.SocketTimeoutException || e is java.net.ConnectException) {
                    "Connection failed. Make sure your server is online and ports are forwarded correctly (e.g. 8096)."
                } else {
                    e?.message ?: e.toString()
                }
                _authState.value = AuthState.Error(msg)
            }
        }
    }
    
    private fun loadItems(serverUrl: String, token: String, userId: String) {
        viewModelScope.launch {
            _isLoadingTracks.value = true
            var hasError = false
            try {
                val api = ApiClient.create(serverUrl)
                
                val tracksJob = launch {
                    try {
                        val response = api.getItems(userId = userId, token = token, includeTypes = "Audio")
                        _items.value = response.items
                    } catch (e: Exception) { 
                        Log.e("AppViewModel", "Fetch tracks error", e)
                        hasError = true
                    }
                }

                val albumsJob = launch {
                    try {
                        val response = api.getItems(userId = userId, token = token, includeTypes = "MusicAlbum")
                        _albums.value = response.items
                    } catch (e: Exception) { 
                        Log.e("AppViewModel", "Fetch albums error", e)
                        hasError = true
                    }
                }

                val playlistsJob = launch {
                    try {
                        val response = api.getItems(userId = userId, token = token, includeTypes = "Playlist")
                        _playlists.value = response.items.filter { it.name != "." }
                    } catch (e: Exception) { 
                        Log.e("AppViewModel", "Fetch playlists error", e)
                        hasError = true
                    }
                }

                val artistsJob = launch {
                    try {
                        val response = api.getArtists(userId = userId, token = token)
                        _artists.value = response.items
                    } catch (e: Exception) { 
                        Log.e("AppViewModel", "Fetch artists error", e)
                        hasError = true
                    }
                }

                tracksJob.join()
                albumsJob.join()
                playlistsJob.join()
                artistsJob.join()
                
                if (hasError) {
                    logout()
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Fetch error", e)
                logout()
            } finally {
                _isLoadingTracks.value = false
            }
        }
    }
    
    private val _qrSessionCode = MutableStateFlow("FIESTA-" + (1000..9999).random())
    val qrSessionCode = _qrSessionCode.asStateFlow()

    private val _qrRequests = MutableStateFlow<List<com.example.data.QrSongRequest>>(emptyList())
    val qrRequests = _qrRequests.asStateFlow()

    private val _activeQrPlaylistContainer = MutableStateFlow<JellyfinItem?>(null)
    val activeQrPlaylistContainer = _activeQrPlaylistContainer.asStateFlow()

    private val _qrPlaylistTracks = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val qrPlaylistTracks = _qrPlaylistTracks.asStateFlow()

    private var partyServer: com.example.server.PartyWebServer? = null

    fun startPartyServer(): String {
        if (partyServer == null) {
            try {
                partyServer = com.example.server.PartyWebServer(
                    port = 8080,
                    onRequestReceived = { guestName, songTitle ->
                        submitGuestRequest(guestName, songTitle)
                    },
                    getAvailableSongs = { if (_items.value.isNotEmpty()) _items.value else getDemoSongs() }
                )
                partyServer?.start(fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val ip = com.example.server.PartyWebServer.getLocalIpAddress()
        return "http://$ip:8080"
    }

    fun getOrCreateQrPlaylist(): JellyfinItem {
        val current = _activeQrPlaylistContainer.value
        if (current != null) return current
        
        val newContainer = JellyfinItem(
            id = "mock_qr_playlist_${System.currentTimeMillis()}",
            name = "Playlist QR Fiesta (${_qrSessionCode.value})",
            type = "Playlist",
            album = "Playlist temporal por QR de invitados",
            albumId = _items.value.firstOrNull()?.albumId
        )
        _activeQrPlaylistContainer.value = newContainer
        return newContainer
    }

    fun submitGuestRequest(guestName: String, songQuery: String): com.example.data.QrSongRequest {
        val allSongs = if (_items.value.isNotEmpty()) _items.value else getDemoSongs()
        fun normalize(str: String) = str.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u").replace("ñ", "n")

        val cleanQuery = normalize(songQuery.trim())
        val matched = allSongs.firstOrNull { song ->
            normalize(song.name).contains(cleanQuery) ||
            song.artists?.any { normalize(it).contains(cleanQuery) } == true ||
            (song.album != null && normalize(song.album).contains(cleanQuery))
        } ?: allSongs.firstOrNull()

        val request = com.example.data.QrSongRequest(
            guestName = if (guestName.isBlank()) "Invitado" else guestName.trim(),
            songTitle = songQuery.ifBlank { matched?.name ?: "Canción solicitada" },
            matchedItem = matched
        )

        _qrRequests.value = listOf(request) + _qrRequests.value

        if (matched != null) {
            addTrackToQrPlaylist(matched)
        }

        return request
    }

    fun addTrackToQrPlaylist(item: JellyfinItem) {
        val container = getOrCreateQrPlaylist()
        val currentList = _qrPlaylistTracks.value.toMutableList()
        if (!currentList.contains(item)) {
            currentList.add(item)
            _qrPlaylistTracks.value = currentList
            mockContainerItems[container.id] = currentList
            
            if (_selectedContainer.value?.id == container.id) {
                _containerItems.value = currentList
            }
        }
    }

    fun openQrPlaylist() {
        val container = getOrCreateQrPlaylist()
        val tracks = _qrPlaylistTracks.value
        pushMockContainer(container, tracks)
    }

    fun resetQrSession() {
        _qrSessionCode.value = "FIESTA-" + (1000..9999).random()
        _qrRequests.value = emptyList()
        _activeQrPlaylistContainer.value = null
        _qrPlaylistTracks.value = emptyList()
    }

    fun pushContainer(container: JellyfinItem) {
        val stack = _containerStack.value.toMutableList()
        stack.add(container)
        _containerStack.value = stack
        _selectedContainer.value = container
        loadContainerContents(container)
    }

    fun popContainer() {
        val stack = _containerStack.value.toMutableList()
        if (stack.isNotEmpty()) {
            val popped = stack.removeAt(stack.size - 1)
            // Ephemeral playlists (AI playlist or QR Party playlist) disappear when leaving
            if (popped.id.startsWith("mock_ai_playlist_") || popped.id.startsWith("mock_qr_playlist_")) {
                mockContainerItems.remove(popped.id)
                if (_activeQrPlaylistContainer.value?.id == popped.id) {
                    _activeQrPlaylistContainer.value = null
                    _qrPlaylistTracks.value = emptyList()
                }
            }
        }
        _containerStack.value = stack
        
        if (stack.isEmpty()) {
            _selectedContainer.value = null
            _containerItems.value = null
        } else {
            val prev = stack.last()
            _selectedContainer.value = prev
            loadContainerContents(prev)
        }
    }

    fun clearContainers() {
        _containerStack.value = emptyList()
        _selectedContainer.value = null
        _containerItems.value = null
    }

    private val mockContainerItems = mutableMapOf<String, List<JellyfinItem>>()

    fun pushMockContainer(container: JellyfinItem, items: List<JellyfinItem>) {
        mockContainerItems[container.id] = items
        val stack = _containerStack.value.toMutableList()
        stack.add(container)
        _containerStack.value = stack
        _selectedContainer.value = container
        _containerItems.value = items
    }

    private fun loadContainerContents(container: JellyfinItem) {
        if (mockContainerItems.containsKey(container.id)) {
            _containerItems.value = mockContainerItems[container.id]
            return
        }
        _containerItems.value = null
        val state = _authState.value as? AuthState.LoggedIn ?: return
        viewModelScope.launch {
            try {
                val api = ApiClient.create(state.serverUrl)
                
                val response = if (container.type == "Playlist") {
                    api.getPlaylistItems(
                        playlistId = container.id,
                        userId = state.userId,
                        token = state.token
                    )
                } else {
                    val isArtist = container.type == "MusicArtist"
                    val includeTypes = if (isArtist) "MusicAlbum" else "Audio"
                    val parentId = if (isArtist) null else container.id
                    val artistIds = if (isArtist) container.id else null

                    api.getItems(
                        userId = state.userId,
                        token = state.token,
                        parentId = parentId,
                        artistIds = artistIds,
                        includeTypes = includeTypes,
                        sortBy = "SortName"
                    )
                }
                _containerItems.value = response.items
            } catch (e: Exception) {
                Log.e("AppViewModel", "Fetch container items error", e)
            }
        }
    }

    fun toggleFavorite(item: JellyfinItem) {
        val state = _authState.value as? AuthState.LoggedIn ?: return
        viewModelScope.launch {
            try {
                val api = ApiClient.create(state.serverUrl)
                val isCurrentlyFavorite = item.userData?.isFavorite == true
                if (isCurrentlyFavorite) {
                    api.unmarkFavorite(state.userId, item.id, state.token)
                } else {
                    api.markFavorite(state.userId, item.id, state.token)
                }

                // Update local memory
                val updatedItems = _items.value.map { if (it.id == item.id) it.copy(userData = UserData(!isCurrentlyFavorite)) else it }
                _items.value = updatedItems

                val updatedContainerItems = _containerItems.value?.map { if (it.id == item.id) it.copy(userData = UserData(!isCurrentlyFavorite)) else it }
                if (updatedContainerItems != null) _containerItems.value = updatedContainerItems
                
                val updatedAlbums = _albums.value.map { if (it.id == item.id) it.copy(userData = UserData(!isCurrentlyFavorite)) else it }
                _albums.value = updatedAlbums
                
                val updatedPlaylists = _playlists.value.map { if (it.id == item.id) it.copy(userData = UserData(!isCurrentlyFavorite)) else it }
                _playlists.value = updatedPlaylists
                
                val updatedArtists = _artists.value.map { if (it.id == item.id) it.copy(userData = UserData(!isCurrentlyFavorite)) else it }
                _artists.value = updatedArtists

            } catch (e: Exception) {
                Log.e("AppViewModel", "Favorite edit error", e)
            }
        }
    }

    fun toggleShuffleMode() {
        val newMode = !_isShuffleModeEnabled.value
        _isShuffleModeEnabled.value = newMode
        if (newMode && _playQueue.value.isNotEmpty()) {
            val currentList = _playQueue.value
            val currentIndex = _currentQueueIndex.value
            val safeIndex = currentIndex.coerceIn(-1, currentList.size - 1)
            val remaining = currentList.subList(safeIndex + 1, currentList.size).shuffled()
            val before = if (safeIndex >= 0) currentList.subList(0, safeIndex + 1) else emptyList()
            _playQueue.value = before + remaining
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playbackPosition.value = positionMs
    }

    fun playTrackList(tracks: List<JellyfinItem>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val state = _authState.value as? AuthState.LoggedIn ?: return

        val queue = if (_isShuffleModeEnabled.value) {
            val itemToPlay = tracks[startIndex]
            val others = tracks.toMutableList()
            others.removeAt(startIndex)
            others.shuffle()
            listOf(itemToPlay) + others
        } else {
            tracks.toList()
        }
        _playQueue.value = queue
        
        val items = queue.map { createMediaItemFor(it, state) }
        val finalStartIndex = if (_isShuffleModeEnabled.value) 0 else startIndex
        
        _currentQueueIndex.value = finalStartIndex
        _currentTrack.value = queue[finalStartIndex]
        
        exoPlayer.setMediaItems(items, finalStartIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    private fun createMediaItemFor(item: JellyfinItem, state: AuthState.LoggedIn): androidx.media3.common.MediaItem {
        val downloaded = downloadedTracks.value.find { it.id == item.id }
        val title = item.name
        val artist = item.artists?.joinToString(", ") ?: item.album ?: "Unknown Artist"
        val artUri = android.net.Uri.parse("${state.serverUrl.removeSuffix("/")}/Items/${item.albumId ?: item.id}/Images/Primary?maxHeight=500&maxWidth=500")
        
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(artUri)
            .build()
            
        return if (downloaded != null && downloaded.localFilePath != null) {
            androidx.media3.common.MediaItem.Builder()
                .setMediaId(item.id)
                .setUri("file://${downloaded.localFilePath}")
                .setMediaMetadata(metadata)
                .build()
        } else {
            val url = state.serverUrl.removeSuffix("/")
            val streamUrl = "$url/Audio/${item.id}/stream?static=true&api_key=${state.token}"
            androidx.media3.common.MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(streamUrl)
                .setMediaMetadata(metadata)
                .build()
        }
    }

    fun playNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        } else {
            exoPlayer.stop()
            _isPlaying.value = false
        }
    }

    fun playPrevious() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        } else {
            exoPlayer.seekTo(0)
        }
    }

    fun downloadItem(item: JellyfinItem) {
        val state = _authState.value as? AuthState.LoggedIn ?: return
        viewModelScope.launch {
            trackRepository.downloadTrack(item, state.serverUrl, state.token)
        }
    }

    fun deleteDownloadedItem(item: JellyfinItem) {
        viewModelScope.launch {
            trackRepository.deleteTrack(item)
        }
    }

    fun playTrack(item: JellyfinItem, isDownloaded: Boolean = false, localPath: String? = null) {
        // Internal testing or single track play backward compatibility
        playTrackList(listOf(item), 0)
    }
    
    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun clearQueue() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _playQueue.value = emptyList()
        _currentTrack.value = null
        _isPlaying.value = false
        _currentQueueIndex.value = -1
    }
    
    fun logout() {
        viewModelScope.launch {
            prefs.logout()
            _authState.value = AuthState.LoggedOut
            _items.value = emptyList()
            exoPlayer.stop()
            _currentTrack.value = null
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        partyServer?.stop()
        partyServer = null
        try {
            getApplication<Application>().unregisterReceiver(noisyAudioReceiver)
        } catch (_: Exception) {}
        PlayerRepository.session?.release()
        PlayerRepository.session = null
        exoPlayer.release()
        PlayerRepository.player = null
    }

    fun refreshPlaylists() {
        val state = _authState.value as? AuthState.LoggedIn ?: return
        viewModelScope.launch {
            try {
                val api = ApiClient.create(state.serverUrl)
                val response = api.getItems(userId = state.userId, token = state.token, includeTypes = "Playlist")
                _playlists.value = response.items.filter { it.name != "." }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Fetch playlists error", e)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication<Application>(), "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun createPlaylist(name: String) {
        val state = _authState.value as? AuthState.LoggedIn ?: return
        viewModelScope.launch {
            try {
                val api = ApiClient.create(state.serverUrl)
                val request = com.example.data.CreatePlaylistRequest(name = name, userId = state.userId)
                val response = api.createPlaylist(request = request, token = state.token)
                if (response.isSuccessful) {
                    refreshPlaylists()
                } else {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(getApplication<Application>(), "Error: ${response.code()} ${response.message()}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Create playlist error", e)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication<Application>(), "Error creating playlist", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val state = _authState.value as? AuthState.LoggedIn ?: return
        viewModelScope.launch {
            try {
                val api = ApiClient.create(state.serverUrl)
                // Needs full JellyfinItem payload. First get item, modify name, post item back
                val request = com.example.data.UpdatePlaylistRequest(name = newName)
                val response = api.updatePlaylist(playlistId, request, state.token)
                if (response.isSuccessful) {
                    refreshPlaylists()
                } else {
                    val errorBody = response.errorBody()?.string()
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(getApplication<Application>(), "Error renaming: ${response.code()} $errorBody", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Rename playlist error", e)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication<Application>(), "Error renaming playlist", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        val state = _authState.value as? AuthState.LoggedIn ?: return
        viewModelScope.launch {
            try {
                val api = ApiClient.create(state.serverUrl)
                
                // 1. Fetch playlist items first and remove them
                try {
                    val itemsResponse = api.getPlaylistItems(playlistId, state.userId, state.token)
                    val playlistItemIds = itemsResponse.items.mapNotNull { it.playlistItemId }
                    if (playlistItemIds.isNotEmpty()) {
                        api.removeFromPlaylist(playlistId, playlistItemIds.joinToString(","), state.token)
                    }
                } catch (e: Exception) {
                    Log.w("AppViewModel", "Failed to clear playlist items before deleting playlist", e)
                }

                // 2. Delete the playlist itself
                val response = api.deleteItem(playlistId, state.token)
                if (response.isSuccessful) {
                    val currentContainer = _selectedContainer.value
                    if (currentContainer?.id == playlistId) {
                        clearContainers()
                    }
                    refreshPlaylists()
                } else {
                    val errorBody = response.errorBody()?.string()
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(getApplication<Application>(), "Error deleting: ${response.code()} $errorBody", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Delete playlist error", e)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication<Application>(), "Error deleting playlist", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun removeFromPlaylist(playlistId: String, entryId: String) {
        val state = _authState.value as? AuthState.LoggedIn ?: return
        viewModelScope.launch {
            try {
                val api = ApiClient.create(state.serverUrl)
                val response = api.removeFromPlaylist(playlistId, entryId, state.token)
                if (response.isSuccessful) {
                    val currentContainer = _selectedContainer.value
                    if (currentContainer?.id == playlistId) {
                        loadContainerContents(currentContainer)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(getApplication<Application>(), "Error removing: ${response.code()} $errorBody", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Remove from playlist error", e)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication<Application>(), "Error removing from playlist", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun addToPlaylist(track: JellyfinItem, playlist: JellyfinItem) {
        val state = _authState.value as? AuthState.LoggedIn ?: return
        viewModelScope.launch {
            try {
                val api = ApiClient.create(state.serverUrl)
                api.addToPlaylist(
                    playlistId = playlist.id,
                    ids = track.id,
                    userId = state.userId,
                    token = state.token
                )
            } catch (e: Exception) {
                Log.e("AppViewModel", "Add to playlist error", e)
            }
        }
    }
}

sealed class AuthState {
    object Checking : AuthState()
    object LoggedOut : AuthState()
    data class LoggedIn(val serverUrl: String, val token: String, val userId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
