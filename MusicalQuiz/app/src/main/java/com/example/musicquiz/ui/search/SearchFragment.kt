package com.example.musicquiz.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.musicquiz.R
import com.example.musicquiz.database.PlaylistRepository
import com.example.musicquiz.databinding.FragmentSearchBinding
import com.example.musicquiz.model.Album
import com.example.musicquiz.model.Playlist
import com.example.musicquiz.model.Track
import com.example.musicquiz.ui.adapters.SearchResultAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: SearchResultAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = SearchResultAdapter(
            onItemClick = { item ->
                when (item) {
                    is Track -> navigateToDetails("track", item.id.toString())
                    is Album -> navigateToDetails("album", item.id.toString())
                }
            },
            onItemLongClick = { item ->
                when (item) {
                    is Track -> showAddToPlaylistDialog(item)
                    is Album -> showAlbumTracksDialog(item)
                }
                true
            }
        )

        binding.searchResultsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 1)
            adapter = this@SearchFragment.adapter
        }
    }

    private fun setupListeners() {
        // Search button click
        binding.searchButton.setOnClickListener {
            performSearch()
        }

        // Search on keyboard action
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val query = binding.searchEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            val radioTracks = view?.findViewById<RadioButton>(R.id.radio_tracks)
            val searchType = if (radioTracks != null && radioTracks.isChecked) {
                SearchType.TRACK
            } else {
                SearchType.ALBUM
            }
            viewModel.search(query, searchType)
        } else {
            Toast.makeText(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
            view?.findViewById<TextView>(R.id.empty_view)?.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            view?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

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

    private fun navigateToDetails(itemType: String, itemId: String) {
        val action = SearchFragmentDirections.actionSearchFragmentToDetailsFragment(itemId, itemType)
        findNavController().navigate(action)
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

    private fun showAlbumTracksDialog(album: Album) {
        // In a real app, we would fetch album tracks from the API
        // For now, we'll just show a placeholder dialog
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Album Tracks")
            .setMessage("Feature coming soon: View tracks in '${album.title}'")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
