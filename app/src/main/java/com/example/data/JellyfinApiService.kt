package com.example.data

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class CreatePlaylistRequest(
    @Json(name = "Name") val name: String,
    @Json(name = "UserId") val userId: String,
    @Json(name = "MediaType") val mediaType: String = "Audio"
)

@JsonClass(generateAdapter = true)
data class UpdatePlaylistRequest(
    @Json(name = "Name") val name: String
)

interface JellyfinApiService {
    @POST("Users/AuthenticateByName")
    suspend fun authenticate(
        @Header("Authorization") authHeader: String,
        @Body request: AuthRequest
    ): AuthResponse

    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Header("x-emby-token") token: String,
        @Query("ParentId") parentId: String? = null,
        @Query("IncludeItemTypes") includeTypes: String? = null,
        @Query("Recursive") recursive: Boolean = true,
        @Query("SortBy") sortBy: String? = null,
        @Query("Limit") limit: Int? = null,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,UserData,PlaylistItemId",
        @Query("Filters") filters: String? = null,
        @Query("ArtistIds") artistIds: String? = null
    ): ItemsResponse

    @GET("Artists")
    suspend fun getArtists(
        @Query("userId") userId: String,
        @Header("x-emby-token") token: String,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,UserData"
    ): ItemsResponse

    @POST("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun markFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("x-emby-token") token: String
    ): UserData

    @DELETE("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun unmarkFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("x-emby-token") token: String
    ): UserData

    @GET("Playlists/{playlistId}/Items")
    suspend fun getPlaylistItems(
        @Path("playlistId") playlistId: String,
        @Query("userId") userId: String,
        @Header("x-emby-token") token: String,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,UserData,PlaylistItemId"
    ): ItemsResponse

    @POST("Playlists/{playlistId}/Items")
    suspend fun addToPlaylist(
        @Path("playlistId") playlistId: String,
        @Query("Ids") ids: String,
        @Query("userId") userId: String,
        @Header("x-emby-token") token: String
    ): retrofit2.Response<Unit>

    @POST("Playlists")
    suspend fun createPlaylist(
        @Body request: CreatePlaylistRequest,
        @Header("x-emby-token") token: String
    ): retrofit2.Response<PlaylistCreationResult>

    @DELETE("Items/{itemId}")
    suspend fun deleteItem(
        @Path("itemId") itemId: String,
        @Header("x-emby-token") token: String
    ): retrofit2.Response<Unit>

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItemRaw(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("x-emby-token") token: String
    ): okhttp3.ResponseBody

    @POST("Playlists/{playlistId}")
    suspend fun updatePlaylist(
        @Path("playlistId") playlistId: String,
        @Body request: UpdatePlaylistRequest,
        @Header("x-emby-token") token: String
    ): retrofit2.Response<Unit>

    @DELETE("Playlists/{playlistId}/Items")
    suspend fun removeFromPlaylist(
        @Path("playlistId") playlistId: String,
        @Query("entryIds") entryIds: String,
        @Header("x-emby-token") token: String
    ): retrofit2.Response<Unit>
}

object ApiClient {
    fun create(baseUrl: String): JellyfinApiService {
        val moshi = Moshi.Builder().build()
        
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
            
        var safeBaseUrl = baseUrl.trim()
        if (!safeBaseUrl.startsWith("http://") && !safeBaseUrl.startsWith("https://")) {
            safeBaseUrl = "http://$safeBaseUrl"
        }
        if (!safeBaseUrl.endsWith("/")) {
            safeBaseUrl = "$safeBaseUrl/"
        }
            
        return Retrofit.Builder()
            .baseUrl(safeBaseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JellyfinApiService::class.java)
    }
}
