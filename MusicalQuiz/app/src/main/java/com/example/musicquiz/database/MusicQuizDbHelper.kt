package com.example.musicquiz.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite database helper for the Music Quiz app
 */
class MusicQuizDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "musicquiz.db"
        const val DATABASE_VERSION = 1

        // Table names
        const val TABLE_PLAYLISTS = "playlists"
        const val TABLE_TRACKS = "tracks"
        const val TABLE_PLAYLIST_TRACKS = "playlist_tracks"
        const val TABLE_QUIZZES = "quizzes"
        const val TABLE_QUIZ_QUESTIONS = "quiz_questions"
        const val TABLE_QUIZ_ANSWERS = "quiz_answers"

        // Common column names
        const val COLUMN_ID = "id"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"

        // Playlist table columns
        const val COLUMN_PLAYLIST_NAME = "name"

        // Track table columns
        const val COLUMN_TRACK_DEEZER_ID = "deezer_id"
        const val COLUMN_TRACK_TITLE = "title"
        const val COLUMN_TRACK_ARTIST = "artist"
        const val COLUMN_TRACK_ALBUM = "album"
        const val COLUMN_TRACK_DURATION = "duration"
        const val COLUMN_TRACK_PREVIEW_URL = "preview_url"
        const val COLUMN_TRACK_COVER_URL = "cover_url"

        // Playlist tracks table columns
        const val COLUMN_PLAYLIST_ID = "playlist_id"
        const val COLUMN_TRACK_ID = "track_id"
        const val COLUMN_TRACK_ORDER = "track_order"

        // Quiz table columns
        const val COLUMN_QUIZ_NAME = "name"
        const val COLUMN_QUIZ_PLAYLIST_ID = "playlist_id"
        const val COLUMN_QUIZ_MODE = "mode"
        const val COLUMN_QUIZ_TIME_LIMIT = "time_limit"
        const val COLUMN_USE_RANDOM_QUESTIONS = "use_random_questions"

        // Quiz questions table columns
        const val COLUMN_QUIZ_ID = "quiz_id"
        const val COLUMN_QUESTION_TRACK_ID = "track_id"
        const val COLUMN_QUESTION_ORDER = "question_order"

        // Quiz answers table columns
        const val COLUMN_QUESTION_ID = "question_id"
        const val COLUMN_ANSWER_TEXT = "answer_text"
        const val COLUMN_IS_CORRECT = "is_correct"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create playlists table
        db.execSQL("""
            CREATE TABLE $TABLE_PLAYLISTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PLAYLIST_NAME TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL
            )
        """.trimIndent())

        // Create tracks table
        db.execSQL("""
            CREATE TABLE $TABLE_TRACKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TRACK_DEEZER_ID INTEGER NOT NULL,
                $COLUMN_TRACK_TITLE TEXT NOT NULL,
                $COLUMN_TRACK_ARTIST TEXT NOT NULL,
                $COLUMN_TRACK_ALBUM TEXT NOT NULL,
                $COLUMN_TRACK_DURATION INTEGER NOT NULL,
                $COLUMN_TRACK_PREVIEW_URL TEXT,
                $COLUMN_TRACK_COVER_URL TEXT,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent())

        // Create playlist_tracks table (junction table)
        db.execSQL("""
            CREATE TABLE $TABLE_PLAYLIST_TRACKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PLAYLIST_ID INTEGER NOT NULL,
                $COLUMN_TRACK_ID INTEGER NOT NULL,
                $COLUMN_TRACK_ORDER INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_PLAYLIST_ID) REFERENCES $TABLE_PLAYLISTS ($COLUMN_ID) ON DELETE CASCADE,
                FOREIGN KEY ($COLUMN_TRACK_ID) REFERENCES $TABLE_TRACKS ($COLUMN_ID) ON DELETE CASCADE,
                UNIQUE ($COLUMN_PLAYLIST_ID, $COLUMN_TRACK_ID)
            )
        """.trimIndent())

        // Create quizzes table
        db.execSQL("""
            CREATE TABLE $TABLE_QUIZZES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_QUIZ_NAME TEXT NOT NULL,
                $COLUMN_QUIZ_PLAYLIST_ID INTEGER NOT NULL,
                $COLUMN_QUIZ_MODE TEXT NOT NULL,
                $COLUMN_QUIZ_TIME_LIMIT INTEGER,
                $COLUMN_USE_RANDOM_QUESTIONS INTEGER NOT NULL DEFAULT 0,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_QUIZ_PLAYLIST_ID) REFERENCES $TABLE_PLAYLISTS($COLUMN_ID)
            )
        """.trimIndent())

        // Create quiz_questions table
        db.execSQL("""
            CREATE TABLE $TABLE_QUIZ_QUESTIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_QUIZ_ID INTEGER NOT NULL,
                $COLUMN_QUESTION_TRACK_ID INTEGER NOT NULL,
                $COLUMN_QUESTION_ORDER INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_QUIZ_ID) REFERENCES $TABLE_QUIZZES ($COLUMN_ID) ON DELETE CASCADE,
                FOREIGN KEY ($COLUMN_QUESTION_TRACK_ID) REFERENCES $TABLE_TRACKS ($COLUMN_ID) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create quiz_answers table
        db.execSQL("""
            CREATE TABLE $TABLE_QUIZ_ANSWERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_QUESTION_ID INTEGER NOT NULL,
                $COLUMN_ANSWER_TEXT TEXT NOT NULL,
                $COLUMN_IS_CORRECT INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_QUESTION_ID) REFERENCES $TABLE_QUIZ_QUESTIONS ($COLUMN_ID) ON DELETE CASCADE
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades here
        if (oldVersion < newVersion) {
            // For simplicity in this version, we'll drop and recreate tables
            db.execSQL("DROP TABLE IF EXISTS $TABLE_QUIZ_ANSWERS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_QUIZ_QUESTIONS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_QUIZZES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLIST_TRACKS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_TRACKS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLISTS")
            onCreate(db)
        }
    }
}
