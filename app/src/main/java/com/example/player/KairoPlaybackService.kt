package com.example.player

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log

class KairoPlaybackService : Service {
    constructor() : super()

    companion object {
        private const val TAG = "KAIRO_PLAYBACK_SVC"

        @Volatile
        var serviceInstance: KairoPlaybackService? = null
            private set

        fun start(context: Context) {
            try {
                val intent = Intent(context, KairoPlaybackService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start KairoPlaybackService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, KairoPlaybackService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not stop KairoPlaybackService: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceInstance = this
        return START_NOT_STICKY
    }

    /**
     * Called when the user removes the task that comes from the service's application,
     * typically by swiping the app away from the recent apps / task switcher list.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "[KAIRO] App task removed from background/recent tasks. Cleaning up notification and playback...")
        KairoNotificationHolder.stopAndClear()
        stopSelf()
    }

    override fun onDestroy() {
        serviceInstance = null
        super.onDestroy()
        Log.d(TAG, "KairoPlaybackService destroyed")
    }
}
