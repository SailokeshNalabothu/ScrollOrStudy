package com.example.scrollorstudy.services

import android.app.usage.UsageStatsManager
import android.content.Context
import android.app.usage.UsageEvents
import android.util.Log

class AppUsageManager(private val context: Context) {

    fun getCurrentApp(): String? {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 5 // last 5 minutes

        val events = usageStatsManager.queryEvents(startTime, endTime)
        var lastApp: String? = null

        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                val packageName = event.packageName

                if (packageName != "com.sec.android.app.launcher" &&
                    packageName != "com.example.scrollorstudy") {
                    lastApp = packageName
                }
            }
        }

        return lastApp
    }
}
