package com.example.musicquiz.ui.quiz

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.musicquiz.R
import com.example.musicquiz.databinding.FragmentQuizPlayBinding
import com.example.musicquiz.model.QuizMode
import com.example.musicquiz.model.QuizQuestion
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class QuizPlayFragment : Fragment() {

    private var _binding: FragmentQuizPlayBinding? = null
    private val binding get() = _binding!!

    private val args: QuizPlayFragmentArgs by navArgs()
    private lateinit var viewModel: QuizViewModel

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizPlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[QuizViewModel::class.java]

        setupListeners()

        // Load quiz and start
        val args = QuizPlayFragmentArgs.fromBundle(requireArguments())
        viewModel.loadQuiz(args.quizId) { quiz ->
            if (quiz != null) {
                viewModel.startQuiz(quiz.id, quiz.useRandomQuestions)
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.currentQuestion.observe(viewLifecycleOwner) { question ->
            question?.let {
                displayQuestion(it)
            }
        }

        viewModel.timeLeft.observe(viewLifecycleOwner) { timeLeft ->
            timeLeft?.let {
                binding.timerText.text = "${it}s"
            }
        }

        viewModel.currentQuestionIndex.observe(viewLifecycleOwner) { index ->
            val totalQuestions = viewModel.quizQuestions.value?.size ?: 0
            binding.questionCounter.text = "Question ${index + 1}/$totalQuestions"
        }

        viewModel.score.observe(viewLifecycleOwner) { score ->
            // We'll use this when showing the final score
        }

        viewModel.currentQuiz.observe(viewLifecycleOwner) { quiz ->
            // Add null check to prevent NullPointerException
            quiz?.let {
                binding.quizNameText.text = it.name

                // Set up timer if applicable
                it.timeLimit?.let { timeLimit ->
                    binding.timerText.visibility = View.VISIBLE
                    startTimer(timeLimit)
                } ?: run {
                    binding.timerText.visibility = View.GONE
                }
            }
        }

        viewModel.quizFinished.observe(viewLifecycleOwner) { finished ->
            if (finished) {
                showQuizResults()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupListeners() {
        binding.playButton.setOnClickListener {
            val question = viewModel.currentQuestion.value ?: return@setOnClickListener
            val previewUrl = question.track?.previewUrl

            if (!previewUrl.isNullOrEmpty()) {
                if (isPlaying) {
                    stopPreview()
                } else {
                    playPreview(previewUrl)
                }
            } else {
                Toast.makeText(requireContext(), "No preview available for this track", Toast.LENGTH_SHORT).show()
            }
        }

        binding.submitAnswerButton.setOnClickListener {
            submitAnswer()
        }
    }

    private fun displayQuestion(question: QuizQuestion) {
        // Reset UI
        stopPreview()
        binding.answerInput.text?.clear()
        binding.answersRadioGroup.clearCheck()

        // Load track image
        question.track?.coverUrl?.let { coverUrl ->
            Glide.with(binding.trackImage)
                .load(coverUrl)
                .centerCrop()
                .into(binding.trackImage)
        }

        // Set up answers for multiple choice
        val quiz = viewModel.currentQuiz.value
        if (quiz?.mode == QuizMode.MULTIPLE_CHOICE) {
            binding.answersRadioGroup.visibility = View.VISIBLE
            binding.answerInputLayout.visibility = View.GONE

            // Get answers and shuffle if using random questions
            val answers = mutableListOf(question.correctAnswer)
            answers.addAll(question.wrongAnswers)
            if (quiz.useRandomQuestions) {
                answers.shuffle()
            }

            // Set up radio buttons
            val radioButtons = listOf(
                binding.root.findViewById<RadioButton>(R.id.answer_1),
                binding.root.findViewById<RadioButton>(R.id.answer_2),
                binding.root.findViewById<RadioButton>(R.id.answer_3),
                binding.root.findViewById<RadioButton>(R.id.answer_4)
            )

            for (i in answers.indices) {
                if (i < radioButtons.size) {
                    radioButtons[i].text = answers[i]
                }
            }
        } else {
            binding.answersRadioGroup.visibility = View.GONE
            binding.answerInputLayout.visibility = View.VISIBLE
        }
    }

    private fun playPreview(url: String) {
        try {
            // Check if the URL is valid
            if (!url.startsWith("http")) {
                // Try to refresh the track data and get a new preview URL
                refreshCurrentTrackPreview()
                return
            }

            // Release any existing MediaPlayer instance first
            stopPreview()

            // Show loading state
            binding.playButton.isEnabled = false
            binding.playButton.text = "Loading..."

            // Create a new MediaPlayer instance
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                // Set a timeout for connection
                val timeout = 10000 // 10 seconds
                setOnPreparedListener { mp ->
                    mp.start()
                    this@QuizPlayFragment.isPlaying = true
                    binding.playButton.isEnabled = true
                    binding.playButton.text = "Stop Preview"
                    binding.playButton.icon = resources.getDrawable(android.R.drawable.ic_media_pause, null)
                }
                setOnCompletionListener { _ ->
                    this@QuizPlayFragment.isPlaying = false
                    binding.playButton.isEnabled = true
                    binding.playButton.text = "Play Preview"
                    binding.playButton.icon = resources.getDrawable(android.R.drawable.ic_media_play, null)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("QuizPlayFragment", "MediaPlayer error: what=$what, extra=$extra, url=$url")
                    this@QuizPlayFragment.isPlaying = false
                    binding.playButton.isEnabled = true
                    binding.playButton.text = "Play Preview"
                    binding.playButton.icon = resources.getDrawable(android.R.drawable.ic_media_play, null)

                    // Try to refresh the track data if there's a network error
                    if (what == MediaPlayer.MEDIA_ERROR_IO || what == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                        refreshCurrentTrackPreview()
                        return@setOnErrorListener true
                    }

                    // Show a more specific error message based on the error code
                    val errorMessage = when(what) {
                        MediaPlayer.MEDIA_ERROR_IO -> "Network error while playing preview"
                        MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "Connection timed out"
                        MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "Audio format not supported"
                        MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "Media server error"
                        else -> "Error playing preview"
                    }

                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                try {
                    setDataSource(url)
                    prepareAsync()
                } catch (e: Exception) {
                    Log.e("QuizPlayFragment", "Error setting data source", e)
                    // Try to refresh the track data if there's an exception
                    refreshCurrentTrackPreview()
                    throw e
                }
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("QuizPlayFragment", "Error playing preview", e)
            binding.playButton.isEnabled = true
            binding.playButton.text = "Play Preview"
            binding.playButton.icon = resources.getDrawable(android.R.drawable.ic_media_play, null)

            if (isAdded && context != null) {
                Toast.makeText(requireContext(), "Error playing preview: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Refresh the current track's preview URL by fetching fresh data from Deezer API
     */
    private fun refreshCurrentTrackPreview() {
        val currentQuestion = viewModel.currentQuestion.value ?: return
        val track = currentQuestion.track ?: return

        if (track.deezerId <= 0) {
            if (isAdded && context != null) {
                Toast.makeText(requireContext(), "Cannot refresh track preview", Toast.LENGTH_SHORT).show()
            }
            return
        }

        binding.playButton.isEnabled = false
        binding.playButton.text = "Refreshing..."

        viewModel.refreshTrackPreview(track.deezerId) { refreshedTrack ->
            if (isAdded && context != null) {
                binding.playButton.isEnabled = true
                binding.playButton.text = "Play Preview"
                binding.playButton.icon = resources.getDrawable(android.R.drawable.ic_media_play, null)

                if (refreshedTrack != null && !refreshedTrack.previewUrl.isNullOrEmpty()) {
                    // Play the preview with the refreshed URL
                    playPreview(refreshedTrack.previewUrl)
                } else {
                    Toast.makeText(requireContext(), "Could not refresh preview URL", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun stopPreview() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
            mediaPlayer = null
            isPlaying = false
            binding.playButton.isEnabled = true
            binding.playButton.text = "Play Preview"
            binding.playButton.icon = resources.getDrawable(android.R.drawable.ic_media_play, null)
        } catch (e: Exception) {
            Log.e("QuizPlayFragment", "Error stopping preview", e)
            // Make sure button is always re-enabled
            binding.playButton.isEnabled = true
            binding.playButton.text = "Play Preview"
            binding.playButton.icon = resources.getDrawable(android.R.drawable.ic_media_play, null)
        }
    }

    private fun submitAnswer() {
        val quiz = viewModel.currentQuiz.value ?: return
        var answer = ""

        when (quiz.mode) {
            QuizMode.MULTIPLE_CHOICE -> {
                val selectedRadioButtonId = binding.answersRadioGroup.checkedRadioButtonId
                if (selectedRadioButtonId == -1) {
                    Toast.makeText(requireContext(), "Please select an answer", Toast.LENGTH_SHORT).show()
                    return
                }

                val selectedRadioButton = binding.root.findViewById<RadioButton>(selectedRadioButtonId)
                answer = selectedRadioButton.text.toString()
            }
            QuizMode.OPEN_ENDED -> {
                answer = binding.answerInput.text.toString().trim()
                if (answer.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter an answer", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            else -> {
                Toast.makeText(requireContext(), "Unknown quiz mode", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val isCorrect = viewModel.submitAnswer(answer)
        showAnswerFeedback(isCorrect)
    }

    private fun showAnswerFeedback(isCorrect: Boolean) {
        val message = if (isCorrect) "Correct!" else "Incorrect!"
        val currentQuestion = viewModel.currentQuestion.value
        val correctAnswer = currentQuestion?.track?.title ?: ""

        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle(message)
            .setMessage(if (isCorrect) "Well done!" else "The correct answer was: $correctAnswer")
            .setPositiveButton("Next") { _, _ ->
                viewModel.nextQuestion()
            }
            .setCancelable(false)
            .show()
    }

    private fun startTimer(seconds: Int) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(seconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                binding.timerText.text = "${secondsLeft}s"
                viewModel.updateTimeLeft(secondsLeft.toInt())
            }

            override fun onFinish() {
                binding.timerText.text = "0s"
                // Auto-submit with empty answer (will be marked incorrect)
                val isCorrect = viewModel.submitAnswer("")
                showAnswerFeedback(isCorrect)
            }
        }.start()
    }

    private fun showQuizResults() {
        val score = viewModel.score.value ?: 0
        val totalQuestions = viewModel.quizQuestions.value?.size ?: 0
        val percentage = if (totalQuestions > 0) (score * 100) / totalQuestions else 0

        // Create a custom message with emoji based on score
        val emoji = when {
            percentage >= 90 -> "🏆"
            percentage >= 70 -> "🎉"
            percentage >= 50 -> "👍"
            else -> "😊"
        }

        val message = "$emoji Your score: $score out of $totalQuestions ($percentage%) $emoji"

        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Quiz Completed!")
            .setMessage(message)
            .setPositiveButton("Finish") { _, _ ->
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    override fun onPause() {
        super.onPause()
        stopPreview()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPreview()
        countDownTimer?.cancel()
        // Reset the quiz state when navigating away
        viewModel.resetQuizState()
        _binding = null
    }
}
