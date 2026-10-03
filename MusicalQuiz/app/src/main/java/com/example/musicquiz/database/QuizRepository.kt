package com.example.musicquiz.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.musicquiz.model.Quiz
import com.example.musicquiz.model.QuizMode
import com.example.musicquiz.model.QuizQuestion
import com.example.musicquiz.model.QuizAnswer
import com.example.musicquiz.model.LocalTrack

/**
 * Repository for handling quiz-related database operations
 */
class QuizRepository(private val context: Context) {
    private val dbHelper = MusicQuizDbHelper(context)
    private val trackRepository = TrackRepository(context)

    /**
     * Create a new quiz
     */
    fun createQuiz(quiz: Quiz): Long {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(MusicQuizDbHelper.COLUMN_QUIZ_NAME, quiz.name)
            put(MusicQuizDbHelper.COLUMN_QUIZ_PLAYLIST_ID, quiz.playlistId)
            put(MusicQuizDbHelper.COLUMN_QUIZ_MODE, quiz.mode.name)
            put(MusicQuizDbHelper.COLUMN_QUIZ_TIME_LIMIT, quiz.timeLimit)
            // Default to 0 (false) for random questions
            put(MusicQuizDbHelper.COLUMN_USE_RANDOM_QUESTIONS, 0)
            put(MusicQuizDbHelper.COLUMN_CREATED_AT, quiz.createdAt)
            put(MusicQuizDbHelper.COLUMN_UPDATED_AT, quiz.updatedAt)
        }

