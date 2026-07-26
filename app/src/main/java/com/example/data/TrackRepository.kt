package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class TrackRepository(private val context: Context, private val dao: DownloadedTrackDao) {
    
    val allDownloadedTracks: Flow<List<DownloadedTrack>> = dao.getAllTracks()
    
    suspend fun isDownloaded(id: String): Boolean {
        return dao.getTrackById(id) != null
    }

    suspend fun downloadTrack(item: JellyfinItem, serverUrl: String, token: String) {
        val client = OkHttpClient()
        val url = serverUrl.removeSuffix("/")
        val streamUrl = "$url/Audio/${item.id}/stream?static=true&api_key=$token"
        
        val request = Request.Builder().url(streamUrl).build()
        
        withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext
                
                val body = response.body ?: return@withContext
                
                // save to local file
                val dir = File(context.filesDir, "downloads")
                if (!dir.exists()) dir.mkdirs()
                
                val file = File(dir, "${item.id}.mp3")
                val fos = FileOutputStream(file)
                fos.write(body.bytes())
                fos.close()
                
                // save metadata to db
                val dt = DownloadedTrack(
                    id = item.id,
                    name = item.name,
                    album = item.album,
                    albumId = item.albumId,
                    artistsName = item.artists?.joinToString(", "),
                    localFilePath = file.absolutePath,
                    runTimeTicks = item.runTimeTicks
                )
                dao.insertTrack(dt)
            } catch (e: Exception) {
                Log.e("TrackRepo", "Error downloading", e)
            }
        }
    }

    suspend fun deleteTrack(item: JellyfinItem) {
        withContext(Dispatchers.IO) {
            val track = dao.getTrackById(item.id) ?: return@withContext
            val file = File(track.localFilePath)
            if (file.exists()) file.delete()
            dao.deleteTrackById(item.id)
        }
    }
}
