package com.example.musicquiz.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.musicquiz.model.LocalTrack
import com.example.musicquiz.model.Playlist
import com.example.musicquiz.model.PlaylistTrack

/**
 * Repository for handling playlist-related database operations
 */
class PlaylistRepository(private val context: Context) {
    private val dbHelper = MusicQuizDbHelper(context)

    /**
     * Create a new playlist
     */
    fun createPlaylist(playlist: Playlist): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MusicQuizDbHelper.COLUMN_PLAYLIST_NAME, playlist.name)
            put(MusicQuizDbHelper.COLUMN_CREATED_AT, playlist.createdAt)
            put(MusicQuizDbHelper.COLUMN_UPDATED_AT, playlist.updatedAt)
        }
        return db.insert(MusicQuizDbHelper.TABLE_PLAYLISTS, null, values)
    }

    /**
     * Get all playlists with track count
     */
    fun getAllPlaylists(): List<Playlist> {
        val playlists = mutableListOf<Playlist>()
        val db = dbHelper.readableDatabase

        // Query to get playlists with track count
        val query = """
            SELECT p.${MusicQuizDbHelper.COLUMN_ID}, 
                   p.${MusicQuizDbHelper.COLUMN_PLAYLIST_NAME}, 
                   p.${MusicQuizDbHelper.COLUMN_CREATED_AT}, 
                   p.${MusicQuizDbHelper.COLUMN_UPDATED_AT}, 
                   COUNT(pt.${MusicQuizDbHelper.COLUMN_TRACK_ID}) AS track_count
            FROM ${MusicQuizDbHelper.TABLE_PLAYLISTS} p
            LEFT JOIN ${MusicQuizDbHelper.TABLE_PLAYLIST_TRACKS} pt 
            ON p.${MusicQuizDbHelper.COLUMN_ID} = pt.${MusicQuizDbHelper.COLUMN_PLAYLIST_ID}
            GROUP BY p.${MusicQuizDbHelper.COLUMN_ID}
            ORDER BY p.${MusicQuizDbHelper.COLUMN_UPDATED_AT} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                playlists.add(cursorToPlaylist(it))
            }
        }

        return playlists
    }

    /**
     * Get a playlist by ID with track count
     */
    fun getPlaylistById(playlistId: Long): Playlist? {
        val db = dbHelper.readableDatabase

        // Query to get playlist with track count
        val query = """
            SELECT p.${MusicQuizDbHelper.COLUMN_ID}, 
                   p.${MusicQuizDbHelper.COLUMN_PLAYLIST_NAME}, 
                   p.${MusicQuizDbHelper.COLUMN_CREATED_AT}, 
                   p.${MusicQuizDbHelper.COLUMN_UPDATED_AT}, 
                   COUNT(pt.${MusicQuizDbHelper.COLUMN_TRACK_ID}) AS track_count
            FROM ${MusicQuizDbHelper.TABLE_PLAYLISTS} p
            LEFT JOIN ${MusicQuizDbHelper.TABLE_PLAYLIST_TRACKS} pt 
            ON p.${MusicQuizDbHelper.COLUMN_ID} = pt.${MusicQuizDbHelper.COLUMN_PLAYLIST_ID}
            WHERE p.${MusicQuizDbHelper.COLUMN_ID} = ?
            GROUP BY p.${MusicQuizDbHelper.COLUMN_ID}
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(playlistId.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                return cursorToPlaylist(it)
            }
        }

        return null
    }

    /**
     * Update a playlist
     */
    fun updatePlaylist(playlist: Playlist): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MusicQuizDbHelper.COLUMN_PLAYLIST_NAME, playlist.name)
            put(MusicQuizDbHelper.COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        return db.update(
            MusicQuizDbHelper.TABLE_PLAYLISTS,
            values,
            "${MusicQuizDbHelper.COLUMN_ID} = ?",
            arrayOf(playlist.id.toString())
        )
    }

    /**
     * Delete a playlist
     */
    fun deletePlaylist(playlistId: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            MusicQuizDbHelper.TABLE_PLAYLISTS,
            "${MusicQuizDbHelper.COLUMN_ID} = ?",
            arrayOf(playlistId.toString())
        )
    }

    /**
     * Add a track to a playlist
     */
    fun addTrackToPlaylist(playlistId: Long, trackId: Long): Long {
        val db = dbHelper.writableDatabase

        // Get the highest order number for this playlist
        val orderQuery = """
            SELECT MAX(${MusicQuizDbHelper.COLUMN_TRACK_ORDER}) 
            FROM ${MusicQuizDbHelper.TABLE_PLAYLIST_TRACKS}
            WHERE ${MusicQuizDbHelper.COLUMN_PLAYLIST_ID} = ?
        """.trimIndent()

        val cursor = db.rawQuery(orderQuery, arrayOf(playlistId.toString()))
        var nextOrder = 1
        cursor.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                nextOrder = it.getInt(0) + 1
            }
        }

        // Insert the track with the next order number
        val values = ContentValues().apply {
            put(MusicQuizDbHelper.COLUMN_PLAYLIST_ID, playlistId)
            put(MusicQuizDbHelper.COLUMN_TRACK_ID, trackId)
            put(MusicQuizDbHelper.COLUMN_TRACK_ORDER, nextOrder)
        }

        // Get current track count
        val countQuery = """
            SELECT COUNT(*) 
            FROM ${MusicQuizDbHelper.TABLE_PLAYLIST_TRACKS}
            WHERE ${MusicQuizDbHelper.COLUMN_PLAYLIST_ID} = ?
        """.trimIndent()

        val countCursor = db.rawQuery(countQuery, arrayOf(playlistId.toString()))
        var currentCount = 0
        countCursor.use {
            if (it.moveToFirst()) {
                currentCount = it.getInt(0)
            }
        }

        // Update the playlist's updated_at timestamp
        val playlistValues = ContentValues().apply {
            put(MusicQuizDbHelper.COLUMN_UPDATED_AT, System.currentTimeMillis())
            // We'll handle track count in the UI layer
        }

        db.update(
            MusicQuizDbHelper.TABLE_PLAYLISTS,
            playlistValues,
            "${MusicQuizDbHelper.COLUMN_ID} = ?",
            arrayOf(playlistId.toString())
        )

        return db.insert(MusicQuizDbHelper.TABLE_PLAYLIST_TRACKS, null, values)
    }

    /**
     * Remove a track from a playlist
     */
    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long): Int {
        val db = dbHelper.writableDatabase

        // Get current track count
        val countQuery = """
            SELECT COUNT(*) 
            FROM ${MusicQuizDbHelper.TABLE_PLAYLIST_TRACKS}
            WHERE ${MusicQuizDbHelper.COLUMN_PLAYLIST_ID} = ?
        """.trimIndent()

        val countCursor = db.rawQuery(countQuery, arrayOf(playlistId.toString()))
        var currentCount = 0
        countCursor.use {
            if (it.moveToFirst()) {
                currentCount = it.getInt(0)
            }
        }

        // Update the playlist's updated_at timestamp
        val playlistValues = ContentValues().apply {
            put(MusicQuizDbHelper.COLUMN_UPDATED_AT, System.currentTimeMillis())
            // We'll handle track count in the UI layer
        }

        db.update(
            MusicQuizDbHelper.TABLE_PLAYLISTS,
            playlistValues,
            "${MusicQuizDbHelper.COLUMN_ID} = ?",
            arrayOf(playlistId.toString())
        )

        return db.delete(
            MusicQuizDbHelper.TABLE_PLAYLIST_TRACKS,
            "${MusicQuizDbHelper.COLUMN_PLAYLIST_ID} = ? AND ${MusicQuizDbHelper.COLUMN_TRACK_ID} = ?",
            arrayOf(playlistId.toString(), trackId.toString())
        )
    }

    /**
     * Get all tracks in a playlist
     */
    fun getTracksInPlaylist(playlistId: Long): List<LocalTrack> {
        val tracks = mutableListOf<LocalTrack>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT t.* 
            FROM ${MusicQuizDbHelper.TABLE_TRACKS} t
            JOIN ${MusicQuizDbHelper.TABLE_PLAYLIST_TRACKS} pt 
            ON t.${MusicQuizDbHelper.COLUMN_ID} = pt.${MusicQuizDbHelper.COLUMN_TRACK_ID}
            WHERE pt.${MusicQuizDbHelper.COLUMN_PLAYLIST_ID} = ?
            ORDER BY pt.${MusicQuizDbHelper.COLUMN_TRACK_ORDER}
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(playlistId.toString()))
        cursor.use {
            val trackRepository = TrackRepository(context, dbHelper)
            while (it.moveToNext()) {
                tracks.add(trackRepository.cursorToTrack(it))
            }
        }

        return tracks
    }

    /**
     * Convert a cursor to a Playlist object
     */
    private fun cursorToPlaylist(cursor: Cursor): Playlist {
        val idIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_ID)
        val nameIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_PLAYLIST_NAME)
        val createdAtIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_CREATED_AT)
        val updatedAtIndex = cursor.getColumnIndex(MusicQuizDbHelper.COLUMN_UPDATED_AT)
        val trackCountIndex = cursor.getColumnIndex("track_count")

        return Playlist(
            id = cursor.getLong(idIndex),
            name = cursor.getString(nameIndex),
            createdAt = cursor.getLong(createdAtIndex),
            updatedAt = cursor.getLong(updatedAtIndex),
            trackCount = cursor.getInt(trackCountIndex)
        )
    }
}
