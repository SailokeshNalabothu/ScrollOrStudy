package com.example.scrollorstudy

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    private const val PREFS_NAME = "scroll_study_prefs"
    private const val KEY_SCROLL_TIME = "scroll_time"
    private const val KEY_STUDY_TIME = "study_time"
    private const val KEY_STREAK = "current_streak"
    private const val KEY_LAST_DATE = "last_date"

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
        
        currentStreak = prefs.getInt(KEY_STREAK, 0)

        if (lastDate == today) {
            scrollTimeToday = prefs.getLong(KEY_SCROLL_TIME, 0L)
            studyTimeToday = prefs.getLong(KEY_STUDY_TIME, 0L)
        } else {
            // New day detected.
            if (lastDate == yesterday) {
                val lastStudy = prefs.getLong(KEY_STUDY_TIME, 0L)
                val lastScroll = prefs.getLong(KEY_SCROLL_TIME, 0L)
                
                if (lastStudy > lastScroll && lastStudy > 0) {
                    currentStreak++
                } else {
                    currentStreak = 0
                }
                
                // Final sync for the completed day
                syncToFirebase(lastDate ?: "unknown", lastStudy, lastScroll)
            } else if (!lastDate.isNullOrEmpty()) {
                currentStreak = 0
            }
            
            // Reset for the new day
            scrollTimeToday = 0L
            studyTimeToday = 0L
            isStudyModeActive = false
            save(context)
        }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong(KEY_SCROLL_TIME, scrollTimeToday)
            putLong(KEY_STUDY_TIME, studyTimeToday)
            putInt(KEY_STREAK, currentStreak)
            putString(KEY_LAST_DATE, getTodayDate())
            apply()
        }
        
        // Sync current progress to Firebase
        syncToFirebase(getTodayDate(), studyTimeToday, scrollTimeToday)
    }

    private fun syncToFirebase(date: String, studyTime: Long, scrollTime: Long) {
        try {
            Log.d("FIREBASE", "Sending data: Study=$studyTime Scroll=$scrollTime")
            
            val database = FirebaseDatabase.getInstance()
            val myRef = database.getReference("user_data").child(date)
            
            val data = mapOf(
                "studyTime" to studyTime,
                "scrollTime" to scrollTime,
                "date" to date,
                "streak" to currentStreak,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            myRef.setValue(data).addOnFailureListener { e ->
                Log.e("FIREBASE", "Failed to sync data: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("FIREBASE", "Firebase error: ${e.message}")
        }
    }
}
