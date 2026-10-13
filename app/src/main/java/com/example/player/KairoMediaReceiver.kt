package com.example.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class KairoMediaReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "KAIRO_MEDIA_RCV"
        const val ACTION_PLAY_PAUSE = "com.example.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_PREV = "com.example.ACTION_PREV"
        const val ACTION_STOP = "com.example.ACTION_STOP"
        const val ACTION_DISMISS = "com.example.ACTION_DISMISS"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "Received notification media broadcast action: $action")
        when (action) {
            ACTION_PLAY_PAUSE -> {
                KairoNotificationHolder.triggerPlayPause()
            }
            ACTION_NEXT -> {
                KairoNotificationHolder.triggerSkipNext()
            }
            ACTION_PREV -> {
                KairoNotificationHolder.triggerSkipPrevious()
            }
            ACTION_STOP, ACTION_DISMISS -> {
                KairoNotificationHolder.stopAndClear()
            }
            Intent.ACTION_MEDIA_BUTTON -> {
                val keyEvent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as? android.view.KeyEvent
                }
                if (keyEvent != null && keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
                        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE,
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        android.view.KeyEvent.KEYCODE_HEADSETHOOK -> KairoNotificationHolder.triggerPlayPause()
                        android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> KairoNotificationHolder.triggerSkipNext()
                        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> KairoNotificationHolder.triggerSkipPrevious()
                        android.view.KeyEvent.KEYCODE_MEDIA_STOP -> KairoNotificationHolder.stopAndClear()
                    }
                }
            }
        }
    }
}
