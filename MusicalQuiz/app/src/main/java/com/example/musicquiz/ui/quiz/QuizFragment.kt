package com.example.musicquiz.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicquiz.R
import com.example.musicquiz.databinding.FragmentQuizBinding
import com.example.musicquiz.model.Playlist
import com.example.musicquiz.model.Quiz
import com.example.musicquiz.model.QuizMode
import com.example.musicquiz.ui.adapters.QuizAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: QuizViewModel
    private lateinit var adapter: QuizAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[QuizViewModel::class.java]

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = QuizAdapter(
            onQuizClick = { quiz ->
                // Show quiz details
                showQuizDetailsDialog(quiz)
            },
            onPlayClick = { quiz ->
                // Navigate to quiz play screen
                val action = QuizFragmentDirections.actionQuizFragmentToQuizPlayFragment(quiz.id)
                findNavController().navigate(action)
            },
            onOptionsClick = { quiz, view ->
                showQuizOptionsMenu(quiz, view)
            }
        )

        binding.quizzesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@QuizFragment.adapter
        }
    }

    private fun setupListeners() {
        binding.addQuizButton.setOnClickListener {
            showCreateQuizDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.quizzes.observe(viewLifecycleOwner) { quizzes ->
            adapter.submitList(quizzes)
            binding.emptyQuizzesView.visibility = if (quizzes.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.playlists.observe(viewLifecycleOwner) {
            // We'll use this when creating a new quiz
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

    private fun showCreateQuizDialog() {
        // Reload playlists to ensure we have the latest data
        viewModel.loadPlaylists()

        // Check if playlists are already loaded
        val currentPlaylists = viewModel.playlists.value
        if (currentPlaylists != null && currentPlaylists.isNotEmpty()) {
            // If playlists are already loaded, show the dialog immediately
            continueShowCreateQuizDialog(currentPlaylists)
        } else {
            // Show a loading indicator
            val loadingDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Loading")
                .setMessage("Loading playlists...")
                .setCancelable(false)
                .create()
            loadingDialog.show()

            // Create a one-time observer to wait for playlists to load
            viewModel.playlists.observe(viewLifecycleOwner, object : androidx.lifecycle.Observer<List<Playlist>> {
                override fun onChanged(playlists: List<Playlist>) {
                    // Remove this observer to avoid multiple callbacks
                    viewModel.playlists.removeObserver(this)

                    // Dismiss the loading dialog
                    loadingDialog.dismiss()

                    if (playlists.isEmpty()) {
                        Toast.makeText(requireContext(), "Please create a playlist first", Toast.LENGTH_SHORT).show()
                    } else {
                        continueShowCreateQuizDialog(playlists)
                    }
                }
            })
        }
    }

    private fun continueShowCreateQuizDialog(playlists: List<Playlist>) {

        // Inflate the quiz settings dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_quiz_settings, null)

        // Get references to views
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.quiz_name_input)
        val playlistDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.playlist_dropdown)
        val randomQuestionsSwitch = dialogView.findViewById<SwitchMaterial>(R.id.random_questions_switch)
        val timeLimitSwitch = dialogView.findViewById<SwitchMaterial>(R.id.time_limit_switch)
        val timeLimitLayout = dialogView.findViewById<TextInputLayout>(R.id.time_limit_layout)
        val timeLimitInput = dialogView.findViewById<TextInputEditText>(R.id.time_limit_input)
        val quizModeGroup = dialogView.findViewById<RadioGroup>(R.id.quiz_mode_group)

        // Setup playlist dropdown
        val playlistNames = playlists.map { it.name }
        val playlistAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, playlistNames)
        playlistDropdown.setAdapter(playlistAdapter)
        playlistDropdown.setText(playlistNames.firstOrNull() ?: "", false)

        // Setup time limit switch listener
        timeLimitSwitch.setOnCheckedChangeListener { _, isChecked ->
            timeLimitLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Create and show the dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create New Quiz")
            .setView(dialogView)
            .setPositiveButton("Create", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Set click listener for the positive button to prevent dialog from dismissing on validation errors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Quiz name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedPlaylistName = playlistDropdown.text.toString()
            val selectedPlaylist = playlists.find { it.name == selectedPlaylistName }

            if (selectedPlaylist == null) {
                Toast.makeText(requireContext(), "Please select a playlist", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get selected quiz mode
            val mode = when (quizModeGroup.checkedRadioButtonId) {
                R.id.mode_multiple_choice -> QuizMode.MULTIPLE_CHOICE
                R.id.mode_open_ended -> QuizMode.OPEN_ENDED
                else -> QuizMode.MULTIPLE_CHOICE
            }

            // Get time limit if enabled
            val timeLimit = if (timeLimitSwitch.isChecked) {
                val timeLimitValue = timeLimitInput.text.toString().toIntOrNull()
                if (timeLimitValue == null || timeLimitValue <= 0) {
                    Toast.makeText(requireContext(), "Please enter a valid time limit", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                timeLimitValue
            } else null

            // Create the quiz
            viewModel.createQuiz(
                name = name,
                playlistId = selectedPlaylist.id,
                mode = mode,
                timeLimit = timeLimit,
                useRandomQuestions = randomQuestionsSwitch.isChecked
            )

            // Dismiss the dialog after successful creation
            dialog.dismiss()
        }
    }

    private fun showQuizOptionsMenu(quiz: Quiz, view: View) {
        val options = arrayOf("Delete")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Quiz Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showDeleteQuizConfirmation(quiz)
                }
            }
            .show()
    }

    private fun showDeleteQuizConfirmation(quiz: Quiz) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Quiz")
            .setMessage("Are you sure you want to delete '${quiz.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteQuiz(quiz.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showQuizDetailsDialog(quiz: Quiz) {
        val message = """
            Name: ${quiz.name}
            Playlist: ${quiz.playlistName}
            Mode: ${quiz.mode}
            Questions: ${quiz.questionCount}
            Time Limit: ${quiz.timeLimit ?: "None"}
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Quiz Details")
            .setMessage(message)
            .setPositiveButton("Play") { _, _ ->
                val action = QuizFragmentDirections.actionQuizFragmentToQuizPlayFragment(quiz.id)
                findNavController().navigate(action)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
