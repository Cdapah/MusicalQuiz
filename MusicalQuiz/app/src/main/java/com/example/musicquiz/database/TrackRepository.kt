package com.example.musicquiz.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.musicquiz.model.LocalTrack
import com.example.musicquiz.model.Track

/**
 * Repository for handling track-related database operations
 */
class TrackRepository {
    private val context: Context
    private val dbHelper: MusicQuizDbHelper

    constructor(context: Context) {
        this.context = context
        this.dbHelper = MusicQuizDbHelper(context)
    }

    constructor(context: Context, dbHelper: MusicQuizDbHelper) {
        this.context = context
        this.dbHelper = dbHelper
    }

    /**
     * Save a track to the local database
     */
    fun saveTrack(track: LocalTrack): Long {
        val db = dbHelper.writableDatabase

        // Check if track already exists by Deezer ID
        val cursor = db.query(
            MusicQuizDbHelper.TABLE_TRACKS,
            arrayOf(MusicQuizDbHelper.COLUMN_ID),
            "${MusicQuizDbHelper.COLUMN_TRACK_DEEZER_ID} = ?",
            arrayOf(track.deezerId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                // Track already exists, return its ID
                return it.getLong(it.getColumnIndexOrThrow(MusicQuizDbHelper.COLUMN_ID))
            }
        }

        // Track doesn't exist, insert it
        val values = ContentValues().apply {
            put(MusicQuizDbHelper.COLUMN_TRACK_DEEZER_ID, track.deezerId)
            put(MusicQuizDbHelper.COLUMN_TRACK_TITLE, track.title)
            put(MusicQuizDbHelper.COLUMN_TRACK_ARTIST, track.artist)
            put(MusicQuizDbHelper.COLUMN_TRACK_ALBUM, track.album)
            put(MusicQuizDbHelper.COLUMN_TRACK_DURATION, track.duration)
            put(MusicQuizDbHelper.COLUMN_TRACK_PREVIEW_URL, track.previewUrl)
            put(MusicQuizDbHelper.COLUMN_TRACK_COVER_URL, track.coverUrl)
            put(MusicQuizDbHelper.COLUMN_CREATED_AT, track.createdAt)
        }

        return db.insert(MusicQuizDbHelper.TABLE_TRACKS, null, values)
    }

    /**
     * Convert a Deezer Track to a LocalTrack
     */
    fun convertDeezerTrackToLocal(track: Track): LocalTrack {
        return LocalTrack(
            deezerId = track.id,
            title = track.title,
            artist = track.artist.name,
            album = track.album.title,
            duration = track.duration,
            previewUrl = track.previewUrl,
            coverUrl = track.album.coverMedium
        )
    }

    /**
     * Get a track by ID
     */
    fun getTrackById(trackId: Long): LocalTrack? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MusicQuizDbHelper.TABLE_TRACKS,
            null,
            "${MusicQuizDbHelper.COLUMN_ID} = ?",
            arrayOf(trackId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToTrack(it)
            }
        }

        return null
    }

    /**
     * Get a track by Deezer ID
     */
    fun getTrackByDeezerId(deezerId: Long): LocalTrack? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MusicQuizDbHelper.TABLE_TRACKS,
            null,
            "${MusicQuizDbHelper.COLUMN_TRACK_DEEZER_ID} = ?",
            arrayOf(deezerId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToTrack(it)
            }
        }

        return null
    }

    /**
     * Get all tracks in the database
     */
    fun getAllTracks(): List<LocalTrack> {
        val tracks = mutableListOf<LocalTrack>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MusicQuizDbHelper.TABLE_TRACKS,
            null,
            null,
            null,
            null,
            null,
            "${MusicQuizDbHelper.COLUMN_CREATED_AT} DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                tracks.add(cursorToTrack(it))
            }
        }

        return tracks
    }

    /**
     * Convert a cursor to a LocalTrack object
     */
    fun cursorToTrack(cursor: Cursor): LocalTrack {
        val idIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_ID)
        val deezerIdIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_TRACK_DEEZER_ID)
        val titleIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_TRACK_TITLE)
        val artistIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_TRACK_ARTIST)
        val albumIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_TRACK_ALBUM)
        val durationIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_TRACK_DURATION)
        val previewUrlIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_TRACK_PREVIEW_URL)
        val coverUrlIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_TRACK_COVER_URL)
        val createdAtIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_CREATED_AT)

        return LocalTrack(
            id = cursor.getLong(idIndex),
            deezerId = cursor.getLong(deezerIdIndex),
            title = cursor.getString(titleIndex),
            artist = cursor.getString(artistIndex),
            album = cursor.getString(albumIndex),
            duration = cursor.getInt(durationIndex),
            previewUrl = if (cursor.isNull(previewUrlIndex)) null else cursor.getString(previewUrlIndex),
            coverUrl = if (cursor.isNull(coverUrlIndex)) null else cursor.getString(coverUrlIndex),
            createdAt = cursor.getLong(createdAtIndex)
        )
    }

    /**
     * Update a track in the database
     * @param track The track to update
     * @return true if the update was successful, false otherwise
     */
    fun updateTrack(track: LocalTrack): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MusicQuizDbHelper.COLUMN_TRACK_DEEZER_ID, track.deezerId)
            put(MusicQuizDbHelper.COLUMN_TRACK_TITLE, track.title)
            put(MusicQuizDbHelper.COLUMN_TRACK_ARTIST, track.artist)
            put(MusicQuizDbHelper.COLUMN_TRACK_ALBUM, track.album)
            put(MusicQuizDbHelper.COLUMN_TRACK_DURATION, track.duration)
            put(MusicQuizDbHelper.COLUMN_TRACK_PREVIEW_URL, track.previewUrl)
            put(MusicQuizDbHelper.COLUMN_TRACK_COVER_URL, track.coverUrl)
        }

        val rowsAffected = db.update(
            MusicQuizDbHelper.TABLE_TRACKS,
            values,
            "${MusicQuizDbHelper.COLUMN_ID} = ?",
            arrayOf(track.id.toString())
        )

        return rowsAffected > 0
    }
}
