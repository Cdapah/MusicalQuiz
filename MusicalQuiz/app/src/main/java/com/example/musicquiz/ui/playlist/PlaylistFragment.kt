package com.example.musicquiz.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicquiz.databinding.FragmentPlaylistBinding
import com.example.musicquiz.model.Playlist
import com.example.musicquiz.ui.adapters.PlaylistAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlaylistFragment : Fragment() {

    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PlaylistViewModel
    private lateinit var adapter: PlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[PlaylistViewModel::class.java]

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                // Navigate to playlist details or show tracks in this playlist
                showPlaylistTracksDialog(playlist)
            },
            onOptionsClick = { playlist, view ->
                showPlaylistOptionsMenu(playlist, view)
            }
        )

        binding.playlistsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PlaylistFragment.adapter
        }
    }

    private fun setupListeners() {
        binding.addPlaylistButton.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            adapter.submitList(playlists)
            binding.emptyPlaylistsView.visibility = if (playlists.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.resetOperationSuccess()
            }
        }
    }

    private fun showCreatePlaylistDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Playlist Name"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create New Playlist")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val playlistName = editText.text.toString().trim()
                if (playlistName.isNotEmpty()) {
                    viewModel.createPlaylist(playlistName)
                } else {
                    Toast.makeText(requireContext(), "Playlist name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPlaylistOptionsMenu(playlist: Playlist, view: View) {
        val options = arrayOf("Rename", "Delete")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Playlist Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenamePlaylistDialog(playlist)
                    1 -> showDeletePlaylistConfirmation(playlist)
                }
            }
            .show()
    }

    private fun showRenamePlaylistDialog(playlist: Playlist) {
        val editText = EditText(requireContext()).apply {
            setText(playlist.name)
            hint = "Playlist Name"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename Playlist")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.updatePlaylist(playlist.copy(name = newName))
                } else {
                    Toast.makeText(requireContext(), "Playlist name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeletePlaylistConfirmation(playlist: Playlist) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Playlist")
            .setMessage("Are you sure you want to delete '${playlist.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePlaylist(playlist.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPlaylistTracksDialog(playlist: Playlist) {
        // In a real app, we would navigate to a playlist details screen
        // For now, we'll just show a placeholder dialog
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(playlist.name)
            .setMessage("This playlist has ${playlist.trackCount} tracks.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
