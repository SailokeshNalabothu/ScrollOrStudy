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
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

class AppAccessibilityService : AccessibilityService() {

    private var currentApp: String? = null
    private var distractionSeconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private var mainRunnable: Runnable? = null
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    
    private var syncCounter = 0

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
                        distractingApps.contains(activeApp) -> {
                            AppState.scrollTimeToday++
                            
                            if (overlayView == null) {
                                distractionSeconds++
                                Log.d("TRACKING", "Scrolling: $distractionSeconds sec | Total Today: ${AppState.scrollTimeToday}s")

                                if (distractionSeconds >= 15) {
                                    Log.d("TRACKING", "Threshold reached! Showing motivation popup.")
                                    showOverlay()
                                }
                            }
                        }
                        
                        usefulApps.contains(activeApp) -> {
                            AppState.studyTimeToday++
                            distractionSeconds = 0
                            removeOverlay()
                            Log.d("TRACKING", "Studying in $activeApp | Total Today: ${AppState.studyTimeToday}s")
                        }
                        
                        else -> {
                            if (!systemPackages.contains(activeApp) && activeApp != packageName) {
                                distractionSeconds = 0
                                removeOverlay()
                            }
                        }
                    }
                }
                
                syncCounter++
                if (syncCounter >= 2) {
                    AppState.save(this@AppAccessibilityService)
                    syncCounter = 0
                }

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(mainRunnable!!)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (systemPackages.contains(packageName) || packageName == this.packageName) {
                return
            }

            if (packageName == currentApp) return
            
            Log.d("TRACKING", "App Changed: $currentApp -> $packageName")
            
            // 🔥 AI PREDICTOR: Force push event for testing
            if (distractingApps.contains(packageName)) {
                logScrollingEventToFirebase(packageName)
            }

            currentApp = packageName
            AppState.currentApp = packageName
            
            if (!distractingApps.contains(packageName)) {
                distractionSeconds = 0
                removeOverlay()
            }
        }
    }

    private fun logScrollingEventToFirebase(appName: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val database = AppState.getDatabaseInstance()
        val timestamp = System.currentTimeMillis()
        
        Log.d("FIREBASE_AI", "Logging event for $appName at $timestamp")
        
        database.getReference("scrolling_events")
            .child(user.uid)
            .push()
            .setValue(timestamp)
            .addOnSuccessListener {
                Log.d("FIREBASE_AI", "Successfully pushed to scrolling_events")
            }
            .addOnFailureListener {
                Log.e("FIREBASE_AI", "Failed to push: ${it.message}")
            }
    }

    private fun showOverlay() {
        handler.post {
            if (overlayView != null) return@post

            try {
                val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                overlayView = inflater.inflate(R.layout.overlay_view, null)

                val quoteView = overlayView?.findViewById<TextView>(R.id.tvMotivationQuote)
                quoteView?.text = AppState.cachedAiMotivation ?: MotivationEngine.getRandomScrollQuote()

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
                Log.d("TRACKING", "Motivation Overlay shown")

                overlayView?.findViewById<Button>(R.id.btnClose)?.setOnClickListener {
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
