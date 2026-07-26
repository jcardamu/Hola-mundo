package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedTrackDao {
    @Query("SELECT * FROM downloaded_tracks")
    fun getAllTracks(): Flow<List<DownloadedTrack>>

    @Query("SELECT * FROM downloaded_tracks WHERE id = :id")
    suspend fun getTrackById(id: String): DownloadedTrack?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: DownloadedTrack)

    @Query("DELETE FROM downloaded_tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)
}
