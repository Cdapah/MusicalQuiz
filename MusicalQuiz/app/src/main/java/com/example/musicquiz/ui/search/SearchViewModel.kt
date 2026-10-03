package com.example.musicquiz.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.musicquiz.api.DeezerApiClient
import com.example.musicquiz.database.PlaylistRepository
import com.example.musicquiz.database.TrackRepository
import com.example.musicquiz.model.Album
import com.example.musicquiz.model.LocalTrack
import com.example.musicquiz.model.Playlist
import com.example.musicquiz.model.Track
import com.example.musicquiz.util.PlaylistTrackManager
import com.example.musicquiz.util.PlaylistUpdateManager
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val trackRepository = TrackRepository(application)
    private val playlistRepository = PlaylistRepository(application)

    private val _searchResults = MutableLiveData<List<Any>>()
    val searchResults: LiveData<List<Any>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String> = _error as LiveData<String>

    private val _addToPlaylistSuccess = MutableLiveData<Boolean>()
    val addToPlaylistSuccess: LiveData<Boolean> = _addToPlaylistSuccess

    /**
     * Search for tracks or albums based on query and type
     */
    fun search(query: String, searchType: SearchType) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                when (searchType) {
                    SearchType.TRACK -> {
                        val response = DeezerApiClient.apiService.searchTracks(query)
                        if (response.isSuccessful) {
                            val tracks = response.body()?.data ?: emptyList()
                            _searchResults.value = tracks
                        } else {
                            _error.value = "Error: ${response.code()} - ${response.message()}"
                        }
                    }
                    SearchType.ALBUM -> {
                        val response = DeezerApiClient.apiService.searchAlbums(query)
                        if (response.isSuccessful) {
                            val albums = response.body()?.data ?: emptyList()
                            _searchResults.value = albums
                        } else {
                            _error.value = "Error: ${response.code()} - ${response.message()}"
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add a track to a playlist
     */
    fun addTrackToPlaylist(track: Track, playlistId: Long) {
        viewModelScope.launch {
            try {
                // Convert Deezer track to local track
                val localTrack = trackRepository.convertDeezerTrackToLocal(track)

                // Save track to database
                val trackId = trackRepository.saveTrack(localTrack)

                // Add track to playlist
                playlistRepository.addTrackToPlaylist(playlistId, trackId)

                // Update the playlist's track count and notify UI components
                PlaylistTrackManager.updatePlaylistTrackCount(getApplication(), playlistId)

                _addToPlaylistSuccess.value = true
            } catch (e: Exception) {
                _error.value = "Error adding track to playlist: ${e.message}"
                _addToPlaylistSuccess.value = false
            }
        }
    }

    /**
     * Create a new playlist and optionally add a track to it
     */
    fun createPlaylist(playlist: Playlist, track: Track? = null) {
        viewModelScope.launch {
            try {
                // Create playlist
                val playlistId = playlistRepository.createPlaylist(playlist)

                // If a track was provided, add it to the playlist
                if (track != null) {
                    addTrackToPlaylist(track, playlistId)
                }

                // Notify other parts of the app that a playlist has been created
                PlaylistUpdateManager.notifyPlaylistUpdated()

            } catch (e: Exception) {
                _error.value = "Error creating playlist: ${e.message}"
            }
        }
    }

    /**
     * Get track details
     */
    fun getTrackDetails(trackId: Long, callback: (Track?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = DeezerApiClient.apiService.getTrack(trackId)
                if (response.isSuccessful) {
                    callback(response.body())
                } else {
                    _error.value = "Error: ${response.code()} - ${response.message()}"
                    callback(null)
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                callback(null)
            }
        }
    }

    /**
     * Get album details
     */
    fun getAlbumDetails(albumId: Long, callback: (AlbumWithTracks?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = DeezerApiClient.apiService.getAlbum(albumId)
                if (response.isSuccessful) {
                    val albumDetails = response.body()
                    if (albumDetails != null) {
                        val albumWithTracks = AlbumWithTracks(
                            album = Album(
                                id = albumDetails.id,
                                title = albumDetails.title,
                                coverSmall = albumDetails.coverSmall,
                                coverMedium = albumDetails.coverMedium,
                                coverBig = albumDetails.coverBig,
                                releaseDate = albumDetails.releaseDate
                            ),
                            tracks = albumDetails.tracks.data
                        )
                        callback(albumWithTracks)
                    } else {
                        callback(null)
                    }
                } else {
                    _error.value = "Error: ${response.code()} - ${response.message()}"
                    callback(null)
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                callback(null)
            }
        }
    }

    /**
     * Reset add to playlist success flag
     */
    fun resetAddToPlaylistSuccess() {
        _addToPlaylistSuccess.value = false
    }
}

/**
 * Enum for search types
 */
enum class SearchType {
    TRACK,
    ALBUM
}

/**
 * Data class to hold album with its tracks
 */
data class AlbumWithTracks(
    val album: Album,
    val tracks: List<Track>
)
