package com.example.musicquiz.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicquiz.databinding.ItemPlaylistBinding
import com.example.musicquiz.model.Playlist

/**
 * Adapter for displaying playlists
 */
class PlaylistAdapter(
    private val onPlaylistClick: (Playlist) -> Unit,
    private val onOptionsClick: (Playlist, View) -> Unit
) : ListAdapter<Playlist, PlaylistAdapter.ViewHolder>(PlaylistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = getItem(position)
        holder.bind(playlist)
    }

    inner class ViewHolder(private val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPlaylistClick(getItem(position))
                }
            }

            binding.playlistOptions.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onOptionsClick(getItem(position), it)
                }
            }

//            binding.createQuizButton.setOnClickListener {
//                val position = bindingAdapterPosition
//                if (position != RecyclerView.NO_POSITION) {
//                    // Handle create quiz click - for now, just open the playlist
//                    onPlaylistClick(getItem(position))
//                }
//            }
        }

        fun bind(playlist: Playlist) {
            binding.playlistName.text = playlist.name

            // Format track count text
            val trackText = if (playlist.trackCount == 1) {
                "1 track"
            } else {
                "${playlist.trackCount} tracks"
            }
            binding.playlistTracksCount.text = trackText

            // Only show create quiz button if playlist has tracks
            //  binding.createQuizButton.visibility = if (playlist.trackCount > 0) View.VISIBLE else View.GONE
        }
    }

    class PlaylistDiffCallback : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem == newItem
        }
    }
}
