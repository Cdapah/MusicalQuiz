package com.example.musicquiz.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicquiz.R
import com.example.musicquiz.model.Track
import androidx.core.content.ContextCompat

class AlbumTrackAdapter(
    private val tracks: List<Track>,
    private val onTrackClick: (Track) -> Unit,
    private val onPlayClick: (Track) -> Unit,
    private val onPauseClick: (Track) -> Unit,
    private val formatDuration: (Int) -> String
) : RecyclerView.Adapter<AlbumTrackAdapter.TrackViewHolder>() {

    private var currentlyPlayingPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_album_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]
        holder.bind(track, position + 1, position == currentlyPlayingPosition)
    }

    override fun getItemCount(): Int = tracks.size

    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val trackNumber: TextView = itemView.findViewById(R.id.track_number)
        private val trackTitle: TextView = itemView.findViewById(R.id.track_title)
        private val trackDuration: TextView = itemView.findViewById(R.id.track_duration)
        private val playButton: ImageButton = itemView.findViewById(R.id.play_button)

        fun bind(track: Track, number: Int, isPlaying: Boolean) {
            trackNumber.text = number.toString()
            trackTitle.text = track.title
            trackDuration.text = formatDuration(track.duration)

            // Set up click listeners
            itemView.setOnClickListener { onTrackClick(track) }

            // Update play button icon based on playing state
            if (isPlaying) {
                playButton.setImageResource(android.R.drawable.ic_media_pause)
                playButton.setOnClickListener {
                    onPauseClick(track)
                    updatePlayingState(-1)
                }
            } else {
                playButton.setImageResource(android.R.drawable.ic_media_play)
                playButton.setOnClickListener {
                    onPlayClick(track)
                    updatePlayingState(adapterPosition)
                }
            }

            // Disable play button if no preview URL is available
            playButton.isEnabled = track.previewUrl != null
            playButton.alpha = if (track.previewUrl != null) 1.0f else 0.5f
        }
    }

    fun updatePlayingState(position: Int) {
        val oldPosition = currentlyPlayingPosition
        currentlyPlayingPosition = position

        // Update both the old and new positions
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition)
        }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }
}
