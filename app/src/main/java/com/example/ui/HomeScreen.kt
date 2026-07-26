package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.DownloadedTrack
import com.example.data.JellyfinItem
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyDarkGray
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyLightGray
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.*
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.Image
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serverUrl: String,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val playlistsRaw by viewModel.playlists.collectAsState()
    val playlists = playlistsRaw.filter { it.name.trim() != "." }
    val artists by viewModel.artists.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState(initial = emptyList())
    
    val isLoading by viewModel.isLoadingTracks.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val selectedContainer by viewModel.selectedContainer.collectAsState()
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val showFullScreenPlayer by viewModel.showFullScreenPlayer.collectAsState()
    var trackToAdd by remember { mutableStateOf<JellyfinItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    var playlistToEdit by remember { mutableStateOf<JellyfinItem?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showGuestSearchDialog by remember { mutableStateOf(false) }
    var itemToRemoveFromPlaylist by remember { mutableStateOf<JellyfinItem?>(null) }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF284C38), SpotifyBlack),
        startY = 0f,
        endY = 1000f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        if (selectedContainer != null) {
            ContainerScreen(
                container = selectedContainer!!,
                serverUrl = serverUrl,
                viewModel = viewModel,
                onBack = { viewModel.popContainer() },
                onExpandPlayer = { viewModel.setShowFullScreenPlayer(true) },
                onAddToPlaylist = { trackToAdd = it },
                onItemLongClick = { item -> 
                    if (selectedContainer!!.type == "Playlist") {
                        itemToRemoveFromPlaylist = item
                    }
                }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 24.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearchActive) {
                        androidx.compose.material3.TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search...", color = SpotifyLightGray) },
                            singleLine = true,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = SpotifyDarkGray,
                                unfocusedContainerColor = SpotifyDarkGray,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = CircleShape
                        )
                        IconButton(onClick = { 
                            isSearchActive = false
                            searchQuery = "" 
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search", tint = Color.White)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = "Logo", tint = SpotifyGreen, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "JellyMusic",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (selectedTabIndex != 0) {
                                IconButton(
                                    onClick = { isSearchActive = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            QrPartyButton(
                                onClick = { showQrDialog = true }
                            )
                            AiGenerateButton(
                                onClick = { showAiDialog = true }
                            )
                            IconButton(
                                onClick = { viewModel.logout() },
                                modifier = Modifier.size(32.dp).testTag("logout_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings / Logout", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = SpotifyGreen,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            val tabPosition = tabPositions[selectedTabIndex]
                            Box(
                                Modifier
                                    .wrapContentSize(Alignment.BottomStart)
                                    .offset(x = tabPosition.left)
                                    .width(tabPosition.width)
                                    .height(3.dp)
                                    .background(SpotifyGreen)
                            )
                        }
                    }
                ) {
                    val tabs = listOf("Home", "Songs", "Albums", "Playlists", "Artists", "Offline")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, color = if (selectedTabIndex == index) SpotifyGreen else SpotifyLightGray) }
                        )
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SpotifyGreen)
                    }
                } else {
                    val listModifier = Modifier.weight(1f)
                    val bottomPadding = if (currentTrack != null) 90.dp else 16.dp
                    
                    when (selectedTabIndex) {
                        0 -> {
                            HomeDashboard(
                                albums = albums,
                                playlists = playlists,
                                items = items,
                                recentItems = items.take(10),
                                serverUrl = serverUrl,
                                viewModel = viewModel,
                                modifier = listModifier,
                                bottomPadding = bottomPadding,
                                onNavigateToTab = { selectedTabIndex = it }
                            )
                        }
                        1 -> {
                            val favFiltered = if (showOnlyFavorites) {
                                items.filter { it.userData?.isFavorite == true }
                            } else {
                                items
                            }
                            val filtered = if (searchQuery.isBlank()) favFiltered else favFiltered.filter { it.name.contains(searchQuery, ignoreCase = true) }
                            SongsList(
                                items = filtered,
                                downloadedTracks = downloadedTracks,
                                serverUrl = serverUrl,
                                viewModel = viewModel,
                                modifier = listModifier,
                                bottomPadding = bottomPadding,
                                showOnlyFavorites = showOnlyFavorites,
                                onToggleShowOnlyFavorites = { showOnlyFavorites = !showOnlyFavorites },
                                onExpandPlayer = { viewModel.setShowFullScreenPlayer(true) },
                                onAddToPlaylist = { trackToAdd = it }
                            )
                        }
                        2 -> {
                            val filtered = if (searchQuery.isBlank()) albums else albums.filter { it.name.contains(searchQuery, ignoreCase = true) }
                            GridContainersList(filtered, serverUrl, viewModel, listModifier, bottomPadding)
                        }
                        3 -> {
                            val filtered = if (searchQuery.isBlank()) playlists else playlists.filter { it.name.contains(searchQuery, ignoreCase = true) }
                            Column(modifier = listModifier) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { showCreatePlaylistDialog = true },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(64.dp).background(SpotifyDarkGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, "Create Playlist", tint = Color.White, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Create Playlist", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                }
                                GridContainersList(filtered, serverUrl, viewModel, Modifier.weight(1f), bottomPadding, onItemLongClick = { playlistToEdit = it })
                            }
                        }
                        4 -> {
                            val filtered = if (searchQuery.isBlank()) artists else artists.filter { it.name.contains(searchQuery, ignoreCase = true) }
                            GridContainersList(filtered, serverUrl, viewModel, listModifier, bottomPadding)
                        }
                        5 -> {
                            val offlineItems = downloadedTracks.map {
                                JellyfinItem(it.id, it.name, "Audio", it.album, it.albumId, it.artistsName?.split(", "), it.runTimeTicks)
                            }
                            val filtered = if (searchQuery.isBlank()) offlineItems else offlineItems.filter { it.name.contains(searchQuery, ignoreCase = true) }
                            SongsList(filtered, downloadedTracks, serverUrl, viewModel, listModifier, bottomPadding, isOfflineMode = true, onExpandPlayer = { viewModel.setShowFullScreenPlayer(true) }, onAddToPlaylist = { trackToAdd = it })
                        }
                    }
                }
            }
        }

        if (currentTrack != null) {
            if (showFullScreenPlayer) {
                FullScreenPlayer(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    viewModel = viewModel,
                    serverUrl = serverUrl,
                    onBack = { viewModel.setShowFullScreenPlayer(false) },
                    onAddToPlaylist = { trackToAdd = currentTrack }
                )
            } else {
                NowPlayingBar(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    viewModel = viewModel,
                    serverUrl = serverUrl,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                    onExpand = { viewModel.setShowFullScreenPlayer(true) }
                )
            }
        }

        if (trackToAdd != null) {
            PlaylistSelectionDialog(
                viewModel = viewModel,
                track = trackToAdd!!,
                onDismiss = { trackToAdd = null }
            )
        }

        if (showCreatePlaylistDialog) {
            var newPlaylistName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = { Text("Create Playlist", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name", color = SpotifyLightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SpotifyGreen,
                            unfocusedBorderColor = SpotifyLightGray
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                        }
                        showCreatePlaylistDialog = false
                    }) {
                        Text("Create", color = SpotifyGreen)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text("Cancel", color = SpotifyLightGray)
                    }
                },
                containerColor = SpotifyDarkGray
            )
        }

        if (playlistToEdit != null) {
            var playlistName by remember { mutableStateOf(playlistToEdit!!.name) }
            AlertDialog(
                onDismissRequest = { playlistToEdit = null },
                title = { Text("Edit Playlist", color = Color.White) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = playlistName,
                            onValueChange = { playlistName = it },
                            label = { Text("Playlist Name", color = SpotifyLightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SpotifyGreen,
                                unfocusedBorderColor = SpotifyLightGray
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (playlistName.isNotBlank() && playlistName != playlistToEdit!!.name) {
                            viewModel.renamePlaylist(playlistToEdit!!.id, playlistName)
                        }
                        playlistToEdit = null
                    }) {
                        Text("Save", color = SpotifyGreen)
                    }
                },
                dismissButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            viewModel.deletePlaylist(playlistToEdit!!.id)
                            playlistToEdit = null
                        }) {
                            Text("Delete", color = Color.Red)
                        }
                        TextButton(onClick = { playlistToEdit = null }) {
                            Text("Cancel", color = SpotifyLightGray)
                        }
                    }
                },
                containerColor = SpotifyDarkGray,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (itemToRemoveFromPlaylist != null) {
            AlertDialog(
                onDismissRequest = { itemToRemoveFromPlaylist = null },
                title = { Text("Remove from Playlist", color = Color.White) },
                text = { Text("Do you want to remove '${itemToRemoveFromPlaylist!!.name}' from this playlist?", color = SpotifyLightGray) },
                confirmButton = {
                    TextButton(onClick = {
                        val playlistId = selectedContainer?.id
                        val entryId = itemToRemoveFromPlaylist!!.playlistItemId ?: itemToRemoveFromPlaylist!!.id
                        if (playlistId != null) {
                            viewModel.removeFromPlaylist(playlistId, entryId)
                        }
                        itemToRemoveFromPlaylist = null
                    }) {
                        Text("Remove", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToRemoveFromPlaylist = null }) {
                        Text("Cancel", color = SpotifyLightGray)
                    }
                },
                containerColor = SpotifyDarkGray
            )
        }

        if (showAiDialog) {
            val context = LocalContext.current
            AiPlaylistDialog(
                onDismiss = { showAiDialog = false },
                onGenerate = { prompt ->
                    val result = viewModel.generateAiPlaylist(prompt)
                    if (result != null) {
                        viewModel.pushMockContainer(result.first, result.second)
                        showAiDialog = false
                    } else {
                        Toast.makeText(
                            context,
                            "No se encontraron canciones en tu biblioteca para esa petición. Prueba con otros artistas.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }

        if (showQrDialog) {
            QrPartyDialog(
                viewModel = viewModel,
                onDismiss = { showQrDialog = false },
                onOpenGuestSearch = { showGuestSearchDialog = true }
            )
        }

        if (showGuestSearchDialog) {
            QrGuestSearchDialog(
                viewModel = viewModel,
                onDismiss = { showGuestSearchDialog = false }
            )
        }
    }
}

@Composable
fun HomeDashboard(
    albums: List<JellyfinItem>,
    playlists: List<JellyfinItem>,
    items: List<JellyfinItem>,
    recentItems: List<JellyfinItem>,
    serverUrl: String,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onNavigateToTab: (Int) -> Unit
) {
    val today = java.time.LocalDate.now().toEpochDay()
    val playCounts by viewModel.playCounts.collectAsState()
    val dailyMixes = remember(items, today, playCounts) {
        if (items.isEmpty()) return@remember emptyList<Pair<JellyfinItem, List<JellyfinItem>>>()

        // 1. Recomendados para ti (Always first)
        val recTracks = viewModel.getRecommendedForYouTracks()
        val recCoverId = recTracks.firstOrNull()?.let { it.albumId ?: it.id }
        val recContainer = JellyfinItem(
            id = "mock_recommended_for_you",
            name = "Recomendados para ti",
            type = "Playlist",
            album = "Basado en tus gustos",
            albumId = recCoverId
        )
        val recPair = recContainer to recTracks

        // 2. Mixes 1 to 5
        val random = java.util.Random(today)
        val mixes = (1..5).map { i ->
            val mixSongs = items.shuffled(random).take(20)
            val coverId = mixSongs.firstOrNull()?.let { it.albumId ?: it.id }
            val container = JellyfinItem(
                id = "mock_daily_mix_$i",
                name = "Mix Diario $i",
                type = "Playlist",
                album = "Generado para ti",
                albumId = coverId
            )
            container to mixSongs
        }

        listOf(recPair) + mixes
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding, start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        item {
            val quickPicks = (playlists.take(3) + albums.take(3)).take(6)
            if (quickPicks.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    for (i in quickPicks.indices step 2) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickPickItem(
                                item = quickPicks[i],
                                serverUrl = serverUrl,
                                onClick = { viewModel.pushContainer(quickPicks[i]) },
                                modifier = Modifier.weight(1f)
                            )
                            if (i + 1 < quickPicks.size) {
                                QuickPickItem(
                                    item = quickPicks[i + 1],
                                    serverUrl = serverUrl,
                                    onClick = { viewModel.pushContainer(quickPicks[i + 1]) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (dailyMixes.isNotEmpty()) {
            item {
                Text("Recomendados de hoy", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    itemsIndexed(dailyMixes) { index, (mixItem, mixSongs) ->
                        MixListItem(
                            item = mixItem,
                            mixIndex = index,
                            onClick = { viewModel.pushMockContainer(mixItem, mixSongs) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (albums.isNotEmpty()) {
            item {
                Text("Nuevos lanzamientos", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val recentAlbums = albums.sortedByDescending { it.premiereDate ?: it.dateCreated ?: "" }.take(10)
                    items(recentAlbums) { album ->
                        HorizontalListItem(
                            item = album,
                            serverUrl = serverUrl,
                            onClick = { viewModel.pushContainer(album) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (playlists.isNotEmpty()) {
            item {
                Text("Tus Playlists", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(playlists) { playlist ->
                        HorizontalListItem(
                            item = playlist,
                            serverUrl = serverUrl,
                            onClick = { viewModel.pushContainer(playlist) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun QuickPickItem(item: JellyfinItem, serverUrl: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = SpotifyDarkGray)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.id.startsWith("mock_ai_playlist_") || item.id.startsWith("mock_qr_playlist_")) {
                AiPlaylistCoverArt(modifier = Modifier.size(56.dp))
            } else {
                val targetId = item.albumId ?: item.id
                val imageUrl = "${serverUrl.removeSuffix("/")}/Items/${targetId}/Images/Primary?maxHeight=100&maxWidth=100"
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
fun HorizontalListItem(item: JellyfinItem, serverUrl: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        if (item.id.startsWith("mock_ai_playlist_") || item.id.startsWith("mock_qr_playlist_")) {
            AiPlaylistCoverArt(modifier = Modifier.size(140.dp))
        } else {
            val targetId = item.albumId ?: item.id
            val imageUrl = "${serverUrl.removeSuffix("/")}/Items/${targetId}/Images/Primary?maxHeight=300&maxWidth=300"
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val artistStr = item.artists?.joinToString(", ") ?: item.album ?: ""
        if (artistStr.isNotEmpty()) {
            Text(
                text = artistStr,
                color = SpotifyLightGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MixCoverArt(mixIndex: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (mixIndex) {
                0 -> {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFE91E63), Color(0xFF8E24AA), Color(0xFF1A237E)),
                            center = Offset(w * 0.3f, h * 0.3f),
                            radius = w * 0.9f
                        )
                    )
                    drawCircle(
                        color = Color(0x4400E5FF),
                        radius = w * 0.45f,
                        center = Offset(w * 0.8f, h * 0.2f)
                    )
                    drawCircle(
                        color = Color(0x44FFD54F),
                        radius = w * 0.35f,
                        center = Offset(w * 0.2f, h * 0.8f)
                    )
                }
                1 -> {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF1E0A4E), Color(0xFF6E0D9D), Color(0xFFC70039)),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(w * 0.2f, h * 0.8f)
                            lineTo(w * 0.5f, h * 0.2f)
                            lineTo(w * 0.8f, h * 0.8f)
                            close()
                        },
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00E5FF), Color(0xFFFF007F))
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                    )
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(0f, h * 0.5f),
                        end = Offset(w, h * 0.1f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                2 -> {
                    drawRect(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color(0xFFFF5722), Color(0xFFE91E63), Color(0xFF00BCD4), Color(0xFFFFC107), Color(0xFFFF5722)),
                            center = Offset(w * 0.5f, h * 0.5f)
                        )
                    )
                }
                3 -> {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF004D40), Color(0xFF000000)),
                            center = Offset(w * 0.5f, h * 0.5f),
                            radius = w * 0.8f
                        )
                    )
                    drawCircle(color = Color(0x88FFC107), radius = w * 0.1f, center = Offset(w * 0.2f, h * 0.3f))
                    drawCircle(color = Color(0xAA00E5FF), radius = w * 0.05f, center = Offset(w * 0.8f, h * 0.2f))
                    drawCircle(color = Color(0x66FFC107), radius = w * 0.15f, center = Offset(w * 0.6f, h * 0.8f))
                    drawCircle(color = Color(0x9900E5FF), radius = w * 0.08f, center = Offset(w * 0.3f, h * 0.7f))
                }
                4 -> {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF3F51B5), Color(0xFF9C27B0))
                        )
                    )
                    drawRect(
                        color = Color(0xFFF44336),
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.5f)
                    )
                    drawRect(
                        color = Color(0xFFFF9800),
                        topLeft = Offset(w * 0.4f, h * 0.4f),
                        size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.6f)
                    )
                    drawCircle(
                        color = Color(0xFF2196F3),
                        radius = w * 0.25f,
                        center = Offset(w * 0.2f, h * 0.8f)
                    )
                }
                else -> {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF673AB7), Color(0xFFE91E63), Color(0xFF00BCD4)),
                            start = Offset(0f, h),
                            end = Offset(w, 0f)
                        )
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(0f, h * 0.2f)
                            quadraticBezierTo(w * 0.5f, h * 0.5f, w, 0f)
                        },
                        color = Color(0x66FFFFFF),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 16.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(0f, h * 0.8f)
                            quadraticBezierTo(w * 0.5f, h * 0.4f, w, h * 0.6f)
                        },
                        color = Color(0x66000000),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 24.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
            }
        }
        Text(
            text = if (mixIndex == 0) "PARA TI" else "MIX $mixIndex",
            color = Color.White,
            fontSize = if (mixIndex == 0) 22.sp else 28.sp,
            fontWeight = FontWeight.ExtraBold,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 2f),
                    blurRadius = 8f
                )
            )
        )
    }
}

@Composable
fun MixListItem(item: JellyfinItem, mixIndex: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        MixCoverArt(
            mixIndex = mixIndex,
            modifier = Modifier.size(140.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val artistStr = item.album ?: ""
        if (artistStr.isNotEmpty()) {
            Text(
                text = artistStr,
                color = SpotifyLightGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SongsList(
    items: List<JellyfinItem>,
    downloadedTracks: List<DownloadedTrack>,
    serverUrl: String,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp,
    isOfflineMode: Boolean = false,
    showOnlyFavorites: Boolean = false,
    onToggleShowOnlyFavorites: (() -> Unit)? = null,
    onExpandPlayer: () -> Unit,
    onAddToPlaylist: (JellyfinItem) -> Unit,
    onItemLongClick: ((JellyfinItem) -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleText = if (isOfflineMode) "Offline Songs" else if (showOnlyFavorites) "Favorite Songs" else "All Songs"
                Text(titleText, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onToggleShowOnlyFavorites != null) {
                        IconButton(onClick = onToggleShowOnlyFavorites) {
                            Icon(
                                imageVector = if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Show Favorites Only",
                                tint = if (showOnlyFavorites) SpotifyGreen else Color.White
                            )
                        }
                    }
                    val isShuffle = viewModel.isShuffleModeEnabled.collectAsState().value
                    IconButton(onClick = { viewModel.toggleShuffleMode() }) {
                        Icon(Icons.Default.Shuffle, "Shuffle", tint = if (isShuffle) SpotifyGreen else Color.White)
                    }
                    IconButton(onClick = { 
                        if (items.isNotEmpty()) {
                            viewModel.playTrackList(items, 0)
                            onExpandPlayer()
                        }
                    }) {
                        Icon(Icons.Default.PlayCircle, "Play All", tint = SpotifyGreen, modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
        if (items.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize().offset(y = (-40).dp), contentAlignment = Alignment.Center) {
                    Text("No tracks found.", color = SpotifyLightGray)
                }
            }
        } else {
            items(items.size) { index ->
                val item = items[index]
                val isDownloaded = downloadedTracks.any { it.id == item.id }
                val currentTrack = viewModel.currentTrack.collectAsState().value
                TrackListItem(
                    item = item,
                    isDownloaded = isDownloaded,
                    isActive = item.id == currentTrack?.id,
                    serverUrl = serverUrl,
                    onPlay = { 
                        viewModel.playTrackList(items, index)
                        onExpandPlayer()
                    },
                    onDownload = {
                        if (!isDownloaded && !isOfflineMode) viewModel.downloadItem(item)
                        else if (isOfflineMode) viewModel.deleteDownloadedItem(item)
                    },
                    isOfflineMode = isOfflineMode,
                    onToggleFavorite = { viewModel.toggleFavorite(item) },
                    onAddToPlaylist = { onAddToPlaylist(item) },
                    onLongClick = { onItemLongClick?.invoke(item) }
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GridContainersList(
    items: List<JellyfinItem>,
    serverUrl: String,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onItemLongClick: ((JellyfinItem) -> Unit)? = null
) {
    if (items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No items found.", color = SpotifyLightGray)
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = bottomPadding, start = 16.dp, end = 16.dp, top = 8.dp)
        ) {
            items(items) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { viewModel.pushContainer(item) },
                            onLongClick = { onItemLongClick?.invoke(item) }
                        )
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(SpotifyDarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            val imageUrl = "${serverUrl.removeSuffix("/")}/Items/${item.id}/Images/Primary?maxHeight=200&maxWidth=200"
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(item.type, color = SpotifyLightGray, fontSize = 14.sp)
                        }
                    }
                    IconButton(onClick = { viewModel.toggleFavorite(item) }) {
                        val isFav = item.userData?.isFavorite == true
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) SpotifyGreen else SpotifyLightGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContainerScreen(
    container: JellyfinItem,
    serverUrl: String,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onExpandPlayer: () -> Unit,
    onAddToPlaylist: (JellyfinItem) -> Unit,
    onItemLongClick: ((JellyfinItem) -> Unit)? = null
) {
    val containerItems by viewModel.containerItems.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState(initial = emptyList())
    val currentTrack by viewModel.currentTrack.collectAsState()

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isAiPlaylist = container.id.startsWith("mock_ai_playlist_") || container.id.startsWith("mock_qr_playlist_")
            val isMockMix = container.id.startsWith("mock_daily_mix_") || container.id == "mock_recommended_for_you"
            val mixIndex = when {
                container.id == "mock_recommended_for_you" -> 0
                container.id.startsWith("mock_daily_mix_") -> container.id.removePrefix("mock_daily_mix_").toIntOrNull() ?: 1
                else -> -1
            }

            if (isAiPlaylist) {
                AiPlaylistCoverArt(modifier = Modifier.size(120.dp))
            } else if (isMockMix) {
                MixCoverArt(
                    mixIndex = mixIndex,
                    modifier = Modifier.size(120.dp)
                )
            } else {
                Box(modifier = Modifier.size(120.dp).background(SpotifyDarkGray).clip(RoundedCornerShape(8.dp))) {
                    val imageUrl = "${serverUrl.removeSuffix("/")}/Items/${container.id}/Images/Primary?maxHeight=300&maxWidth=300"
                    AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(container.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(container.type, color = SpotifyLightGray, fontSize = 16.sp)
            }
        }

        if (containerItems == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SpotifyGreen)
            }
        } else if (containerItems!!.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Empty", color = SpotifyLightGray)
            }
        } else {
            val items = containerItems!!
            val isAudio = items.isNotEmpty() && items[0].type.equals("Audio", ignoreCase = true)
            
            if (isAudio) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isShuffle = viewModel.isShuffleModeEnabled.collectAsState().value
                    IconButton(onClick = { viewModel.toggleShuffleMode() }) {
                        Icon(Icons.Default.Shuffle, "Shuffle", tint = if (isShuffle) SpotifyGreen else Color.White)
                    }
                    IconButton(onClick = { 
                        viewModel.playTrackList(items, 0)
                        onExpandPlayer()
                    }) {
                        Icon(Icons.Default.PlayCircle, "Play", tint = SpotifyGreen, modifier = Modifier.size(56.dp))
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = if (currentTrack != null) 90.dp else 16.dp)
                ) {
                    items(items.size) { index ->
                        val item = items[index]
                        val isDownloaded = downloadedTracks.any { it.id == item.id }
                        TrackListItem(
                            item = item,
                            isDownloaded = isDownloaded,
                            isActive = item.id == currentTrack?.id,
                            serverUrl = serverUrl,
                            onPlay = { 
                                viewModel.playTrackList(items, index)
                                onExpandPlayer()
                            },
                            onDownload = { if (!isDownloaded) viewModel.downloadItem(item) },
                            isOfflineMode = false,
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onAddToPlaylist = { onAddToPlaylist(item) },
                            onLongClick = { onItemLongClick?.invoke(item) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = if (currentTrack != null) 90.dp else 16.dp, start = 16.dp, end = 16.dp, top = 8.dp)
                ) {
                    items(items.size) { index ->
                        val item = items[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.pushContainer(item) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(SpotifyDarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val imageUrl = "${serverUrl.removeSuffix("/")}/Items/${item.id}/Images/Primary?maxHeight=200&maxWidth=200"
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Cover",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text(item.type, color = SpotifyLightGray, fontSize = 14.sp)
                                }
                            }
                            IconButton(onClick = { viewModel.toggleFavorite(item) }) {
                                val isFav = item.userData?.isFavorite == true
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFav) SpotifyGreen else SpotifyLightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TrackListItem(
    item: JellyfinItem,
    isDownloaded: Boolean,
    isActive: Boolean,
    serverUrl: String,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    isOfflineMode: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onPlay() },
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SpotifyDarkGray),
            contentAlignment = Alignment.Center
        ) {
            val targetId = item.albumId ?: item.id
            val imageUrl = "${serverUrl.removeSuffix("/")}/Items/${targetId}/Images/Primary?maxHeight=100&maxWidth=100"
            AsyncImage(
                model = imageUrl,
                contentDescription = "Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = if (isActive) SpotifyGreen else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val artistStr = item.artists?.joinToString(", ") ?: item.album ?: "Unknown Artist"
            Text(
                text = artistStr,
                fontSize = 14.sp,
                color = SpotifyLightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = onToggleFavorite) {
            val isFav = item.userData?.isFavorite == true
            Icon(
                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFav) SpotifyGreen else SpotifyLightGray
            )
        }

        IconButton(onClick = onAddToPlaylist) {
            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist", tint = SpotifyLightGray)
        }

        IconButton(onClick = onDownload) {
            if (isDownloaded && !isOfflineMode) {
                Icon(Icons.Default.DownloadDone, contentDescription = "Downloaded", tint = SpotifyGreen)
            } else if (isOfflineMode) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = SpotifyLightGray)
            } else {
                Icon(Icons.Default.CloudDownload, contentDescription = "Download", tint = SpotifyLightGray)
            }
        }
    }
}

@Composable
fun NowPlayingBar(
    track: JellyfinItem,
    isPlaying: Boolean,
    viewModel: AppViewModel,
    serverUrl: String,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit
) {
    val position by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.trackDuration.collectAsState()
    val progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(64.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF303030)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable Album Art + Song Info area to open full screen player safely
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onExpand() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.DarkGray)
                    ) {
                        val targetId = track.albumId ?: track.id
                        val imageUrl = "${serverUrl.removeSuffix("/")}/Items/${targetId}/Images/Primary?maxHeight=100&maxWidth=100"
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val artistStr = track.artists?.joinToString(", ") ?: track.album ?: "Unknown Artist"
                        Text(
                            artistStr,
                            color = SpotifyLightGray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Controls: Favorite
                IconButton(onClick = { viewModel.toggleFavorite(track) }) {
                    val isFav = track.userData?.isFavorite == true
                    Icon(
                        imageVector = if(isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if(isFav) SpotifyGreen else SpotifyLightGray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Controls: Previous
                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Controls: Play/Pause
                IconButton(onClick = { viewModel.togglePlayPause() }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Controls: Next
                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Controls: Clear Playback Queue
                IconButton(onClick = { viewModel.clearQueue() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Queue",
                        tint = SpotifyLightGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            // Thin progress bar at the bottom
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun FullScreenPlayer(
    track: JellyfinItem,
    isPlaying: Boolean,
    viewModel: AppViewModel,
    serverUrl: String,
    onBack: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val position by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.trackDuration.collectAsState()
    val isShuffle by viewModel.isShuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    
    var isQueueVisible by remember { mutableStateOf(false) }
    val playQueue by viewModel.playQueue.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF5A4A30), SpotifyBlack),
        startY = 0f,
        endY = 2000f
    )

    if (isQueueVisible) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotifyBlack)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isQueueVisible = false }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close Queue", tint = Color.White)
                }
                Text("Now Playing", color = Color.White, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.size(48.dp))
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(playQueue) { index, item ->
                    val isPlayingNow = index == currentQueueIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // optional: skip to this track
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                color = if (isPlayingNow) SpotifyGreen else Color.White,
                                fontWeight = if (isPlayingNow) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val artistStr = item.artists?.joinToString(", ") ?: item.album ?: "Unknown Artist"
                            Text(
                                text = artistStr,
                                color = SpotifyLightGray,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "REPRODUCIENDO DESDE LISTA",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = track.album ?: "Now Playing",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { /* More options placeholder */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
            }
        }

        // Cover Art
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            val targetId = track.albumId ?: track.id
            val imageUrl = "${serverUrl.removeSuffix("/")}/Items/${targetId}/Images/Primary?maxHeight=600&maxWidth=600"
            AsyncImage(
                model = imageUrl,
                contentDescription = "Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Track Info & Add
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val artistStr = track.artists?.joinToString(", ") ?: track.album ?: "Unknown Artist"
                Text(
                    text = artistStr,
                    color = SpotifyLightGray,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add to playlist",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp).border(1.dp, Color.White, CircleShape).padding(4.dp)
                )
            }
        }

        // Seek Bar
        AnimatedMusicSeekBar(
            trackId = track.id,
            position = position,
            duration = duration,
            isPlaying = isPlaying,
            onSeek = { newPos ->
                viewModel.seekTo(newPos)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 8.dp)
        )

        // Play Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleShuffleMode() }) {
                Icon(Icons.Default.Shuffle, "Shuffle", tint = if (isShuffle) SpotifyGreen else Color.White)
            }
            IconButton(onClick = { viewModel.playPrevious() }) {
                Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = { viewModel.playNext() }) {
                Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                when (repeatMode) {
                    androidx.media3.common.Player.REPEAT_MODE_ONE -> {
                        Icon(Icons.Default.RepeatOne, "Repeat One", tint = SpotifyGreen)
                    }
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> {
                        Icon(Icons.Default.Repeat, "Repeat All", tint = SpotifyGreen)
                    }
                    else -> {
                        Icon(Icons.Default.Repeat, "Repeat Off", tint = Color.White)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom Tools
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Devices, contentDescription = "Devices", tint = Color.LightGray, modifier = Modifier.size(24.dp))
            IconButton(onClick = { isQueueVisible = true }) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun PlaylistSelectionDialog(
    viewModel: AppViewModel,
    track: JellyfinItem,
    onDismiss: () -> Unit
) {
    val playlistsRaw by viewModel.playlists.collectAsState()
    val playlists = playlistsRaw.filter { it.name.trim() != "." }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist", color = Color.White) },
        text = {
            if (playlists.isEmpty()) {
                Text("No playlists found.", color = SpotifyLightGray)
            } else {
                LazyColumn {
                    items(playlists) { playlist ->
                        Text(
                            text = playlist.name,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addToPlaylist(track, playlist)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SpotifyGreen)
            }
        },
        containerColor = SpotifyDarkGray
    )
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun AnimatedMusicSeekBar(
    trackId: String,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    key(trackId) {
        val validDuration = duration.coerceAtLeast(1L)
        val validPosition = position.coerceIn(0L, validDuration)
        
        var isDragging by remember { mutableStateOf(false) }
        var dragProgress by remember { mutableStateOf(0f) }
        
        var lastSeekedPosition by remember { mutableStateOf(-1L) }
        
        // Safety timeout to reset seek state if player takes too long to catch up
        LaunchedEffect(lastSeekedPosition) {
            if (lastSeekedPosition != -1L) {
                kotlinx.coroutines.delay(1000)
                lastSeekedPosition = -1L
            }
        }
        
        // Clear seek override once player position has caught up close to the seeked position
        LaunchedEffect(position) {
            if (lastSeekedPosition != -1L) {
                val diff = kotlin.math.abs(position - lastSeekedPosition)
                if (diff < 2000) {
                    lastSeekedPosition = -1L
                }
            }
        }
        
        val displayPosition = if (isDragging) {
            (dragProgress * validDuration).toLong()
        } else if (lastSeekedPosition != -1L) {
            lastSeekedPosition
        } else {
            validPosition
        }
        
        val targetFraction = displayPosition.toFloat() / validDuration.toFloat()
        
        var lastFraction by remember { mutableStateOf(0f) }
        val isBigJump = kotlin.math.abs(targetFraction - lastFraction) > 0.05f
        
        LaunchedEffect(targetFraction) {
            lastFraction = targetFraction
        }
        
        val animatedFraction by animateFloatAsState(
            targetValue = targetFraction,
            animationSpec = if (isPlaying && !isDragging && !isBigJump && lastSeekedPosition == -1L) {
                tween(durationMillis = 1000, easing = LinearEasing)
            } else {
                snap()
            },
            label = "SeekProgress"
        )
        
        val currentFraction = if (isDragging) dragProgress else animatedFraction
        
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isPlaying) 1.25f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        
        val isInteracting = isDragging
        
        val trackHeight by animateDpAsState(
            targetValue = if (isInteracting) 6.dp else 4.dp,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "trackHeight"
        )
        
        val thumbRadius by animateDpAsState(
            targetValue = if (isInteracting) 8.dp else 4.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "thumbRadius"
        )
        
        val glowAlpha by animateFloatAsState(
            targetValue = if (isInteracting) 0.35f else 0.15f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "glowAlpha"
        )

        Column(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .pointerInput(validDuration) {
                        awaitPointerEventScope {
                            while (true) {
                                val firstEvent = awaitPointerEvent()
                                val down = firstEvent.changes.firstOrNull { it.pressed } ?: continue
                                isDragging = true
                                dragProgress = (down.position.x / size.width).coerceIn(0f, 1f)
                                
                                var pointerId = down.id
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val anyPressed = event.changes.any { it.pressed }
                                    if (!anyPressed) {
                                        break
                                    }
                                    val activePointer = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull { it.pressed } ?: event.changes.first()
                                    pointerId = activePointer.id
                                    dragProgress = (activePointer.position.x / size.width).coerceIn(0f, 1f)
                                    event.changes.forEach { it.consume() }
                                }
                                isDragging = false
                                val seekPos = (dragProgress * validDuration).toLong()
                                lastSeekedPosition = seekPos
                                onSeek(seekPos)
                            }
                        }
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f
                    
                    val barCount = 45
                    val barSpacingPx = 3.dp.toPx()
                    val totalSpacing = barSpacingPx * (barCount - 1)
                    val barWidthPx = (width - totalSpacing) / barCount
                    
                    if (barWidthPx > 0) {
                        for (i in 0 until barCount) {
                            val x = i * (barWidthPx + barSpacingPx) + barWidthPx / 2f
                            val fractionAtBar = i.toFloat() / barCount.toFloat()
                            
                            val isActive = fractionAtBar <= currentFraction
                            
                            val waveFactor = kotlin.math.sin(i * 0.35 + (if (isPlaying) System.currentTimeMillis() / 250.0 else 0.0)).toFloat()
                            val normalizedWave = (waveFactor + 1f) / 2f
                            val baseHeight = 3.dp.toPx()
                            val maxHeight = 16.dp.toPx()
                            val barHeight = baseHeight + normalizedWave * (maxHeight - baseHeight) * (if (isPlaying) pulseScale else 1f)
                            
                            val color = if (isActive) {
                                SpotifyGreen.copy(alpha = 0.35f)
                            } else {
                                Color.White.copy(alpha = 0.12f)
                            }
                            
                            drawLine(
                                color = color,
                                start = Offset(x, centerY - barHeight / 2f),
                                end = Offset(x, centerY + barHeight / 2f),
                                strokeWidth = barWidthPx,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    
                    val lineY = centerY
                    val activeColor = SpotifyGreen
                    val inactiveColor = Color.White.copy(alpha = 0.24f)
                    
                    drawLine(
                        color = inactiveColor,
                        start = Offset(0f, lineY),
                        end = Offset(width, lineY),
                        strokeWidth = trackHeight.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    val activeWidth = width * currentFraction
                    if (activeWidth > 0f) {
                        drawLine(
                            color = activeColor,
                            start = Offset(0f, lineY),
                            end = Offset(activeWidth, lineY),
                            strokeWidth = trackHeight.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    
                    val thumbX = activeWidth
                    val pulseRadius = thumbRadius.toPx() * (if (isPlaying) pulseScale else 1f)
                    
                    if (isInteracting || isPlaying) {
                        drawCircle(
                            color = SpotifyGreen.copy(alpha = glowAlpha),
                            radius = pulseRadius * 1.8f,
                            center = Offset(thumbX, lineY)
                        )
                    }
                    
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadius.toPx(),
                        center = Offset(thumbX, lineY)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(displayPosition),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTime(validDuration),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AiPlaylistCoverArt(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE2FF70),
                        Color(0xFF42D6A4),
                        Color(0xFFC7F038),
                        Color(0xFF00E5FF)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner soft radial diamond area matching the uploaded image style
        Box(
            modifier = Modifier
                .fillMaxSize(0.88f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFEAF5DD),
                            Color(0xFFB0F25A),
                            Color(0xFF1DE9B6)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // "Ai" title with red sparkles
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Ai",
                        color = Color(0xFFE53935),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0x55000000),
                                offset = Offset(2f, 3f),
                                blurRadius = 3f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "Playlist",
                    color = Color(0xFFE53935),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0x55000000),
                            offset = Offset(2f, 3f),
                            blurRadius = 3f
                        )
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Red circle audio emblem
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AiGenerateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = remember {
        Brush.sweepGradient(
            colors = listOf(
                Color(0xFF00E5FF),
                Color(0xFF7C4DFF),
                Color(0xFFE91E63),
                Color(0xFF00E5FF)
            )
        )
    }

    Box(
        modifier = modifier
            .height(30.dp)
            .clip(CircleShape)
            .background(gradientBrush)
            .padding(1.5.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E1E2C))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "IA Generate",
                tint = Color(0xFF80D8FF),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Ai",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPlaylistDialog(
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit
) {
    var promptText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                promptText = spokenText
                errorMessage = null
            }
        }
    }

    fun launchVoiceRecognition() {
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di los artistas para tu playlist...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Reconocimiento de voz no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    val aiGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF00E5FF), Color(0xFF9C27B0), Color(0xFFE91E63))
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181824),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(aiGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Crear Playlist con IA", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Playlist instantánea por voz o texto", color = SpotifyLightGray, fontSize = 12.sp)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Habla o escribe qué artistas o estilo quieres incluir:",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Voice Recording Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { launchVoiceRecognition() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(aiGradient)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = "Hablar", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pulsar para Hablar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = promptText,
                    onValueChange = { 
                        promptText = it 
                        errorMessage = null
                    },
                    placeholder = { Text("Ej: Creame una playlist con Quevedo, Bad bunny y Paulo Londra", color = SpotifyLightGray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = SpotifyDarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF10101A),
                        unfocusedContainerColor = Color(0xFF10101A)
                    ),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = Color(0xFFFF5252), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Sugerencias rápidas:", color = SpotifyLightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                val suggestions = listOf(
                    "Quevedo, Bad Bunny y Paulo Londra",
                    "Duki, Bizarrap y Trueno",
                    "Aitana, Morat y Sebastian Yatra"
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { sugg ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(SpotifyDarkGray)
                                .clickable {
                                    promptText = "Creame una playlist con $sugg"
                                    errorMessage = null
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(sugg, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (promptText.isBlank()) {
                        errorMessage = "Por favor di o escribe los artistas que deseas."
                    } else {
                        onGenerate(promptText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(aiGradient)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Generar Playlist", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SpotifyLightGray)
            }
        }
    )
}

@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 512,
    foregroundColor: Color = Color.White,
    backgroundColor: Color = Color(0xFF10101A)
) {
    val bitmap = remember(content) {
        try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, sizePx, sizePx)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val fg = foregroundColor.toArgb()
            val bg = backgroundColor.toArgb()
            for (x in 0 until w) {
                for (y in 0 until h) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) fg else bg)
                }
            }
            bmp.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "QR Code",
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text("QR Error", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
fun QrPartyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = remember {
        Brush.sweepGradient(
            colors = listOf(
                Color(0xFF00E5FF),
                Color(0xFF00E676),
                Color(0xFFFFD600),
                Color(0xFF00E5FF)
            )
        )
    }

    Box(
        modifier = modifier
            .height(30.dp)
            .clip(CircleShape)
            .background(gradientBrush)
            .padding(1.5.dp)
            .clip(CircleShape)
            .background(Color(0xFF101C24))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = "QR Fiesta",
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "QR Fiesta",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrPartyDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onOpenGuestSearch: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val qrCode by viewModel.qrSessionCode.collectAsState()
    val requests by viewModel.qrRequests.collectAsState()
    val activePlaylistTracks by viewModel.qrPlaylistTracks.collectAsState()

    val localServerUrl = remember { viewModel.startPartyServer() }
    val publicAppUrl = "https://cardamu.es/jellymusic/party.es"
    var useLocalWifi by remember { mutableStateOf(false) }

    val activeUrl = if (useLocalWifi) localServerUrl else "$publicAppUrl?partyCode=$qrCode"

    val qrGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF00E5FF), Color(0xFF00E676), Color(0xFFFFD600))
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141824),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(qrGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("QR Playlist para Fiesta", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Los invitados escanean y abren la web gratis", color = SpotifyLightGray, fontSize = 12.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                // QR Display Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0A0D14))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            QrCodeImage(
                                content = activeUrl,
                                sizePx = 400,
                                foregroundColor = Color.Black,
                                backgroundColor = Color.White,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Código de sesión: $qrCode",
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeUrl,
                            color = SpotifyLightGray,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(activeUrl))
                                    Toast.makeText(context, "Enlace web copiado: $activeUrl", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, SpotifyLightGray),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar Link Web", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { useLocalWifi = !useLocalWifi },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                                border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    if (useLocalWifi) Icons.Default.Wifi else Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (useLocalWifi) "Wi-Fi Web" else "cardamu.es Web", fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Guest Mode Trigger
                Button(
                    onClick = {
                        onDismiss()
                        onOpenGuestSearch()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(qrGradient)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simular Escaneo / Probar como Invitado", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Open Created QR Playlist
                Button(
                    onClick = {
                        onDismiss()
                        viewModel.openQrPlaylist()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyDarkGray),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = SpotifyGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Ver Playlist QR Fiesta (${activePlaylistTracks.size} canciones)",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Incoming Requests List
                Text(
                    "Peticiones de Invitados (${requests.size}):",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (requests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aún no hay peticiones. Escanea el QR para agregar la primera canción.",
                            color = SpotifyLightGray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(requests) { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2234)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LibraryMusic,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            req.songTitle,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Pedida por: ${req.guestName}",
                                            color = SpotifyLightGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (req.matchedItem != null) {
                                        IconButton(
                                            onClick = {
                                                viewModel.playTrackList(listOf(req.matchedItem))
                                                Toast.makeText(context, "Reproduciendo ${req.matchedItem.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = SpotifyGreen)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.resetQrSession()
                    Toast.makeText(context, "Sesión QR reiniciada", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Reiniciar QR", color = Color(0xFFFF5252), fontSize = 12.sp)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrGuestSearchDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var guestName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var manualSongQuery by remember { mutableStateOf("") }

    val allSongs by viewModel.items.collectAsState()

    val filteredSongs = remember(searchQuery, allSongs) {
        if (searchQuery.isBlank()) {
            allSongs.take(15)
        } else {
            val q = searchQuery.lowercase().trim()
            allSongs.filter {
                it.name.lowercase().contains(q) ||
                it.artists?.any { a -> a.lowercase().contains(q) } == true ||
                (it.album != null && it.album.lowercase().contains(q))
            }.take(20)
        }
    }

    val qrGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF00E5FF), Color(0xFF00E676))
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121622),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(qrGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Buscador de Canciones", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Añadir tema a la Playlist QR del anfitrión", color = SpotifyLightGray, fontSize = 12.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                OutlinedTextField(
                    value = guestName,
                    onValueChange = { guestName = it },
                    placeholder = { Text("Tu Nombre / Apodo (ej: Carlos)", color = SpotifyLightGray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = SpotifyDarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0D111A),
                        unfocusedContainerColor = Color(0xFF0D111A)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar canción en la biblioteca...", color = SpotifyLightGray, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = SpotifyDarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0D111A),
                        unfocusedContainerColor = Color(0xFF0D111A)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Resultados (${filteredSongs.size}):", color = SpotifyLightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredSongs) { song ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2232)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.submitGuestRequest(guestName, song.name)
                                    viewModel.addTrackToQrPlaylist(song)
                                    Toast.makeText(context, "¡'${song.name}' añadida a la Playlist QR!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        song.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        song.artists?.joinToString(", ") ?: (song.album ?: "Desconocido"),
                                        color = SpotifyLightGray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.submitGuestRequest(guestName, song.name)
                                        viewModel.addTrackToQrPlaylist(song)
                                        Toast.makeText(context, "¡'${song.name}' añadida a la Playlist QR!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Agregar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("¿No la encuentras? Escribe el título:", color = SpotifyLightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = manualSongQuery,
                        onValueChange = { manualSongQuery = it },
                        placeholder = { Text("Ej: Quevedo - Columbia", color = SpotifyLightGray, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = SpotifyDarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0D111A),
                            unfocusedContainerColor = Color(0xFF0D111A)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (manualSongQuery.isNotBlank()) {
                                viewModel.submitGuestRequest(guestName, manualSongQuery)
                                Toast.makeText(context, "Petición enviada a la Playlist QR", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Enviar", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SpotifyLightGray)
            }
        }
    )
}
