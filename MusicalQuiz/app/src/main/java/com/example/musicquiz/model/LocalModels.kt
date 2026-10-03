package com.example.musicquiz.model

/**
 * Represents a playlist in the local database
 */
data class Playlist(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val trackCount: Int = 0 // This is not stored in the database but calculated when needed
)

/**
 * Represents a track in the local database
 */
data class LocalTrack(
    val id: Long = 0,
    val deezerId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val previewUrl: String?,
    val coverUrl: String?,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Represents a playlist-track relationship in the local database
 */
data class PlaylistTrack(
    val id: Long = 0,
    val playlistId: Long,
    val trackId: Long,
    val order: Int
)

/**
 * Represents a quiz in the local database
 */
data class Quiz(
    val id: Long = 0,
    val name: String,
    val playlistId: Long,
    val mode: QuizMode,
    val timeLimit: Int?, // Null means no time limit
    val useRandomQuestions: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // These fields are not stored in the database but calculated when needed
    val playlistName: String = "",
    val questionCount: Int = 0
)

/**
 * Represents a quiz question in the local database
 */
data class QuizQuestion(
    val id: Long = 0,
    val quizId: Long,
    val trackId: Long,
    val order: Int,
    // These fields are not stored in the database but joined when needed
    val track: LocalTrack? = null,
    val answers: List<QuizAnswer> = emptyList(),
    // For easier handling of correct and wrong answers
    val correctAnswer: String = "",
    val wrongAnswers: List<String> = emptyList()
)

/**
 * Represents a quiz answer in the local database
 */
data class QuizAnswer(
    val id: Long = 0,
    val questionId: Long,
    val answerText: String,
    val isCorrect: Boolean
)

/**
 * Enum representing the different quiz modes
 */
enum class QuizMode {
    MULTIPLE_CHOICE,
    FILL_IN_BLANKS,
    OPEN_ENDED;

    companion object {
        fun fromString(mode: String): QuizMode {
            return when (mode.uppercase()) {
                "MULTIPLE_CHOICE" -> MULTIPLE_CHOICE
                "FILL_IN_BLANKS" -> FILL_IN_BLANKS
                "OPEN_ENDED" -> OPEN_ENDED
                else -> MULTIPLE_CHOICE // Default
            }
        }
    }
}
