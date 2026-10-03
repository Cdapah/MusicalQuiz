package com.example.musicquiz.util

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

/**
 * Utility class to manage MediaPlayer instances throughout the app
 */
class MediaPlayerManager private constructor() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentUrl: String? = null
    private var onPlaybackStartedListener: (() -> Unit)? = null
    private var onPlaybackCompletedListener: (() -> Unit)? = null
    private var onPlaybackErrorListener: ((Exception) -> Unit)? = null

    companion object {
        private const val TAG = "MediaPlayerManager"
        val instance = MediaPlayerManager()
    }

    /**
     * Play audio from the given URL
     */
    fun playAudio(url: String) {
        // If already playing the same URL, do nothing
        if (isPlaying && url == currentUrl) {
            return
        }

        // If playing a different URL, stop current playback
        if (isPlaying) {
            stopPlayback()
        }

        try {
            currentUrl = url
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener {
                    start()
                    this@MediaPlayerManager.isPlaying = true
                    onPlaybackStartedListener?.invoke()
                }
                setOnCompletionListener {
                    this@MediaPlayerManager.isPlaying = false
                    onPlaybackCompletedListener?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    this@MediaPlayerManager.isPlaying = false
                    onPlaybackErrorListener?.invoke(Exception("MediaPlayer error: what=$what, extra=$extra"))
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}", e)
            onPlaybackErrorListener?.invoke(e)
        }
    }

    /**
     * Stop current playback
     */
    fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying()) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        isPlaying = false
        currentUrl = null
    }

    /**
     * Check if audio is currently playing
     */
    fun isAudioPlaying(): Boolean {
        return isPlaying
    }

    /**
     * Get the URL of the currently playing audio
     */
    fun getCurrentUrl(): String? {
        return currentUrl
    }

    /**
     * Set listener for playback started events
     */
    fun setOnPlaybackStartedListener(listener: (() -> Unit)?) {
        onPlaybackStartedListener = listener
    }

    /**
     * Set listener for playback completed events
     */
    fun setOnPlaybackCompletedListener(listener: (() -> Unit)?) {
        onPlaybackCompletedListener = listener
    }

    /**
     * Set listener for playback error events
     */
    fun setOnPlaybackErrorListener(listener: ((Exception) -> Unit)?) {
        onPlaybackErrorListener = listener
    }

    /**
     * Release all resources. Should be called when the app is closing.
     */
    fun release() {
        stopPlayback()
        onPlaybackStartedListener = null
        onPlaybackCompletedListener = null
        onPlaybackErrorListener = null
    }
}
