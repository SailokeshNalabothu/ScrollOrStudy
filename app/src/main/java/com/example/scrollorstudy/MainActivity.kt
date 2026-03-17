package com.example.scrollorstudy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollorstudy.ui.theme.ScrollOrStudyTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        AppState.load(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
        
        checkPermissions()
        
        setContent {
            ScrollOrStudyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppState.load(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION", "Notification permission granted")
            } else {
                Log.d("PERMISSION", "Notification permission denied")
            }
        }
    }

    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
        
        try {
            val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val stats = usageStatsManager.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, System.currentTimeMillis() - 1000 * 10, System.currentTimeMillis())
            if (stats == null || stats.isEmpty()) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        } catch (e: Exception) {
            Log.e("PERMISSION", "Error checking usage stats permission", e)
        }
    }
}

@Composable
fun DashboardScreen() {
    val currentDate = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date())
    
    val quotes = listOf(
        "Small steps every day = Big success",
        "Focus on being productive, not busy.",
        "Your future self will thank you for today.",
        "Discipline is choosing between what you want now and what you want most."
    )
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val dailyQuote = quotes[dayOfYear % quotes.size]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // 🥇 HEADER SECTION
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "👋 Welcome back!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Stay Focused 🎯",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = currentDate,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 🥈 TODAY'S STATS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "📊 Today Stats",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                StatRow(icon = "📖", label = "Study Time", seconds = AppState.studyTimeToday, color = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.height(12.dp))
                StatRow(icon = "📱", label = "Scroll Time", seconds = AppState.scrollTimeToday, color = Color(0xFFF44336))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🥉 STREAK SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🔥", fontSize = 32.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Streak: ${AppState.currentStreak} days",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF9800)
                    )
                    Text(
                        text = "You're on fire! Keep it up.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 💡 MOTIVATION SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "💡 Quote:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "\"$dailyQuote\"",
                    fontSize = 16.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ▶ START STUDY BUTTON
        Button(
            onClick = { AppState.isStudyModeActive = !AppState.isStudyModeActive },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (AppState.isStudyModeActive) Color(0xFFF44336) else Color(0xFF4CAF50)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(
                text = if (AppState.isStudyModeActive) "🛑 Stop Study" else "▶ Start Study",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun StatRow(icon: String, label: String, seconds: Long, color: Color) {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    val timeString = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m ${secs}s"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = timeString,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    ScrollOrStudyTheme {
        DashboardScreen()
    }
}
