package com.example.musicquiz.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicquiz.databinding.ItemSearchResultBinding
import com.example.musicquiz.model.Album
import com.example.musicquiz.model.Track

/**
 * Adapter for displaying search results (tracks or albums)
 */
class SearchResultAdapter(
    private val onItemClick: (Any) -> Unit,
    private val onItemLongClick: (Any) -> Boolean
) : ListAdapter<Any, SearchResultAdapter.ViewHolder>(SearchResultDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    return@setOnLongClickListener onItemLongClick(getItem(position))
                }
                false
            }
        }

        fun bind(item: Any) {
            when (item) {
                is Track -> bindTrack(item)
                is Album -> bindAlbum(item)
            }
        }

        private fun bindTrack(track: Track) {
            binding.itemTitle.text = track.title
            binding.itemArtist.text = track.artist.name
            binding.itemAlbum.text = track.album.title
            binding.itemTypeBadge.text = "TRACK"

            // Load album cover image
            Glide.with(binding.itemImage)
                .load(track.album.coverMedium)
                .centerCrop()
                .into(binding.itemImage)
        }

        private fun bindAlbum(album: Album) {
            binding.itemTitle.text = album.title
            binding.itemArtist.text = album.artist?.name ?: "Various Artists"

            // Format release date if available
            val releaseDate = album.releaseDate
            binding.itemAlbum.text = if (!releaseDate.isNullOrEmpty()) {
                "Released: $releaseDate"
            } else {
                ""
            }

            binding.itemTypeBadge.text = "ALBUM"

            // Load album cover image
            Glide.with(binding.itemImage)
                .load(album.coverMedium)
                .centerCrop()
                .into(binding.itemImage)
        }
    }

    class SearchResultDiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Track && newItem is Track -> oldItem.id == newItem.id
                oldItem is Album && newItem is Album -> oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Track && newItem is Track -> oldItem == newItem
                oldItem is Album && newItem is Album -> oldItem == newItem
                else -> false
            }
        }
    }
}