        return db.insert(MusicQuizDbHelper.TABLE_QUIZZES, null, values)
    }

    /**
     * Get all quizzes with playlist name and question count
     */
    fun getAllQuizzes(): List<Quiz> {
        val quizzes = mutableListOf<Quiz>()
        val db = dbHelper.readableDatabase

        // Query to get quizzes with playlist name and question count
        val query = """
            SELECT q.${MusicQuizDbHelper.COLUMN_ID}, 
                   q.${MusicQuizDbHelper.COLUMN_QUIZ_NAME}, 
                   q.${MusicQuizDbHelper.COLUMN_QUIZ_PLAYLIST_ID}, 
                   q.${MusicQuizDbHelper.COLUMN_QUIZ_MODE}, 
                   q.${MusicQuizDbHelper.COLUMN_QUIZ_TIME_LIMIT}, 
                   q.${MusicQuizDbHelper.COLUMN_USE_RANDOM_QUESTIONS},
                   q.${MusicQuizDbHelper.COLUMN_CREATED_AT}, 
                   q.${MusicQuizDbHelper.COLUMN_UPDATED_AT},
                   p.${MusicQuizDbHelper.COLUMN_PLAYLIST_NAME},
                   COUNT(qq.${MusicQuizDbHelper.COLUMN_ID}) AS question_count
            FROM ${MusicQuizDbHelper.TABLE_QUIZZES} q
            JOIN ${MusicQuizDbHelper.TABLE_PLAYLISTS} p 
            ON q.${MusicQuizDbHelper.COLUMN_QUIZ_PLAYLIST_ID} = p.${MusicQuizDbHelper.COLUMN_ID}
            LEFT JOIN ${MusicQuizDbHelper.TABLE_QUIZ_QUESTIONS} qq 
            ON q.${MusicQuizDbHelper.COLUMN_ID} = qq.${MusicQuizDbHelper.COLUMN_QUIZ_ID}
            GROUP BY q.${MusicQuizDbHelper.COLUMN_ID}
            ORDER BY q.${MusicQuizDbHelper.COLUMN_UPDATED_AT} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                quizzes.add(cursorToQuiz(it))
            }
        }

        return quizzes
    }

    /**
     * Get a quiz by ID with playlist name and question count
     */
    fun getQuizById(quizId: Long): Quiz? {
        val db = dbHelper.readableDatabase

        // Query to get quiz with playlist name and question count
        val query = """
            SELECT q.${MusicQuizDbHelper.COLUMN_ID}, 
                   q.${MusicQuizDbHelper.COLUMN_QUIZ_NAME}, 
                   q.${MusicQuizDbHelper.COLUMN_QUIZ_PLAYLIST_ID}, 
                   q.${MusicQuizDbHelper.COLUMN_QUIZ_MODE}, 
                   q.${MusicQuizDbHelper.COLUMN_QUIZ_TIME_LIMIT}, 
                   q.${MusicQuizDbHelper.COLUMN_USE_RANDOM_QUESTIONS},
                   q.${MusicQuizDbHelper.COLUMN_CREATED_AT}, 
                   q.${MusicQuizDbHelper.COLUMN_UPDATED_AT},
                   p.${MusicQuizDbHelper.COLUMN_PLAYLIST_NAME},
                   COUNT(qq.${MusicQuizDbHelper.COLUMN_ID}) AS question_count
            FROM ${MusicQuizDbHelper.TABLE_QUIZZES} q
            JOIN ${MusicQuizDbHelper.TABLE_PLAYLISTS} p 
            ON q.${MusicQuizDbHelper.COLUMN_QUIZ_PLAYLIST_ID} = p.${MusicQuizDbHelper.COLUMN_ID}
            LEFT JOIN ${MusicQuizDbHelper.TABLE_QUIZ_QUESTIONS} qq 
            ON q.${MusicQuizDbHelper.COLUMN_ID} = qq.${MusicQuizDbHelper.COLUMN_QUIZ_ID}
            WHERE q.${MusicQuizDbHelper.COLUMN_ID} = ?
            GROUP BY q.${MusicQuizDbHelper.COLUMN_ID}
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(quizId.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                return cursorToQuiz(it)
            }
        }

        return null
    }

    /**
     * Update a quiz
     */
    fun updateQuiz(quiz: Quiz): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MusicQuizDbHelper.COLUMN_QUIZ_NAME, quiz.name)
            put(MusicQuizDbHelper.COLUMN_QUIZ_PLAYLIST_ID, quiz.playlistId)
            put(MusicQuizDbHelper.COLUMN_QUIZ_MODE, quiz.mode.name)
            put(MusicQuizDbHelper.COLUMN_QUIZ_TIME_LIMIT, quiz.timeLimit)
            // Default to 0 (false) for random questions
            put(MusicQuizDbHelper.COLUMN_USE_RANDOM_QUESTIONS, 0)
            put(MusicQuizDbHelper.COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        return db.update(
            MusicQuizDbHelper.TABLE_QUIZZES,
            values,
            "${MusicQuizDbHelper.COLUMN_ID} = ?",
            arrayOf(quiz.id.toString())
        )
    }

    /**
     * Delete a quiz
     */
    fun deleteQuiz(quizId: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            MusicQuizDbHelper.TABLE_QUIZZES,
            "${MusicQuizDbHelper.COLUMN_ID} = ?",
            arrayOf(quizId.toString())
        )
    }

    /**
     * Add a question to a quiz
     */
    fun addQuestionToQuiz(question: QuizQuestion): Long {
        val db = dbHelper.writableDatabase

        // Get the highest order number for this quiz
        val orderQuery = """
            SELECT MAX(${MusicQuizDbHelper.COLUMN_QUESTION_ORDER}) 
            FROM ${MusicQuizDbHelper.TABLE_QUIZ_QUESTIONS}
            WHERE ${MusicQuizDbHelper.COLUMN_QUIZ_ID} = ?
        """.trimIndent()

        val cursor = db.rawQuery(orderQuery, arrayOf(question.quizId.toString()))
        val nextOrder = cursor.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                it.getInt(0) + 1
            } else {
                0
            }
        }

        val values = ContentValues().apply {
            put(MusicQuizDbHelper.COLUMN_QUIZ_ID, question.quizId)
            put(MusicQuizDbHelper.COLUMN_QUESTION_TRACK_ID, question.trackId)
            put(MusicQuizDbHelper.COLUMN_QUESTION_ORDER, nextOrder)
        }

        // Update the quiz's updated_at timestamp
        val quizValues = ContentValues().apply {
            put(MusicQuizDbHelper.COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        db.update(
            MusicQuizDbHelper.TABLE_QUIZZES,
            quizValues,
            "${MusicQuizDbHelper.COLUMN_ID} = ?",
            arrayOf(question.quizId.toString())
        )

        return db.insert(MusicQuizDbHelper.TABLE_QUIZ_QUESTIONS, null, values)
    }

    /**
     * Add multiple questions to a quiz at once
     * @return The number of questions successfully added
     */
    fun addQuestionsToQuiz(questions: List<QuizQuestion>): Int {
        var addedCount = 0

        for (question in questions) {
            val id = addQuestionToQuiz(question)
            if (id > 0) {
                addedCount++
            }
        }

        return addedCount
    }

    /**
     * Add an answer to a question
     */
    fun addAnswerToQuestion(answer: QuizAnswer): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MusicQuizDbHelper.COLUMN_QUESTION_ID, answer.questionId)
            put(MusicQuizDbHelper.COLUMN_ANSWER_TEXT, answer.answerText)
            put(MusicQuizDbHelper.COLUMN_IS_CORRECT, if (answer.isCorrect) 1 else 0)
        }
        return db.insert(MusicQuizDbHelper.TABLE_QUIZ_ANSWERS, null, values)
    }

    /**
     * Get all questions for a quiz
     */
    fun getQuestionsForQuiz(quizId: Long): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()
        val db = dbHelper.readableDatabase

        // Query to get questions with their tracks
        val query = """
            SELECT qq.* 
            FROM ${MusicQuizDbHelper.TABLE_QUIZ_QUESTIONS} qq
            WHERE qq.${MusicQuizDbHelper.COLUMN_QUIZ_ID} = ?
            ORDER BY qq.${MusicQuizDbHelper.COLUMN_QUESTION_ORDER}
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(quizId.toString()))
        cursor.use {
            while (it.moveToNext()) {
                val questionId = it.getLong(it.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_ID))
                val trackId = it.getLong(it.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_QUESTION_TRACK_ID))
                val order = it.getInt(it.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_QUESTION_ORDER))

                // Get the track for this question
                val track = trackRepository.getTrackById(trackId)

                // Get answers for this question
                val answers = getAnswersForQuestion(questionId)

                // Extract correct answer and wrong answers for multiple choice
                val correctAnswer = answers.find { it.isCorrect }?.answerText ?: ""
                val wrongAnswers = answers.filter { !it.isCorrect }.map { it.answerText }

                questions.add(
                    QuizQuestion(
                        id = questionId,
                        quizId = quizId,
                        trackId = trackId,
                        order = order,
                        track = track,
                        answers = answers,
                        correctAnswer = correctAnswer,
                        wrongAnswers = wrongAnswers
                    )
                )
            }
        }

        return questions
    }

    /**
     * Get all answers for a question
     */
    private fun getAnswersForQuestion(questionId: Long): List<QuizAnswer> {
        val answers = mutableListOf<QuizAnswer>()
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            MusicQuizDbHelper.TABLE_QUIZ_ANSWERS,
            null,
            "${MusicQuizDbHelper.COLUMN_QUESTION_ID} = ?",
            arrayOf(questionId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_ID))
                val answerText = it.getString(it.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_ANSWER_TEXT))
                val isCorrect = it.getInt(it.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_IS_CORRECT)) == 1

                answers.add(
                    QuizAnswer(
                        id = id,
                        questionId = questionId,
                        answerText = answerText,
                        isCorrect = isCorrect
                    )
                )
            }
        }

        return answers
    }

    /**
     * Generate questions for a quiz from a playlist
     * This will create questions for all tracks in the playlist
     */
    fun generateQuestionsFromPlaylist(quizId: Long, playlistId: Long): Int {
        val playlistRepository = PlaylistRepository(context)
        val tracks = playlistRepository.getTracksInPlaylist(playlistId)
        var questionsAdded = 0

        for (track in tracks) {
            val question = QuizQuestion(
                quizId = quizId,
                trackId = track.id,
                order = 0 // Will be set in addQuestionToQuiz
            )
            val questionId = addQuestionToQuiz(question)

            if (questionId > 0) {
                questionsAdded++
            }
        }

        return questionsAdded
    }

    /**
     * Generate multiple-choice answers for a question
     * This will create one correct answer and three incorrect answers
     */
    fun generateMultipleChoiceAnswers(questionId: Long, correctTrack: LocalTrack, allTracks: List<LocalTrack>): Int {
        // Add the correct answer
        val correctAnswer = QuizAnswer(
            questionId = questionId,
            answerText = correctTrack.title,
            isCorrect = true
        )
        addAnswerToQuestion(correctAnswer)

        // Get other tracks to use as incorrect answers
        val incorrectTracks = allTracks
            .filter { it.id != correctTrack.id }
            .shuffled()
            .take(3)

        // Add incorrect answers
        var answersAdded = 1 // We already added the correct answer
        for (track in incorrectTracks) {
            val incorrectAnswer = QuizAnswer(
                questionId = questionId,
                answerText = track.title,
                isCorrect = false
            )
            val answerId = addAnswerToQuestion(incorrectAnswer)

            if (answerId > 0) {
                answersAdded++
            }
        }

        return answersAdded
    }

    /**
     * Convert a cursor to a Quiz object
     */
    private fun cursorToQuiz(cursor: Cursor): Quiz {
        // Get the playlist name and question count from the query
        val playlistNameIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_PLAYLIST_NAME)
        val questionCountIndex = cursor.getColumnIndex("question_count")

        return Quiz(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_QUIZ_NAME)),
            playlistId = cursor.getLong(cursor.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_QUIZ_PLAYLIST_ID)),
            mode = QuizMode.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_QUIZ_MODE))),
            timeLimit = if (cursor.isNull(cursor.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_QUIZ_TIME_LIMIT))) null
            else cursor.getInt(cursor.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_QUIZ_TIME_LIMIT)),
            useRandomQuestions = cursor.getInt(cursor.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_USE_RANDOM_QUESTIONS)) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_UPDATED_AT)),
            playlistName = if (playlistNameIndex >= 0) cursor.getString(playlistNameIndex) else "",
            questionCount = if (questionCountIndex >= 0) cursor.getInt(questionCountIndex) else 0
        )
    }
}
