package com.example.scrollorstudy

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppState {
    var currentApp by mutableStateOf("")
    
    // Time in seconds
    var scrollTimeToday by mutableStateOf(0L)
    var studyTimeToday by mutableStateOf(0L)
    
    // Manual Study Mode
    var isStudyModeActive by mutableStateOf(false)

    // Streak
    var currentStreak by mutableStateOf(0)
    
    // Dark Mode
    var isDarkMode by mutableStateOf(false)

    // User Profile
    var userName by mutableStateOf("")
    var userEmail by mutableStateOf("")
    var userRole by mutableStateOf("student") // "student" or "parent"
    var studentUidForParent by mutableStateOf("") // If parent, who are they watching?
    
    // Sync Status for UI feedback
    var lastSyncStatus by mutableStateOf("Ready")
    var cachedAiMotivation: String? = null

    const val MIN_STUDY_TIME = 1800L // 30 minutes threshold
    private const val DATABASE_URL = "https://scrollorstudy-default-rtdb.asia-southeast1.firebasedatabase.app"

    private const val PREFS_NAME = "scroll_study_prefs"
    private const val KEY_SCROLL_TIME = "scroll_time"
    private const val KEY_STUDY_TIME = "study_time"
    private const val KEY_STREAK = "current_streak"
    private const val KEY_LAST_DATE = "last_date"
    private const val KEY_LAST_STUDY_DATE = "last_study_date"
    private const val KEY_DARK_MODE = "is_dark_mode"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_STUDENT_UID = "student_uid"

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getYesterdayDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayDate()
        val yesterday = getYesterdayDate()
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        val lastStudyDate = prefs.getString(KEY_LAST_STUDY_DATE, "")
        
        currentStreak = prefs.getInt(KEY_STREAK, 0)
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        userName = prefs.getString(KEY_USER_NAME, "") ?: ""
        userRole = prefs.getString(KEY_USER_ROLE, "student") ?: "student"
        studentUidForParent = prefs.getString(KEY_STUDENT_UID, "") ?: ""
        
        val user = FirebaseAuth.getInstance().currentUser
        userEmail = user?.email ?: ""
        if (userName.isEmpty()) {
            userName = user?.displayName ?: ""
        }

        if (lastDate == today) {
            scrollTimeToday = prefs.getLong(KEY_SCROLL_TIME, 0L)
            studyTimeToday = prefs.getLong(KEY_STUDY_TIME, 0L)
        } else {
            // New day detected. Reset stats but sync yesterday first if student.
            if (lastDate == yesterday) {
                val lastStudy = prefs.getLong(KEY_STUDY_TIME, 0L)
                val lastScroll = prefs.getLong(KEY_SCROLL_TIME, 0L)
                if (lastStudy > lastScroll && lastStudy > 0) {
                    currentStreak++
                } else {
                    currentStreak = 0
                }
                if (userRole == "student") {
                    syncToFirebase(lastDate ?: "unknown", lastStudy, lastScroll)
                }
            } else if (!lastDate.isNullOrEmpty()) {
                currentStreak = 0
            }
            scrollTimeToday = 0L
            studyTimeToday = 0L
            isStudyModeActive = false
            save(context)
        }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayDate()
        val lastStudyDate = prefs.getString(KEY_LAST_STUDY_DATE, "")

        // Streak check
        if (studyTimeToday >= MIN_STUDY_TIME) {
            if (lastStudyDate != today) {
                if (lastStudyDate == getYesterdayDate()) {
                    currentStreak++
                } else {
                    currentStreak = 1
                }
                prefs.edit().putString(KEY_LAST_STUDY_DATE, today).apply()
            }
        }

        prefs.edit().apply {
            putLong(KEY_SCROLL_TIME, scrollTimeToday)
            putLong(KEY_STUDY_TIME, studyTimeToday)
            putInt(KEY_STREAK, currentStreak)
            putString(KEY_LAST_DATE, today)
            putBoolean(KEY_DARK_MODE, isDarkMode)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_ROLE, userRole)
            putString(KEY_STUDENT_UID, studentUidForParent)
            apply()
        }
        
        if (userRole == "student") {
            syncToFirebase(today, studyTimeToday, scrollTimeToday)
        }
    }

    private fun syncToFirebase(date: String, studyTime: Long, scrollTime: Long) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            lastSyncStatus = "Not Logged In"
            return
        }
        val uid = user.uid

        try {
            lastSyncStatus = "Syncing..."
            val database = FirebaseDatabase.getInstance(DATABASE_URL)
            
            // 1. Update Daily Progress
            val data = mapOf(
                "studyTime" to studyTime,
                "scrollTime" to scrollTime,
                "date" to date,
                "streak" to currentStreak,
                "lastUpdated" to System.currentTimeMillis(),
                "userName" to userName
            )
            
            database.getReference("user_data").child(uid).child(date).setValue(data)
                .addOnSuccessListener {
                    lastSyncStatus = "Synced ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
                    Log.d("FIREBASE_SYNC", "Sync Success for $uid")
                }
                .addOnFailureListener {
                    lastSyncStatus = "Sync Failed"
                    Log.e("FIREBASE_SYNC", "Sync Error: ${it.message}")
                }
            
            // 2. Update Persistent Stats
            val statsRef = database.getReference("user_stats").child(uid)
            statsRef.child("streak").setValue(currentStreak)
            statsRef.child("lastStudyDate").setValue(date)
            statsRef.child("userName").setValue(userName)
            statsRef.child("role").setValue("student")

        } catch (e: Exception) {
            lastSyncStatus = "Error"
            Log.e("FIREBASE_SYNC", "Initialization Error: ${e.message}")
        }
    }
    
    fun getDatabaseInstance(): FirebaseDatabase {
        return FirebaseDatabase.getInstance(DATABASE_URL)
    }
}
