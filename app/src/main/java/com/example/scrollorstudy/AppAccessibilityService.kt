package com.example.scrollorstudy

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button

class AppAccessibilityService : AccessibilityService() {

    private var currentApp: String? = null
    private var distractionSeconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private var mainRunnable: Runnable? = null
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    private val distractingApps = listOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.facebook.katana",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.snapchat.android"
    )

    private val usefulApps = listOf(
        "com.google.android.apps.classroom",
        "com.github.android",
        "com.microsoft.office.word",
        "com.microsoft.office.excel",
        "com.google.android.apps.docs.editors.sheets"
    )

    // System and Neutral apps are ignored by the logic below
    private val systemPackages = listOf(
        "android",
        "com.android.systemui",
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.miui.home"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("TRACKING", "Accessibility Service Connected")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        AppState.load(this)
        startGlobalTracking()
    }

    private fun startGlobalTracking() {
        mainRunnable = object : Runnable {
            override fun run() {
                val activeApp = currentApp
                
                if (activeApp != null) {
                    when {
                        // 1. Distraction Tracking
                        distractingApps.contains(activeApp) -> {
                            AppState.scrollTimeToday++
                            
                            if (overlayView == null) {
                                distractionSeconds++
                                Log.d("TRACKING", "Scrolling: $distractionSeconds sec | Total Today: ${AppState.scrollTimeToday}s")

                                if (distractionSeconds >= 15) {
                                    Log.d("TRACKING", "Threshold reached! Showing overlay.")
                                    showOverlay()
                                }
                            }
                        }
                        
                        // 2. Useful Apps Tracking (Study Time)
                        usefulApps.contains(activeApp) -> {
                            AppState.studyTimeToday++
                            distractionSeconds = 0
                            removeOverlay()
                            Log.d("TRACKING", "Studying in $activeApp | Total Today: ${AppState.studyTimeToday}s")
                        }
                        
                        // 3. Neutral/Other Apps
                        else -> {
                            // Don't count time, just reset distraction counter and remove overlay
                            if (!systemPackages.contains(activeApp) && activeApp != packageName) {
                                distractionSeconds = 0
                                removeOverlay()
                            }
                        }
                    }
                }
                
                // Save periodically (every 10 seconds of activity)
                if ((AppState.scrollTimeToday > 0 && AppState.scrollTimeToday % 10 == 0L) || 
                    (AppState.studyTimeToday > 0 && AppState.studyTimeToday % 10 == 0L)) {
                    AppState.save(this@AppAccessibilityService)
                }

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(mainRunnable!!)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Ignore internal switches to system UI so we don't drop tracking of the main app
            if (systemPackages.contains(packageName) || packageName == this.packageName) {
                return
            }

            if (packageName == currentApp) return
            
            Log.d("TRACKING", "App Changed: $currentApp -> $packageName")
            currentApp = packageName
            AppState.currentApp = packageName
            
            // If user manually switched to a non-distraction app, cleanup
            if (!distractingApps.contains(packageName)) {
                distractionSeconds = 0
                removeOverlay()
            }
        }
    }

    private fun showOverlay() {
        handler.post {
            if (overlayView != null) return@post

            try {
                val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                overlayView = inflater.inflate(R.layout.overlay_view, null)

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )
                params.gravity = Gravity.CENTER

                windowManager?.addView(overlayView, params)
                Log.d("TRACKING", "Overlay shown")

                overlayView?.findViewById<Button>(R.id.btnClose)?.setOnClickListener {
                    Log.d("TRACKING", "User clicked Close on overlay")
                    removeOverlay()
                    distractionSeconds = 0
                }
            } catch (e: Exception) {
                Log.e("TRACKING", "Error showing overlay: ${e.message}")
            }
        }
    }

    private fun removeOverlay() {
        handler.post {
            if (overlayView != null) {
                try {
                    windowManager?.removeView(overlayView)
                    Log.d("TRACKING", "Overlay removed")
                } catch (e: Exception) {
                    Log.e("TRACKING", "Error removing overlay: ${e.message}")
                }
                overlayView = null
            }
        }
    }

    override fun onInterrupt() {
        stopGlobalTracking()
    }

    private fun stopGlobalTracking() {
        mainRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppState.save(this)
        stopGlobalTracking()
        removeOverlay()
    }
}
