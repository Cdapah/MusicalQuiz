package com.example.musicquiz.ui.quiz

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.musicquiz.api.DeezerApiClient
import com.example.musicquiz.database.PlaylistRepository
import com.example.musicquiz.database.QuizRepository
import com.example.musicquiz.database.TrackRepository
import com.example.musicquiz.model.*
import com.example.musicquiz.util.PlaylistUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val quizRepository = QuizRepository(application)
    private val playlistRepository = PlaylistRepository(application)
    private val trackRepository = TrackRepository(application)
    // DeezerApiClient is a singleton object, no need to instantiate

    private val _quizzes = MutableLiveData<List<Quiz>>()
    val quizzes: LiveData<List<Quiz>> = _quizzes

    private val _currentQuiz = MutableLiveData<Quiz>()
    val currentQuiz: LiveData<Quiz> = _currentQuiz

    private val _quizQuestions = MutableLiveData<List<QuizQuestion>>()
    val quizQuestions: LiveData<List<QuizQuestion>> = _quizQuestions

    private val _playlists = MutableLiveData<List<Playlist>>()
    val playlists: LiveData<List<Playlist>> = _playlists

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> = _operationSuccess

    // For quiz play
    private val _currentQuestion = MutableLiveData<QuizQuestion>()
    val currentQuestion: LiveData<QuizQuestion> = _currentQuestion

    private val _currentQuestionIndex = MutableLiveData<Int>()
    val currentQuestionIndex: LiveData<Int> = _currentQuestionIndex

    private val _score = MutableLiveData<Int>()
    val score: LiveData<Int> = _score

    private val _timeLeft = MutableLiveData<Int>()
    val timeLeft: LiveData<Int> = _timeLeft

    private val _quizFinished = MutableLiveData<Boolean>()
    val quizFinished: LiveData<Boolean> = _quizFinished

    init {
        loadQuizzes()
        loadPlaylists()

        // Observe playlist updates from other parts of the app
        PlaylistUpdateManager.playlistUpdated.observeForever { updated ->
            if (updated) {
                loadPlaylists()
                PlaylistUpdateManager.resetUpdateFlag()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Remove the observer when the ViewModel is cleared to prevent memory leaks
        PlaylistUpdateManager.playlistUpdated.removeObserver { }
    }

    /**
     * Load all quizzes from the database
     */
    fun loadQuizzes() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    quizRepository.getAllQuizzes()
                }
                _quizzes.value = result
            } catch (e: Exception) {
                _error.value = "Error loading quizzes: ${e.message}"
            }
        }
    }

    /**
     * Load all playlists from the database
     */
    fun loadPlaylists() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    playlistRepository.getAllPlaylists()
                }
                _playlists.value = result
            } catch (e: Exception) {
                _error.value = "Error loading playlists: ${e.message}"
            }
        }
    }

    /**
     * Create a new quiz
     */
    fun createQuiz(name: String, playlistId: Long, mode: QuizMode, timeLimit: Int?, useRandomQuestions: Boolean = false) {
        viewModelScope.launch {
            try {
                // Check if the playlist has tracks
                val tracks = withContext(Dispatchers.IO) {
                    playlistRepository.getTracksInPlaylist(playlistId)
                }

                if (tracks.isEmpty()) {
                    _error.value = "Playlist has no tracks. Please add tracks to the playlist first."
                    _operationSuccess.value = false
                    return@launch
                }

                // Create the quiz
                val quiz = Quiz(
                    name = name,
                    playlistId = playlistId,
                    mode = mode,
                    timeLimit = timeLimit,
                    useRandomQuestions = useRandomQuestions
                )

                val quizId = withContext(Dispatchers.IO) {
                    quizRepository.createQuiz(quiz)
                }

                if (quizId > 0) {
                    // Get tracks to use for questions
                    val questionTracks = if (useRandomQuestions) {
                        tracks.shuffled().take(10)
                    } else {
                        tracks.take(10)
                    }

                    // Generate questions
                    val questions = questionTracks.mapIndexed { index, track ->
                        QuizQuestion(
                            id = 0, // Will be set by database
                            quizId = quizId,
                            trackId = track.id,
                            order = index,
                            track = track
                        )
                    }

                    val questionsAdded = withContext(Dispatchers.IO) {
                        quizRepository.addQuestionsToQuiz(questions)
                    }

                    if (questionsAdded > 0) {
                        // If it's a multiple-choice quiz, generate answers for each question
                        if (mode == QuizMode.MULTIPLE_CHOICE) {
                            val savedQuestions = withContext(Dispatchers.IO) {
                                quizRepository.getQuestionsForQuiz(quizId)
                            }

                            val allTracks = withContext(Dispatchers.IO) {
                                trackRepository.getAllTracks()
                            }

                            for (question in savedQuestions) {
                                val correctTrack = question.track
                                if (correctTrack != null) {
                                    withContext(Dispatchers.IO) {
                                        quizRepository.generateMultipleChoiceAnswers(
                                            question.id,
                                            correctTrack,
                                            if (useRandomQuestions) allTracks.shuffled() else allTracks
                                        )
                                    }
                                }
                            }
                        }

                        _operationSuccess.value = true
                        // Refresh the quiz list
                        loadQuizzes()
                    } else {
                        _error.value = "Failed to generate questions for the quiz."
                        _operationSuccess.value = false
                    }
                } else {
                    _error.value = "Failed to create quiz."
                    _operationSuccess.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error creating quiz: ${e.message}"
                _operationSuccess.value = false
            }
        }
    }

    /**
     * Delete a quiz
     */
    fun deleteQuiz(quizId: Long) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    quizRepository.deleteQuiz(quizId)
                }

                if (result > 0) {
                    _operationSuccess.value = true
                    loadQuizzes()
                } else {
                    _error.value = "Failed to delete quiz"
                    _operationSuccess.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error deleting quiz: ${e.message}"
                _operationSuccess.value = false
            }
        }
    }

    /**
     * Load a quiz by ID
     */
    fun loadQuiz(quizId: Long) {
        viewModelScope.launch {
            try {
                val quiz = withContext(Dispatchers.IO) {
                    quizRepository.getQuizById(quizId)
                }

                if (quiz != null) {
                    _currentQuiz.value = quiz
                    loadQuizQuestions(quizId)
                } else {
                    _error.value = "Quiz not found"
                }
            } catch (e: Exception) {
                _error.value = "Error loading quiz: ${e.message}"
            }
        }
    }

    /**
     * Load a quiz by ID and return it via callback
     */
    fun loadQuiz(quizId: Long, callback: (Quiz?) -> Unit) {
        viewModelScope.launch {
            try {
                val quiz = withContext(Dispatchers.IO) {
                    quizRepository.getQuizById(quizId)
                }

                if (quiz != null) {
                    _currentQuiz.value = quiz
                    callback(quiz)
                } else {
                    _error.value = "Quiz not found"
                    callback(null)
                }
            } catch (e: Exception) {
                _error.value = "Error loading quiz: ${e.message}"
                callback(null)
            }
        }
    }

    /**
     * Load questions for a quiz
     */
    fun loadQuizQuestions(quizId: Long) {
        viewModelScope.launch {
            try {
                val questions = withContext(Dispatchers.IO) {
                    quizRepository.getQuestionsForQuiz(quizId)
                }
                _quizQuestions.value = questions
            } catch (e: Exception) {
                _error.value = "Error loading quiz questions: ${e.message}"
            }
        }
    }

    /**
     * Start playing a quiz
     */
    fun startQuiz(quizId: Long, useRandomQuestions: Boolean = false) {
        viewModelScope.launch {
            try {
                // Load the quiz
                val quiz = withContext(Dispatchers.IO) {
                    quizRepository.getQuizById(quizId)
                }

                if (quiz != null) {
                    // Create a new quiz object with the useRandomQuestions parameter if needed
                    val updatedQuiz = if (quiz.useRandomQuestions != useRandomQuestions) {
                        quiz.copy(useRandomQuestions = useRandomQuestions)
                    } else {
                        quiz
                    }
                    _currentQuiz.value = updatedQuiz

                    // Load questions
                    val questions = withContext(Dispatchers.IO) {
                        quizRepository.getQuestionsForQuiz(quizId)
                    }

                    if (questions.isNotEmpty()) {
                        // If random questions are enabled, shuffle the questions
                        val finalQuestions = if (useRandomQuestions) {
                            questions.shuffled()
                        } else {
                            questions
                        }

                        _quizQuestions.value = finalQuestions
                        _currentQuestionIndex.value = 0
                        _currentQuestion.value = finalQuestions[0]
                        _score.value = 0
                        _quizFinished.value = false

                        // Set time limit if applicable
                        if (quiz.timeLimit != null) {
                            _timeLeft.value = quiz.timeLimit
                        }
                    } else {
                        _error.value = "Quiz has no questions"
                    }
                } else {
                    _error.value = "Quiz not found"
                }
            } catch (e: Exception) {
                _error.value = "Error starting quiz: ${e.message}"
            }
        }
    }

    /**
     * Move to the next question in the quiz
     */
    fun nextQuestion() {
        val currentIndex = _currentQuestionIndex.value ?: 0
        val questions = _quizQuestions.value ?: return

        if (currentIndex < questions.size - 1) {
            _currentQuestionIndex.value = currentIndex + 1
            _currentQuestion.value = questions[currentIndex + 1]
        } else {
            // Quiz is finished
            _quizFinished.value = true
        }
    }

    /**
     * Submit an answer for the current question
     */
    fun submitAnswer(answer: String): Boolean {
        val currentQuestion = _currentQuestion.value ?: return false

        // For multiple choice, check if the answer matches the correct answer
        if (currentQuiz.value?.mode == QuizMode.MULTIPLE_CHOICE) {
            val correctAnswer = currentQuestion.correctAnswer
            val isCorrect = correctAnswer.equals(answer, ignoreCase = true)

            if (isCorrect) {
                _score.value = (_score.value ?: 0) + 1
            }

            return isCorrect
        }
        // For fill in the blanks or open-ended, check if the answer contains the track title
        else {
            val trackTitle = currentQuestion.track?.title ?: return false
            val isCorrect = answer.contains(trackTitle, ignoreCase = true)

            if (isCorrect) {
                _score.value = (_score.value ?: 0) + 1
            }

            return isCorrect
        }
    }

    /**
     * Refresh a track's data from the Deezer API to get a fresh preview URL
     * @param deezerId The Deezer ID of the track to refresh
     * @param callback Callback with the refreshed track or null if failed
     */
    fun refreshTrackPreview(deezerId: Long, callback: (LocalTrack?) -> Unit) {
        viewModelScope.launch {
            try {
                // Fetch fresh track data from Deezer API
                val response = withContext(Dispatchers.IO) {
                    DeezerApiClient.apiService.getTrack(deezerId)
                }

                if (response.isSuccessful && response.body() != null) {
                    val trackResponse = response.body()!!

                    // Update the track in the database with fresh data
                    val updatedTrack = LocalTrack(
                        deezerId = trackResponse.id,
                        title = trackResponse.title,
                        artist = trackResponse.artist.name,
                        album = trackResponse.album.title,
                        duration = trackResponse.duration,
                        previewUrl = trackResponse.previewUrl,
                        coverUrl = trackResponse.album.coverMedium
                    )

                    // Get the existing track to preserve its ID
                    val existingTrack = withContext(Dispatchers.IO) {
                        trackRepository.getTrackByDeezerId(deezerId)
                    }

                    if (existingTrack != null) {
                        // Update the existing track with the new preview URL
                        val trackToUpdate = updatedTrack.copy(id = existingTrack.id)

                        val updated = withContext(Dispatchers.IO) {
                            trackRepository.updateTrack(trackToUpdate)
                        }

                        if (updated) {
                            // Get the updated track from the database
                            val refreshedTrack = withContext(Dispatchers.IO) {
                                trackRepository.getTrackById(existingTrack.id)
                            }

                            // Refresh the current question with the updated track
                            _currentQuestion.value?.let { question ->
                                if (question.track?.id == refreshedTrack?.id) {
                                    _currentQuestion.value = question.copy(track = refreshedTrack)
                                }
                            }

                            callback(refreshedTrack)
                            return@launch
                        }
                    }
                } else {
                    Log.e("QuizViewModel", "API error: ${response.code()} - ${response.message()}")
                }

                // If we get here, something went wrong
                callback(null)

            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error refreshing track preview", e)
                callback(null)
            }
        }
    }

    /**
     * Update time left for timed quizzes
     */
    fun updateTimeLeft(seconds: Int) {
        _timeLeft.value = seconds
    }

    /**
     * Reset the quiz state when navigating away from the quiz play screen
     * This ensures that when returning to the quiz tab, the quiz list is shown
     * instead of continuing the previous quiz
     */
    fun resetQuizState() {
        _currentQuiz.value = null
        _quizQuestions.value = emptyList()
        _currentQuestion.value = null
        _currentQuestionIndex.value = 0
        _score.value = 0
        _timeLeft.value = null
    }

    /**
     * Reset operation success flag
     */
    fun resetOperationSuccess() {
        _operationSuccess.value = false
    }
}
