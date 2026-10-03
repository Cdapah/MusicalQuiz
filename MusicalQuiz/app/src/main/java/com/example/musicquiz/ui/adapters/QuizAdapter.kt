package com.example.musicquiz.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicquiz.databinding.ItemQuizBinding
import com.example.musicquiz.model.Quiz

/**
 * Adapter for displaying quizzes
 */
class QuizAdapter(
    private val onQuizClick: (Quiz) -> Unit,
    private val onPlayClick: (Quiz) -> Unit,
    private val onOptionsClick: (Quiz, View) -> Unit
) : ListAdapter<Quiz, QuizAdapter.ViewHolder>(QuizDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuizBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val quiz = getItem(position)
        holder.bind(quiz)
    }

    inner class ViewHolder(private val binding: ItemQuizBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onQuizClick(getItem(position))
                }
            }

            binding.playButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPlayClick(getItem(position))
                }
            }

            binding.optionsButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onOptionsClick(getItem(position), it)
                }
            }
        }

        fun bind(quiz: Quiz) {
            binding.quizName.text = quiz.name

            // Format details text with playlist name and question count
            val details = StringBuilder()
            details.append("Playlist: ${quiz.playlistName}")

            if (quiz.questionCount > 0) {
                details.append(" • ${quiz.questionCount} questions")
            }

            // Add quiz mode
            when (quiz.mode) {
                com.example.musicquiz.model.QuizMode.MULTIPLE_CHOICE -> {
                    details.append(" • Multiple Choice")
                    binding.quizModeBadge.text = "Multiple Choice"
                }
                com.example.musicquiz.model.QuizMode.OPEN_ENDED -> {
                    details.append(" • Open Ended")
                    binding.quizModeBadge.text = "Open Ended"
                }
                else -> {
                    binding.quizModeBadge.visibility = View.GONE
                }
            }

            binding.quizDetails.text = details.toString()
        }
    }

    class QuizDiffCallback : DiffUtil.ItemCallback<Quiz>() {
        override fun areItemsTheSame(oldItem: Quiz, newItem: Quiz): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Quiz, newItem: Quiz): Boolean {
            return oldItem == newItem
        }
    }
}
