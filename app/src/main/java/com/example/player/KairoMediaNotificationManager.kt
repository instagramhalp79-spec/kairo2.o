package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KairoMediaNotificationManager(
    private val context: Context,
    private val onPlayPause: () -> Unit,
    private val onSkipNext: () -> Unit,
    private val onSkipPrevious: () -> Unit,
    private val onStopPlayback: () -> Unit,
    private val onSeekTo: (Long) -> Unit
) {
    companion object {
        const val CHANNEL_ID = "kairo_playback_channel"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "KAIRO_MEDIA"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val scope = CoroutineScope(Dispatchers.IO)
    private val imageLoader = ImageLoader(context)
    private var lastBitmap: Bitmap? = null
    private var lastSongId: String? = null

    val mediaSession: MediaSession = MediaSession(context, "KairoMusicSession").apply {
        setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                onPlayPause()
            }

            override fun onPause() {
                onPlayPause()
            }

            override fun onSkipToNext() {
                onSkipNext()
            }

            override fun onSkipToPrevious() {
                onSkipPrevious()
            }

            override fun onStop() {
                onStopPlayback()
            }

            override fun onSeekTo(pos: Long) {
                onSeekTo(pos)
            }
        })
        isActive = true
    }

    init {
        // Register holder callbacks for responsive lock screen & notification broadcast controls
        KairoNotificationHolder.onPlayPause = onPlayPause
        KairoNotificationHolder.onSkipNext = onSkipNext
        KairoNotificationHolder.onSkipPrevious = onSkipPrevious
        KairoNotificationHolder.onStopPlayback = onStopPlayback
        KairoNotificationHolder.onSeekTo = onSeekTo

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "KAIRO Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media controls and current playing song"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateMetadata(song: Song, durationMs: Long, isPlaying: Boolean, positionMs: Long) {
        try {
            if (!mediaSession.isActive) {
                mediaSession.isActive = true
            }

            // Update MediaSession state for Lock Screen and System Controls
            val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val actions = PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_STOP or
                    PlaybackState.ACTION_SEEK_TO

            val playbackState = PlaybackState.Builder()
                .setState(state, positionMs, 1.0f, android.os.SystemClock.elapsedRealtime())
                .setActions(actions)
                .build()
            mediaSession.setPlaybackState(playbackState)

            // Update MediaSession metadata
            val metadataBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)

            lastBitmap?.let { bmp ->
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bmp)
            }
            mediaSession.setMetadata(metadataBuilder.build())

            // Fetch Artwork Bitmap asynchronously if new song
            if (lastSongId != song.id) {
                lastSongId = song.id
                lastBitmap = null
                scope.launch {
                    try {
                        val req = ImageRequest.Builder(context)
                            .data(song.artworkUrl)
                            .allowHardware(false)
                            .build()
                        val result = imageLoader.execute(req)
                        val drawable = result.drawable
                        if (drawable is BitmapDrawable) {
                            lastBitmap = drawable.bitmap
                            val updatedMeta = MediaMetadata.Builder()
                                .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                                .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                                .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                                .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)
                                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, drawable.bitmap)
                                .build()
                            mediaSession.setMetadata(updatedMeta)
                            // Re-post notification with loaded album art
                            showNotification(song, isPlaying)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed loading notification artwork: ${e.message}")
                    }
                }
            }

            // Build Notification
            showNotification(song, isPlaying)
        } catch (e: Exception) {
            Log.w(TAG, "Could not update media session/notification: ${e.message}")
        }
    }

    private fun showNotification(song: Song, isPlaying: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Responsive Broadcast Actions for lock screen and notification shade
            val prevAction = Notification.Action.Builder(
                android.R.drawable.ic_media_previous,
                "Previous",
                createBroadcastPendingIntent(KairoMediaReceiver.ACTION_PREV, 1)
            ).build()

            val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            val playPauseTitle = if (isPlaying) "Pause" else "Play"
            val playPauseAction = Notification.Action.Builder(
                playPauseIcon,
                playPauseTitle,
                createBroadcastPendingIntent(KairoMediaReceiver.ACTION_PLAY_PAUSE, 2)
            ).build()

            val nextAction = Notification.Action.Builder(
                android.R.drawable.ic_media_next,
                "Next",
                createBroadcastPendingIntent(KairoMediaReceiver.ACTION_NEXT, 3)
            ).build()

            val stopAction = Notification.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Close",
                createBroadcastPendingIntent(KairoMediaReceiver.ACTION_STOP, 4)
            ).build()

            val dismissPendingIntent = createBroadcastPendingIntent(KairoMediaReceiver.ACTION_DISMISS, 5)

            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

            builder.setContentTitle(song.title)
                .setContentText(song.artist)
                .setSubText(if (song.album.isNotBlank() && song.album != "KAIRO Music") "From \"${song.album}\"" else "KAIRO Music")
                .setSmallIcon(com.example.R.drawable.ic_kairo_notification)
                .setColor(0xFFE50914.toInt()) // Kairo Brand Red
                .setColorized(true)
                .setContentIntent(contentPendingIntent)
                .setDeleteIntent(dismissPendingIntent)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying)
                .addAction(prevAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .addAction(stopAction)

            lastBitmap?.let { bmp ->
                builder.setLargeIcon(bmp)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.style = Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            }

            val notification = builder.build()
            val service = KairoPlaybackService.serviceInstance
            if (service != null && isPlaying) {
                try {
                    service.startForeground(NOTIFICATION_ID, notification)
                } catch (e: Exception) {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            } else {
                if (service != null) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            service.stopForeground(android.app.Service.STOP_FOREGROUND_DETACH)
                        } else {
                            @Suppress("DEPRECATION")
                            service.stopForeground(false)
                        }
                    } catch (_: Exception) {}
                }
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Notification notify skipped or failed safely: ${e.message}")
        }
    }

    private fun createBroadcastPendingIntent(actionName: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, KairoMediaReceiver::class.java).apply {
            action = actionName
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun clearNotification() {
        try {
            val service = KairoPlaybackService.serviceInstance
            if (service != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        service.stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        service.stopForeground(true)
                    }
                } catch (_: Exception) {}
            }
            notificationManager.cancel(NOTIFICATION_ID)
            mediaSession.setPlaybackState(
                PlaybackState.Builder()
                    .setState(PlaybackState.STATE_STOPPED, 0L, 0f)
                    .build()
            )
        } catch (e: Exception) {
            Log.w(TAG, "clearNotification error: ${e.message}")
        }
    }

    fun release() {
        clearNotification()
        mediaSession.isActive = false
        mediaSession.release()
    }
}
