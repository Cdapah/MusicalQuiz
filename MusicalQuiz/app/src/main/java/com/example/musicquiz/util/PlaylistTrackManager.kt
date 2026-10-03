package com.example.musicquiz.util

import android.content.Context
import com.example.musicquiz.database.PlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class to update playlist track counts and notify UI components
 */
object PlaylistTrackManager {

    /**
     * Update a playlist's track count and notify UI components
     */
    fun updatePlaylistTrackCount(context: Context, playlistId: Long) {
        val playlistRepository = PlaylistRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            // Get the actual track count from the database
            val trackCount = playlistRepository.getTracksInPlaylist(playlistId).size

            // Update the playlist's updatedAt timestamp
            val playlist = playlistRepository.getPlaylistById(playlistId)
            if (playlist != null) {
                val updatedPlaylist = playlist.copy(
                    trackCount = trackCount,  // This will be handled by the UI
                    updatedAt = System.currentTimeMillis()
                )
                playlistRepository.updatePlaylist(updatedPlaylist)

                // Notify other parts of the app about the update
                PlaylistUpdateManager.notifyPlaylistUpdated()
            }
        }
    }
}
