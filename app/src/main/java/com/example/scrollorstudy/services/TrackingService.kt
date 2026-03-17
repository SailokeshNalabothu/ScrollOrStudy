package com.example.scrollorstudy.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
import com.example.scrollorstudy.AppState
import com.example.scrollorstudy.OverlayService
import com.example.scrollorstudy.R
import kotlinx.coroutines.*

class TrackingService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val distractionApps = listOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.facebook.katana",
        "com.whatsapp"
    )

    private var lastApp: String? = null
    private var timeSpent = 0
    private var alertShown = false

    override fun onCreate() {
        super.onCreate()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tracking_channel",
                "Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "tracking_channel")
            .setContentTitle("ScrollOrStudy Running")
            .setContentText("Tracking apps...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
        Log.d("SERVICE", "Foreground started")

        if (intent?.action == "RESET_TRACKING") {
            timeSpent = 0
            lastApp = null
            alertShown = false
            Log.d("TRACKING", "Manual reset from Activity")
        }

        startTracking()

        return START_STICKY
    }

    private fun startTracking() {
        // Use a job to avoid starting multiple coroutines on multiple onStartCommand calls
        scope.coroutineContext[Job]?.cancel()
        scope.launch {
            while (true) {
                val currentApp = AppState.currentApp

                if (!distractionApps.contains(currentApp)) {
                    timeSpent = 0
                    lastApp = currentApp
                    alertShown = false
                    delay(3000)
                    continue
                }

                if (currentApp != lastApp) {
                    Log.d("TRACKING", "App changed: $lastApp -> $currentApp")
                    // RESET TIMER
                    timeSpent = 0
                    lastApp = currentApp
                    alertShown = false
                }

                timeSpent += 3
                Log.d("TRACKING", "App: $currentApp | Time: $timeSpent sec")

                if (timeSpent >= 15 && !alertShown) {
                    Log.d("ALERT", "User wasting time")
                    withContext(Dispatchers.Main) {
                        showOverlay()
                    }
                    alertShown = true
                }

                delay(3000)
            }
        }
    }

    private fun showOverlay() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val view = LayoutInflater.from(this).inflate(R.layout.overlay_view, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER

        try {
            windowManager.addView(view, params)

            view.findViewById<Button>(R.id.btnClose).setOnClickListener {
                windowManager.removeView(view)
            }
        } catch (e: Exception) {
            Log.e("OVERLAY_ERROR", "Could not add overlay view: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
