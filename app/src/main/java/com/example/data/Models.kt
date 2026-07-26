package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class AuthRequest(
    @Json(name = "Username") val username: String,
    @Json(name = "Pw") val pw: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "AccessToken") val accessToken: String,
    @Json(name = "SessionInfo") val sessionInfo: SessionInfo? = null,
    @Json(name = "User") val user: User
)

@JsonClass(generateAdapter = true)
data class SessionInfo(
    @Json(name = "UserId") val userId: String
)

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String
)

@JsonClass(generateAdapter = true)
data class ItemsResponse(
    @Json(name = "Items") val items: List<JellyfinItem>
)

@JsonClass(generateAdapter = true)
data class UserData(
    @Json(name = "IsFavorite") val isFavorite: Boolean = false,
    @Json(name = "PlayCount") val playCount: Int? = null,
    @Json(name = "Played") val played: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistCreationResult(
    @Json(name = "Id") val id: String
)

@JsonClass(generateAdapter = true)
data class JellyfinItem(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String,
    @Json(name = "Type") val type: String, // "Audio", "MusicAlbum", "MusicArtist", "Playlist"
    @Json(name = "Album") val album: String? = null,
    @Json(name = "AlbumId") val albumId: String? = null,
    @Json(name = "Artists") val artists: List<String>? = null,
    @Json(name = "RunTimeTicks") val runTimeTicks: Long? = null,
    @Json(name = "UserData") val userData: UserData? = null,
    @Json(name = "PlaylistItemId") val playlistItemId: String? = null,
    @Json(name = "PremiereDate") val premiereDate: String? = null,
    @Json(name = "ProductionYear") val productionYear: Int? = null,
    @Json(name = "DateCreated") val dateCreated: String? = null
)

data class QrSongRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val guestName: String,
    val songTitle: String,
    val matchedItem: JellyfinItem? = null,
    val timestamp: Long = System.currentTimeMillis()
)
