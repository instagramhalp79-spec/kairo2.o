package com.example.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * YouTube Player internal state enum according to the state machine architecture.
 */
enum class YouTubePlaybackStatus {
    IDLE,
    LOADING,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

/**
 * Robust YouTube Player State Machine and WebView bridge.
 * Features:
 * - Internal state machine tracking: IDLE, LOADING, BUFFERING, PLAYING, PAUSED, ENDED, ERROR.
 * - Strict 1-to-1 session ID tracking: every loadSong request generates a unique session ID.
 *   Responses, errors, or events with outdated session IDs are discarded to prevent race conditions.
 * - Zero API key reliance. Handles error 150/152 gracefully by notifying the engine to fallback seamlessly.
 */
class YouTubePlayerBridge(
    private val context: Context,
    private val onStateChanged: (isPlaying: Boolean, isBuffering: Boolean) -> Unit,
    private val onTimeUpdate: (currentMs: Long, durationMs: Long) -> Unit,
    private val onSongEnded: () -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "KAIRO_YT_BRIDGE"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var isPlayerReady = false

    // Session ID generator for strict 1:1 request-response race-condition prevention
    private val sessionCounter = AtomicLong(0L)
    @Volatile
    private var activeSessionId: Long = 0L

    private val _playbackStatus = MutableStateFlow(YouTubePlaybackStatus.IDLE)
    val playbackStatus: StateFlow<YouTubePlaybackStatus> = _playbackStatus.asStateFlow()

    private var pendingRequest: PendingLoadRequest? = null

    var currentVideoId: String? = null
        private set

    private data class PendingLoadRequest(
        val sessionId: Long,
        val videoId: String
    )

    @SuppressLint("SetJavaScriptEnabled")
    fun attachWebView(view: WebView) {
        if (webView === view) return
        webView = view
        setupWebView(view)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(ctx: Context): WebView {
        webView?.let { existing ->
            (existing.parent as? android.view.ViewGroup)?.removeView(existing)
            return existing
        }
        val wv = WebView(ctx)
        webView = wv
        setupWebView(wv)
        return wv
    }

    fun detachFromParent() {
        mainHandler.post {
            (webView?.parent as? android.view.ViewGroup)?.removeView(webView)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(wv: WebView) {
        wv.settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowContentAccess = true
            allowFileAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }

        wv.webChromeClient = WebChromeClient()
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(TAG, "[KAIRO][YT] IFrame host HTML loaded: $url")
            }
        }

        wv.addJavascriptInterface(KairoBridgeInterface(), "KairoBridge")

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
              <style>
                html, body { margin:0; padding:0; background:#000; overflow:hidden; width:100%; height:100%; }
                #player { width:100%; height:100%; position:absolute; top:0; left:0; right:0; bottom:0; }
              </style>
            </head>
            <body>
              <div id="player"></div>
              <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player = null;
                var trackerTimer = null;
                var isApiReady = false;
                var currentSessionId = 0;

                function onYouTubeIframeAPIReady() {
                  isApiReady = true;
                  if (window.KairoBridge) window.KairoBridge.onBridgeReady();
                }

                function loadSongWithSession(sessionId, videoId) {
                  currentSessionId = sessionId;
                  if (!player) {
                    player = new YT.Player('player', {
                      height: '100%',
                      width: '100%',
                      videoId: videoId,
                      playerVars: {
                        'autoplay': 1,
                        'controls': 1,
                        'fs': 0,
                        'playsinline': 1,
                        'rel': 0,
                        'modestbranding': 1,
                        'enablejsapi': 1
                      },
                      events: {
                        'onReady': function(e) {
                          try {
                            e.target.playVideo();
                          } catch(err) {
                            console.error(err);
                          }
                          startTracker();
                          if (window.KairoBridge) window.KairoBridge.onPlayerState(currentSessionId, "READY");
                        },
                        'onStateChange': function(e) {
                          var state = e.data;
                          // YT states: -1 (unstarted), 0 (ended), 1 (playing), 2 (paused), 3 (buffering), 5 (cued)
                          if (window.KairoBridge) window.KairoBridge.onYouTubeState(currentSessionId, state);
                          if (state === 1) { // PLAYING
                            startTracker();
                          } else if (state === 0) { // ENDED
                            stopTracker();
                            if (window.KairoBridge) window.KairoBridge.onTrackEnded(currentSessionId);
                          } else if (state === 2) { // PAUSED
                            stopTracker();
                          } else if (state === 5) { // CUED
                            try { e.target.playVideo(); } catch(err) {}
                          }
                        },
                        'onError': function(e) {
                          var errCode = e.data;
                          if (window.KairoBridge) window.KairoBridge.onPlayerError(currentSessionId, errCode);
                        }
                      }
                    });
                  } else {
                    try {
                      player.loadVideoById({
                        videoId: videoId,
                        startSeconds: 0
                      });
                      player.playVideo();
                    } catch(err) {
                      try {
                        player.loadVideoById(videoId);
                        player.playVideo();
                      } catch(e2) {
                        console.error(e2);
                      }
                    }
                  }
                }

                function startTracker() {
                  stopTracker();
                  trackerTimer = setInterval(function() {
                    if (player && typeof player.getCurrentTime === 'function') {
                      var cur = player.getCurrentTime() || 0;
                      var dur = player.getDuration() || 0;
                      if (window.KairoBridge) window.KairoBridge.onTime(currentSessionId, cur, dur);
                    }
                  }, 250);
                }

                function stopTracker() {
                  if (trackerTimer) { clearInterval(trackerTimer); trackerTimer = null; }
                }

                function playAudio() {
                  if (player && typeof player.playVideo === 'function') {
                    player.playVideo();
                  }
                }

                function pauseAudio() {
                  if (player && typeof player.pauseVideo === 'function') {
                    player.pauseVideo();
                  }
                }

                function seekAudio(sec) {
                  if (player && typeof player.seekTo === 'function') {
                    player.seekTo(sec, true);
                  }
                }

                function setPlayerVolume(vol) {
                  if (player && typeof player.setVolume === 'function') {
                    player.setVolume(vol);
                  }
                }

                function mutePlayer() {
                  if (player && typeof player.mute === 'function') {
                    player.mute();
                  }
                }

                function unMutePlayer() {
                  if (player && typeof player.unMute === 'function') {
                    player.unMute();
                  }
                }
              </script>
            </body>
            </html>
        """.trimIndent()

        wv.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
    }

    /**
     * Initiates playback for the exact videoId.
     * Generates a new session ID and transitions state to LOADING.
     */
    fun playVideo(videoId: String) {
        val sessionId = sessionCounter.incrementAndGet()
        activeSessionId = sessionId
        currentVideoId = videoId
        transitionTo(YouTubePlaybackStatus.LOADING)

        mainHandler.post {
            if (webView == null) {
                getOrCreateWebView(context)
            }
            if (isPlayerReady) {
                Log.d(TAG, "[KAIRO][YT] [Session #$sessionId] Dispatching loadSong for videoId: $videoId")
                webView?.evaluateJavascript("loadSongWithSession($sessionId, '$videoId');", null)
            } else {
                Log.d(TAG, "[KAIRO][YT] [Session #$sessionId] Bridge not ready yet, queuing request for videoId: $videoId")
                pendingRequest = PendingLoadRequest(sessionId, videoId)
            }
        }
    }

    fun play() {
        mainHandler.post {
            webView?.evaluateJavascript("playAudio();", null)
        }
    }

    fun pause() {
        mainHandler.post {
            webView?.evaluateJavascript("pauseAudio();", null)
            transitionTo(YouTubePlaybackStatus.PAUSED)
        }
    }

    fun seekTo(positionMs: Long) {
        mainHandler.post {
            val seconds = positionMs / 1000f
            webView?.evaluateJavascript("seekAudio($seconds);", null)
        }
    }

    fun stop() {
        mainHandler.post {
            webView?.evaluateJavascript("pauseAudio();", null)
            currentVideoId = null
            transitionTo(YouTubePlaybackStatus.IDLE)
        }
    }

    fun setVolume(volume0to100: Int) {
        val clamped = volume0to100.coerceIn(0, 100)
        mainHandler.post {
            webView?.evaluateJavascript("setPlayerVolume($clamped);", null)
        }
    }

    fun mute() {
        mainHandler.post {
            webView?.evaluateJavascript("mutePlayer();", null)
        }
    }

    fun unmute() {
        mainHandler.post {
            webView?.evaluateJavascript("unMutePlayer();", null)
        }
    }

    private fun transitionTo(newStatus: YouTubePlaybackStatus) {
        _playbackStatus.value = newStatus
    }

    inner class KairoBridgeInterface {
        @JavascriptInterface
        fun onBridgeReady() {
            mainHandler.post {
                Log.d(TAG, "[KAIRO][YT] YouTube IFrame API Ready in WebView")
                isPlayerReady = true
                pendingRequest?.let { req ->
                    pendingRequest = null
                    if (req.sessionId == activeSessionId) {
                        Log.d(TAG, "[KAIRO][YT] Executing pending session #${req.sessionId} for video ${req.videoId}")
                        webView?.evaluateJavascript("loadSongWithSession(${req.sessionId}, '${req.videoId}');", null)
                    } else {
                        Log.d(TAG, "[KAIRO][YT] Dropped stale pending session #${req.sessionId} (current: $activeSessionId)")
                    }
                }
            }
        }

        @JavascriptInterface
        fun onPlayerState(sessionId: Long, stateStr: String) {
            mainHandler.post {
                if (sessionId != activeSessionId) {
                    Log.v(TAG, "[KAIRO][YT] Discarded onPlayerState for stale session #$sessionId")
                    return@post
                }
                Log.d(TAG, "[KAIRO][YT] [Session #$sessionId] Player State: $stateStr")
                if (stateStr == "READY") {
                    transitionTo(YouTubePlaybackStatus.BUFFERING)
                }
            }
        }

        @JavascriptInterface
        fun onYouTubeState(sessionId: Long, state: Int) {
            mainHandler.post {
                if (sessionId != activeSessionId) {
                    Log.v(TAG, "[KAIRO][YT] Discarded onYouTubeState for stale session #$sessionId")
                    return@post
                }
                Log.d(TAG, "[KAIRO][YT] [Session #$sessionId] YouTube state: $state")
                when (state) {
                    1 -> { // PLAYING
                        transitionTo(YouTubePlaybackStatus.PLAYING)
                        onStateChanged(true, false)
                    }
                    2 -> { // PAUSED
                        transitionTo(YouTubePlaybackStatus.PAUSED)
                        onStateChanged(false, false)
                    }
                    3 -> { // BUFFERING
                        transitionTo(YouTubePlaybackStatus.BUFFERING)
                        onStateChanged(false, true)
                    }
                    -1 -> { // UNSTARTED
                        transitionTo(YouTubePlaybackStatus.LOADING)
                        onStateChanged(false, true)
                    }
                    0 -> { // ENDED
                        transitionTo(YouTubePlaybackStatus.ENDED)
                    }
                    else -> {}
                }
            }
        }

        @JavascriptInterface
        fun onTime(sessionId: Long, currentSeconds: Float, durationSeconds: Float) {
            mainHandler.post {
                if (sessionId != activeSessionId) return@post
                val curMs = (currentSeconds * 1000).toLong()
                val durMs = (durationSeconds * 1000).toLong()
                onTimeUpdate(curMs, durMs)
            }
        }

        @JavascriptInterface
        fun onTrackEnded(sessionId: Long) {
            mainHandler.post {
                if (sessionId != activeSessionId) {
                    Log.d(TAG, "[KAIRO][YT] Dropped onTrackEnded for stale session #$sessionId")
                    return@post
                }
                Log.d(TAG, "[KAIRO][YT] [Session #$sessionId] YouTube track finished")
                transitionTo(YouTubePlaybackStatus.ENDED)
                onSongEnded()
            }
        }

        @JavascriptInterface
        fun onPlayerError(sessionId: Long, code: Int) {
            mainHandler.post {
                if (sessionId != activeSessionId) {
                    Log.d(TAG, "[KAIRO][YT] Dropped onPlayerError for stale session #$sessionId")
                    return@post
                }
                Log.w(TAG, "[KAIRO][YT] [Session #$sessionId] YouTube player error code: $code")
                transitionTo(YouTubePlaybackStatus.ERROR)

                // Error 150/152/101 indicates embedding is restricted by video owner or YouTube
                val message = when (code) {
                    101, 150, 152 -> "RESTRICTED_CODE_$code"
                    153 -> "RESTRICTED_CODE_153"
                    2 -> "Invalid video ID ($code)"
                    5 -> "HTML5 player error ($code)"
                    100 -> "Video not found or removed ($code)"
                    else -> "Playback error ($code)"
                }
                onError(message)
            }
        }
    }
}
