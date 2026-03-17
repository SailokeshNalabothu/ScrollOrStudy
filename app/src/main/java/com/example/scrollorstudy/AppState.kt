package com.example.scrollorstudy

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
            // It's a new day. Evaluate yesterday's performance for streak update.
            if (lastDate == yesterday) {
                val lastStudy = prefs.getLong(KEY_STUDY_TIME, 0L)
                val lastScroll = prefs.getLong(KEY_SCROLL_TIME, 0L)
                
                if (lastStudy > lastScroll && lastStudy > 0) {
                    currentStreak++
                } else {
                    currentStreak = 0
                }
            } else if (lastDate != "") {
                // User missed more than one day
                currentStreak = 0
            }
            
            // Reset daily stats
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
    }
}
