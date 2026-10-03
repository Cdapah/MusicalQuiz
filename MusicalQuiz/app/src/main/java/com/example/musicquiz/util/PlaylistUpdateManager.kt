package com.example.musicquiz.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Singleton manager to notify different parts of the app about playlist updates
 */
object PlaylistUpdateManager {
    private val _playlistUpdated = MutableLiveData<Boolean>()
    val playlistUpdated: LiveData<Boolean> = _playlistUpdated

    /**
     * Notify that a playlist has been created or updated
     */
    fun notifyPlaylistUpdated() {
        _playlistUpdated.postValue(true)
    }

    /**
     * Reset the update flag after handling the update
     */
    fun resetUpdateFlag() {
        _playlistUpdated.postValue(false)
    }
}
