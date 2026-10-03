package com.example.musicquiz.ui.details

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.musicquiz.R
import com.example.musicquiz.database.PlaylistRepository
import com.example.musicquiz.databinding.FragmentDetailsBinding
import com.example.musicquiz.model.Album
import com.example.musicquiz.model.Playlist
import com.example.musicquiz.model.Track
import com.example.musicquiz.ui.adapters.AlbumTrackAdapter
import com.example.musicquiz.ui.search.AlbumWithTracks
import com.example.musicquiz.ui.search.SearchViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: DetailsFragmentArgs by navArgs()
    private lateinit var viewModel: SearchViewModel

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentAdapter: AlbumTrackAdapter? = null
    private var currentTrack: Track? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        setupObservers()
        loadDetails()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.addToPlaylistSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Added to playlist", Toast.LENGTH_SHORT).show()
                viewModel.resetAddToPlaylistSuccess()
            }
        }
    }

    private fun loadDetails() {
        binding.detailsProgressBar.visibility = View.VISIBLE

        when (args.itemType) {
            "track" -> loadTrackDetails(args.itemId.toLong())
            "album" -> loadAlbumDetails(args.itemId.toLong())
            else -> {
                binding.detailsProgressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Unknown item type", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTrackDetails(trackId: Long) {
        viewModel.getTrackDetails(trackId) { track ->
            binding.detailsProgressBar.visibility = View.GONE

            if (track != null) {
                displayTrackDetails(track)
            } else {
                Toast.makeText(requireContext(), "Failed to load track details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAlbumDetails(albumId: Long) {
        viewModel.getAlbumDetails(albumId) { albumWithTracks ->
            binding.detailsProgressBar.visibility = View.GONE

            if (albumWithTracks != null) {
                displayAlbumDetails(albumWithTracks)
            } else {
                Toast.makeText(requireContext(), "Failed to load album details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayTrackDetails(track: Track) {
        binding.detailsTitle.text = track.title
        binding.detailsArtist.text = track.artist.name
        binding.detailsAlbum.text = track.album.title

        val additionalInfo = "Duration: ${formatDuration(track.duration)}"
        binding.detailsAdditionalInfo.text = additionalInfo

        // Load album cover image
        Glide.with(binding.detailsImage)
            .load(track.album.coverMedium)
            .centerCrop()
            .into(binding.detailsImage)

        // Show or hide preview button based on preview URL availability
        binding.playPreviewButton.visibility = if (track.previewUrl != null) View.VISIBLE else View.GONE

        // Set up preview button
        binding.playPreviewButton.setOnClickListener {
            track.previewUrl?.let { url ->
                if (isPlaying) {
                    stopPreview()
                } else {
                    playPreview(url)
                }
            }
        }

        // Set up add to playlist button
        binding.addToPlaylistButton.setOnClickListener {
            showAddToPlaylistDialog(track)
        }
    }

    private fun displayAlbumDetails(albumWithTracks: AlbumWithTracks) {
        val album = albumWithTracks.album
        val tracks = albumWithTracks.tracks

        binding.detailsTitle.text = album.title
        binding.detailsArtist.text = album.artist?.name ?: ""
        binding.detailsAlbum.text = album.releaseDate ?: ""

        // Set basic album info
        val additionalInfo = "Tracks: ${tracks.size}"
        binding.detailsAdditionalInfo.text = additionalInfo

        // Load album cover image
        Glide.with(binding.detailsImage)
            .load(album.coverMedium)
            .centerCrop()
            .into(binding.detailsImage)

        // Set up the RecyclerView for tracks
        binding.tracksRecyclerView.apply {
            visibility = View.VISIBLE
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

            val trackAdapter = AlbumTrackAdapter(
                tracks = tracks,
                onTrackClick = { track ->
                    // Show track details or other action when track is clicked
                    Toast.makeText(context, "Selected: ${track.title}", Toast.LENGTH_SHORT).show()
                },
                onPlayClick = { track ->
                    // Play the track preview
                    track.previewUrl?.let { url ->
                        playPreview(url, track)
                    } ?: run {
                        Toast.makeText(context, "No preview available for this track", Toast.LENGTH_SHORT).show()
                    }
                },
                onPauseClick = { track ->
                    // Stop the track preview
                    stopPreview()
                },
                formatDuration = this@DetailsFragment::formatDuration
            )

            adapter = trackAdapter
            currentAdapter = trackAdapter
        }

        // Hide preview button for albums since we have play buttons for each track
        binding.playPreviewButton.visibility = View.GONE

        // Set up add to playlist button
        binding.addToPlaylistButton.setOnClickListener {
            showAddTracksToPlaylistDialog(tracks)
        }
    }

    private fun formatDuration(durationInSeconds: Int): String {
        val minutes = durationInSeconds / 60
        val seconds = durationInSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    private fun playPreview(url: String, track: Track? = null) {
        // Stop any currently playing preview
        stopPreview()

        // Set the current track
        currentTrack = track

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener {
                    it.start()
                    this@DetailsFragment.isPlaying = true
                    this@DetailsFragment.binding.playPreviewButton.text = "Stop Preview"
                }
                setOnCompletionListener {
                    this@DetailsFragment.isPlaying = false
                    this@DetailsFragment.binding.playPreviewButton.text = "Play Preview"
                    // Reset the adapter's playing state when playback completes
                    this@DetailsFragment.currentAdapter?.updatePlayingState(-1)
                    this@DetailsFragment.currentTrack = null
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error playing preview: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPreview() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            this@DetailsFragment.mediaPlayer = null
            this@DetailsFragment.isPlaying = false
            this@DetailsFragment.binding.playPreviewButton.text = "Play Preview"

            // Reset the adapter's playing state
            this@DetailsFragment.currentAdapter?.updatePlayingState(-1)
            this@DetailsFragment.currentTrack = null
        }
    }

    private fun setupListeners() {
        // Set up back button to navigate back to search screen
        binding.backButton.setOnClickListener {
            // Navigate back to the previous screen
            requireActivity().onBackPressed()
        }
    }

    private fun showAddToPlaylistDialog(track: Track) {
        val playlistRepository = PlaylistRepository(requireContext())
        val playlists = playlistRepository.getAllPlaylists()

        if (playlists.isEmpty()) {
            showCreatePlaylistDialog(track)
            return
        }

        val playlistNames = playlists.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add to Playlist")
            .setItems(playlistNames) { _, which ->
                val selectedPlaylist = playlists[which]
                viewModel.addTrackToPlaylist(track, selectedPlaylist.id)
            }
            .setPositiveButton("New Playlist") { _, _ ->
                showCreatePlaylistDialog(track)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddTracksToPlaylistDialog(tracks: List<Track>) {
        val playlistRepository = PlaylistRepository(requireContext())
        val playlists = playlistRepository.getAllPlaylists()

        if (playlists.isEmpty()) {
            showCreatePlaylistDialog(tracks)
            return
        }

        val playlistNames = playlists.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add ${tracks.size} Tracks to Playlist")
            .setItems(playlistNames) { _, which ->
                val selectedPlaylist = playlists[which]
                // Add each track to the selected playlist
                tracks.forEach { track ->
                    viewModel.addTrackToPlaylist(track, selectedPlaylist.id)
                }
                Toast.makeText(requireContext(), "${tracks.size} tracks added to ${selectedPlaylist.name}", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("New Playlist") { _, _ ->
                showCreatePlaylistDialog(tracks)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreatePlaylistDialog(track: Track? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_playlist, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.playlist_name_input)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create New Playlist")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val playlistName = nameInput.text.toString().trim()
                if (playlistName.isNotEmpty()) {
                    val playlist = Playlist(
                        id = 0, // Will be set by the database
                        name = playlistName,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        trackCount = 0
                    )
                    viewModel.createPlaylist(playlist, track)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreatePlaylistDialog(tracks: List<Track>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_playlist, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.playlist_name_input)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create New Playlist")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val playlistName = nameInput.text.toString().trim()
                if (playlistName.isNotEmpty()) {
                    val playlist = Playlist(
                        id = 0, // Will be set by the database
                        name = playlistName,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        trackCount = 0
                    )
                    // Create the playlist
                    val playlistRepository = PlaylistRepository(requireContext())
                    val playlistId = playlistRepository.createPlaylist(playlist)

                    // Add each track to the playlist
                    tracks.forEach { track ->
                        viewModel.addTrackToPlaylist(track, playlistId)
                    }

                    Toast.makeText(requireContext(), "Created playlist and added ${tracks.size} tracks", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPreview()
        _binding = null
    }
}
