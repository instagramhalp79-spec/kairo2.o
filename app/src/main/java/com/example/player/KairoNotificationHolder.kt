package com.example.player

/**
 * Singleton holder connecting Android Notification & MediaSession callbacks
 * directly with the active KairoAudioPlayer instance.
 */
object KairoNotificationHolder {
    @Volatile
    var onPlayPause: (() -> Unit)? = null

    @Volatile
    var onSkipNext: (() -> Unit)? = null

    @Volatile
    var onSkipPrevious: (() -> Unit)? = null

    @Volatile
    var onStopPlayback: (() -> Unit)? = null

    @Volatile
    var onSeekTo: ((Long) -> Unit)? = null

    fun triggerPlayPause() {
        onPlayPause?.invoke()
    }

    fun triggerSkipNext() {
        onSkipNext?.invoke()
    }

    fun triggerSkipPrevious() {
        onSkipPrevious?.invoke()
    }

    fun stopAndClear() {
        onStopPlayback?.invoke()
    }

    fun triggerSeekTo(positionMs: Long) {
        onSeekTo?.invoke(positionMs)
    }
}
