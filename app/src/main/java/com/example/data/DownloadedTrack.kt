package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrack(
    @PrimaryKey val id: String,
    val name: String,
    val album: String?,
    val albumId: String?,
    val artistsName: String?,
    val localFilePath: String,
    val runTimeTicks: Long?
)
