package com.hifth.ui

import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AudioController {
    private var player: MediaPlayer? = null
    var isPlaying by mutableStateOf(false)
        private set
    var duration by mutableIntStateOf(0)
        private set
    var position by mutableIntStateOf(0)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun play(url: String) {
        stop()
        error = null
        runCatching {
            MediaPlayer().also { media ->
                player = media
                media.setDataSource(url)
                media.setOnPreparedListener {
                    duration = it.duration.coerceAtLeast(0)
                    it.start()
                    isPlaying = true
                }
                media.setOnCompletionListener {
                    isPlaying = false
                    position = 0
                }
                media.setOnErrorListener { _, _, _ ->
                    error = "Audio could not be played. Check your connection and try again."
                    isPlaying = false
                    true
                }
                media.prepareAsync()
            }
        }.onFailure {
            error = it.message ?: "Audio could not be played."
        }
    }

    fun playFile(path: String) {
        stop()
        error = null
        runCatching {
            MediaPlayer().also { media ->
                player = media
                media.setDataSource(path)
                media.setOnPreparedListener {
                    duration = it.duration.coerceAtLeast(0)
                    it.start()
                    isPlaying = true
                }
                media.setOnCompletionListener {
                    isPlaying = false
                    position = 0
                }
                media.setOnErrorListener { _, _, _ ->
                    error = "Recording could not be played."
                    isPlaying = false
                    true
                }
                media.prepareAsync()
            }
        }.onFailure {
            error = it.message ?: "Recording could not be played."
        }
    }

    fun toggle() {
        val current = player ?: return
        if (isPlaying) {
            current.pause()
            position = current.currentPosition
            isPlaying = false
        } else {
            current.start()
            isPlaying = true
        }
    }

    fun seekTo(value: Int) {
        player?.seekTo(value)
        position = value
    }

    fun refreshPosition() {
        if (isPlaying) position = player?.currentPosition ?: position
    }

    fun stop() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        isPlaying = false
        position = 0
        duration = 0
    }

    fun release() = stop()
}
