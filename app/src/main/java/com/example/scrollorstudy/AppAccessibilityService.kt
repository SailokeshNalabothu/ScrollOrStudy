package com.example.scrollorstudy

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView
import com.example.scrollorstudy.data.local.PreferencesManager
import com.example.scrollorstudy.data.repository.UserRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppAccessibilityService : AccessibilityService() {

    private val preferencesManager: PreferencesManager by lazy { (application as ScrollOrStudyApplication).container.preferencesManager }
    private val userRepository: UserRepository by lazy { (application as ScrollOrStudyApplication).container.userRepository }

    private var currentApp: String? = null
    private var distractionSeconds = 0
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var trackingJob: Job? = null

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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (packageName == this.packageName || packageName == "com.android.systemui") {
                return
            }

            if (packageName == currentApp) return
            
            Log.d("TRACKING", "App Changed: $currentApp -> $packageName")
            currentApp = packageName
            
            manageTrackingCycle()
        }
    }

    private fun manageTrackingCycle() {
        trackingJob?.cancel()
        
        val activeApp = currentApp ?: return
        
        if (distractingApps.contains(activeApp) || usefulApps.contains(activeApp)) {
            trackingJob = serviceScope.launch {
                while (isActive) {
                    delay(1000)
                    val isStudyMode = preferencesManager.isStudyModeActive.first()
                    
                    if (distractingApps.contains(activeApp)) {
                        if (overlayView == null && isStudyMode) {
                            preferencesManager.updateTime(studyDelta = 0, scrollDelta = 1)
                            distractionSeconds++
                            
                            Log.d("TRACKING", "App: $activeApp | Time: $distractionSeconds sec")
                            
                            if (distractionSeconds >= 15) {
                                Log.d("ALERT", "User wasting time")
                                showOverlay()
                            }
                        }
                    } else if (usefulApps.contains(activeApp) && isStudyMode) {
                        preferencesManager.updateTime(studyDelta = 1, scrollDelta = 0)
                        distractionSeconds = 0
                        removeOverlay()
                    }
                    
                    if (System.currentTimeMillis() % 10000 < 1000 && isStudyMode) {
                        syncToFirebase()
                    }
                }
            }
        } else {
            distractionSeconds = 0
            removeOverlay()
        }
    }

    private suspend fun syncToFirebase() {
        val study = preferencesManager.studyTimeToday.first()
        val scroll = preferencesManager.scrollTimeToday.first()
        val streak = preferencesManager.currentStreak.first()
        val name = preferencesManager.userName.first()
        userRepository.syncDailyProgress(study, scroll, streak, name)
    }

    private fun showOverlay() {
        if (overlayView != null) return

        try {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            overlayView = inflater.inflate(R.layout.overlay_view, null)

            val quoteView = overlayView?.findViewById<TextView>(R.id.tvMotivationQuote)
            val closeBtn = overlayView?.findViewById<Button>(R.id.btnClose)
            
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
            
            serviceScope.launch {
                val hardcore = preferencesManager.isHardcoreModeActive.first()
                if (hardcore) {
                    closeBtn?.visibility = View.GONE
                    for (i in 5 downTo 1) {
                        if (overlayView == null) break
                        quoteView?.text = "⚠️ HARDCORE MODE: App closing in ${i}s..."
                        delay(1000)
                    }
                    if (overlayView != null) {
                        syncToFirebase()
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        removeOverlay()
                        distractionSeconds = 0
                    }
                } else {
                    val motivation = userRepository.getAiMotivation().first()
                    quoteView?.text = motivation
                    closeBtn?.visibility = View.VISIBLE
                    closeBtn?.setOnClickListener {
                        serviceScope.launch { syncToFirebase() }
                        removeOverlay()
                        distractionSeconds = 0
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TRACKING", "Error showing overlay: ${e.message}")
        }
    }

    private fun removeOverlay() {
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {}
            overlayView = null
        }
    }

    override fun onInterrupt() {
        trackingJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        removeOverlay()
    }
}
