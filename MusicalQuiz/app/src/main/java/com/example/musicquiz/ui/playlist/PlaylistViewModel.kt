package com.example.musicquiz.ui.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.musicquiz.database.PlaylistRepository
import com.example.musicquiz.model.LocalTrack
import com.example.musicquiz.model.Playlist
import com.example.musicquiz.util.PlaylistUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val playlistRepository = PlaylistRepository(application)

    private val _playlists = MutableLiveData<List<Playlist>>()
    val playlists: LiveData<List<Playlist>> = _playlists

    private val _currentPlaylist = MutableLiveData<Playlist>()
    val currentPlaylist: LiveData<Playlist> = _currentPlaylist

    private val _playlistTracks = MutableLiveData<List<LocalTrack>>()
    val playlistTracks: LiveData<List<LocalTrack>> = _playlistTracks

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> = _operationSuccess

    init {
        loadPlaylists()

        // Observe playlist updates from other parts of the app
        PlaylistUpdateManager.playlistUpdated.observeForever { updated ->
            if (updated) {
                loadPlaylists()
                PlaylistUpdateManager.resetUpdateFlag()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Remove the observer when the ViewModel is cleared to prevent memory leaks
        PlaylistUpdateManager.playlistUpdated.removeObserver { }
    }

    /**
     * Load all playlists from the database
     */
    fun loadPlaylists() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    playlistRepository.getAllPlaylists()
                }
                _playlists.value = result
            } catch (e: Exception) {
                _error.value = "Error loading playlists: ${e.message}"
            }
        }
    }

    /**
     * Create a new playlist
     */
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            try {
                val playlist = Playlist(name = name)
                val playlistId = withContext(Dispatchers.IO) {
                    playlistRepository.createPlaylist(playlist)
                }

                if (playlistId > 0) {
                    _operationSuccess.value = true
                    loadPlaylists()
                } else {
                    _error.value = "Failed to create playlist"
                    _operationSuccess.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error creating playlist: ${e.message}"
                _operationSuccess.value = false
            }
        }
    }

    /**
     * Update a playlist
     */
    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    playlistRepository.updatePlaylist(playlist)
                }

                if (result > 0) {
                    _operationSuccess.value = true
                    loadPlaylists()
                } else {
                    _error.value = "Failed to update playlist"
                    _operationSuccess.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error updating playlist: ${e.message}"
                _operationSuccess.value = false
            }
        }
    }

    /**
     * Delete a playlist
     */
    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    playlistRepository.deletePlaylist(playlistId)
                }

                if (result > 0) {
                    _operationSuccess.value = true
                    loadPlaylists()
                } else {
                    _error.value = "Failed to delete playlist"
                    _operationSuccess.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error deleting playlist: ${e.message}"
                _operationSuccess.value = false
            }
        }
    }

    /**
     * Load a playlist by ID
     */
    fun loadPlaylist(playlistId: Long) {
        viewModelScope.launch {
            try {
                val playlist = withContext(Dispatchers.IO) {
                    playlistRepository.getPlaylistById(playlistId)
                }

                if (playlist != null) {
                    _currentPlaylist.value = playlist
                    loadPlaylistTracks(playlistId)
                } else {
                    _error.value = "Playlist not found"
                }
            } catch (e: Exception) {
                _error.value = "Error loading playlist: ${e.message}"
            }
        }
    }

    /**
     * Load tracks for a playlist
     */
    fun loadPlaylistTracks(playlistId: Long) {
        viewModelScope.launch {
            try {
                val tracks = withContext(Dispatchers.IO) {
                    playlistRepository.getTracksInPlaylist(playlistId)
                }
                _playlistTracks.value = tracks
            } catch (e: Exception) {
                _error.value = "Error loading playlist tracks: ${e.message}"
            }
        }
    }

    /**
     * Remove a track from a playlist
     */
    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
                }

                if (result > 0) {
                    _operationSuccess.value = true
                    loadPlaylistTracks(playlistId)
                    loadPlaylists() // Refresh playlist list to update track count
                } else {
                    _error.value = "Failed to remove track from playlist"
                    _operationSuccess.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error removing track from playlist: ${e.message}"
                _operationSuccess.value = false
            }
        }
    }

    /**
     * Reset operation success flag
     */
    fun resetOperationSuccess() {
        _operationSuccess.value = false
    }
}
