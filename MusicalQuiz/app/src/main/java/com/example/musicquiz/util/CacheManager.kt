package com.example.musicquiz.util

import android.content.Context
import android.content.SharedPreferences
import com.example.musicquiz.model.Track
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages caching of search results and recently played tracks
 */
class CacheManager(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val CACHE_PREFS = "music_quiz_cache"
        private const val RECENT_SEARCHES = "recent_searches"
        private const val RECENT_TRACKS = "recent_tracks"
        private const val MAX_CACHE_SIZE = 50 // Maximum number of items to cache
    }

    /**
     * Cache a search query and its results
     */
    fun cacheSearchResults(query: String, tracks: List<Track>) {
        val cachedSearches = getRecentSearches().toMutableMap()

        // Remove oldest entry if cache is full
        if (cachedSearches.size >= MAX_CACHE_SIZE) {
            val oldestKey = cachedSearches.keys.first()
            cachedSearches.remove(oldestKey)
        }

        cachedSearches[query] = tracks
        preferences.edit().putString(RECENT_SEARCHES, gson.toJson(cachedSearches)).apply()
    }

    /**
     * Get cached search results for a query
     */
    fun getCachedSearchResults(query: String): List<Track>? {
        val cachedSearches = getRecentSearches()
        return cachedSearches[query]
    }

    /**
     * Cache a recently played track
     */
    fun cacheRecentTrack(track: Track) {
        val recentTracks = getRecentTracks().toMutableList()

        // Remove track if it already exists to avoid duplicates
        recentTracks.removeAll { it.id == track.id }

        // Add new track at the beginning
        recentTracks.add(0, track)

        // Keep only the most recent tracks
        while (recentTracks.size > MAX_CACHE_SIZE) {
            recentTracks.removeAt(recentTracks.lastIndex)
        }

        preferences.edit().putString(RECENT_TRACKS, gson.toJson(recentTracks)).apply()
    }

    /**
     * Get list of recently played tracks
     */
    fun getRecentTracks(): List<Track> {
        val json = preferences.getString(RECENT_TRACKS, null) ?: return emptyList()
        val type = object : TypeToken<List<Track>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getRecentSearches(): Map<String, List<Track>> {
        val json = preferences.getString(RECENT_SEARCHES, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, List<Track>>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Clear all cached data
     */
    fun clearCache() {
        preferences.edit().clear().apply()
    }
}
