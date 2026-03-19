package com.example.scrollorstudy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.scrollorstudy.ui.theme.ScrollOrStudyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
            val useDarkMode = AppState.isDarkMode
            
            ScrollOrStudyTheme(darkTheme = useDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            if (AppState.userRole == "parent") {
                                ParentDashboardScreen(
                                    onProfileClick = { navController.navigate("profile") }
                                )
                            } else {
                                DashboardScreen(
                                    onProfileClick = { navController.navigate("profile") }
                                )
                            }
                        }
                        composable("profile") {
                            ProfileScreen(
                                onBack = { navController.popBackStack() },
                                onLogout = { logout() },
                                onDeleteAccount = { deleteAccount() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Failed to delete account: ${task.exception?.message}", Toast.LENGTH_LONG).show()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onProfileClick: () -> Unit) {
    val currentDate = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date())
    val user = FirebaseAuth.getInstance().currentUser
    var aiMessage by remember { mutableStateOf("Analyzing patterns...") }
    var aiCoachMessage by remember { mutableStateOf(AppState.cachedAiMotivation ?: "Awaiting AI Coach analysis...") }
    var focusRank by remember { mutableStateOf("Unranked") }
    var focusScore by remember { mutableStateOf(0) }

    // Fetch AI Prediction
    LaunchedEffect(user?.uid) {
        if (user != null) {
            val database = AppState.getDatabaseInstance()
            database.getReference("ai_insights").child(user.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        aiMessage = snapshot.child("message").getValue(String::class.java) ?: "Keep studying to train the AI!"
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
                
            database.getReference("ai_motivation").child(user.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val msg = snapshot.child("message").getValue(String::class.java)
                        if (msg != null) {
                            aiCoachMessage = msg
                            AppState.cachedAiMotivation = msg
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
                
            // Fetch Leaderboard Rank
            database.getReference("leaderboard").child(user.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        focusRank = snapshot.child("rank").getValue(String::class.java) ?: "Unranked"
                        focusScore = snapshot.child("score").getValue(Int::class.java) ?: 0
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Scroll or Study", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(currentDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // WELCOME MESSAGE
            val displayName = if (AppState.userName.isNotEmpty()) AppState.userName else "Student"
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = "👋 Welcome, $displayName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // RANK CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiary.copy(alpha=0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(text = "🏆", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Focus Rank", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha=0.8f))
                        Text(text = focusRank, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Score", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha=0.8f))
                        Text(text = "$focusScore", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // AI CARDS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "AI Analysis", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = aiMessage, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top=2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Recommendation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text(text = aiCoachMessage, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top=2.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // PERFORMANCE GRID
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Study Time
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(28.dp).background(Color(0xFF4CAF50).copy(alpha=0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Study", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2E7D32))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val studyMins = AppState.studyTimeToday / 60
                        val studyHrs = studyMins / 60
                        val formatStudy = if (studyHrs > 0) "${studyHrs}h ${studyMins % 60}m" else "${studyMins}m"
                        Text(text = formatStudy, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
                
                // Scroll Time
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(28.dp).background(Color(0xFFF44336).copy(alpha=0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Scroll", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFC62828))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val scrollMins = AppState.scrollTimeToday / 60
                        val scrollHrs = scrollMins / 60
                        val formatScroll = if (scrollHrs > 0) "${scrollHrs}h ${scrollMins % 60}m" else "${scrollMins}m"
                        Text(text = formatScroll, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // STREAK ROW
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(Color(0xFFFF9800).copy(alpha=0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(text = "🔥", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "${AppState.currentStreak} Day Streak", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // COMMIT BUTTON
            Button(
                onClick = { AppState.isStudyModeActive = !AppState.isStudyModeActive },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (AppState.isStudyModeActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(if (AppState.isStudyModeActive) Icons.Default.Close else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (AppState.isStudyModeActive) "Stop Study Session" else "Start Study Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // HARDCORE MODE TOGGLE
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Hardcore Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Force-close apps instead of showing popups.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.8f))
                    }
                    Switch(
                        checked = AppState.isHardcoreModeActive,
                        onCheckedChange = { 
                            AppState.isHardcoreModeActive = it 
                            AppState.save(context)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.error, checkedTrackColor = MaterialTheme.colorScheme.errorContainer)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(onProfileClick: () -> Unit) {
    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var studentStudyTime by remember { mutableStateOf(0L) }
    var studentScrollTime by remember { mutableStateOf(0L) }
    var studentStreak by remember { mutableStateOf(0) }
    var studentName by remember { mutableStateOf("Loading...") }
    val context = androidx.compose.ui.platform.LocalContext.current
    var weeklyChartUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val database = AppState.getDatabaseInstance()
        val studentRef = database.getReference("user_data").child(AppState.studentUidForParent).child(currentDate)
        val statsRef = database.getReference("user_stats").child(AppState.studentUidForParent)
        val weeklyRef = database.getReference("weekly_reports").child(AppState.studentUidForParent).child("latest_chart_url")

        studentRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                studentStudyTime = snapshot.child("studyTime").getValue(Long::class.java) ?: 0L
                studentScrollTime = snapshot.child("scrollTime").getValue(Long::class.java) ?: 0L
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        statsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                studentStreak = snapshot.child("streak").getValue(Int::class.java) ?: 0
                studentName = snapshot.child("userName").getValue(String::class.java) ?: "Student"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        weeklyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                weeklyChartUrl = snapshot.getValue(String::class.java)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Parent Monitoring", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            Text(text = "Currently Monitoring", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 16.dp))
            Text(text = studentName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(24.dp)) {
                    StatRowPro(icon = Icons.Default.Edit, label = "Study Time", seconds = studentStudyTime, color = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.height(20.dp))
                    StatRowPro(icon = Icons.Default.PlayArrow, label = "Scroll Time", seconds = studentScrollTime, color = Color(0xFFF44336))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.15f))) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔥", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "$studentStreak Day Streak", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }
            
            if (weeklyChartUrl != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📊 Weekly Performance", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        AsyncImage(
                            model = weeklyChartUrl,
                            contentDescription = "Weekly Study vs. Scroll Chart",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { 
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(weeklyChartUrl))
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Open Full Size", fontSize = 14.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Live monitoring active. Data updates in real-time.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onLogout: () -> Unit, onDeleteAccount: () -> Unit) {
    var editName by remember { mutableStateOf(AppState.userName) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val user = FirebaseAuth.getInstance().currentUser

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to permanently delete your account? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDeleteAccount() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (AppState.userRole == "parent") "Parent Profile" else "Student Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(100.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Text(text = if (editName.isNotEmpty()) editName.first().uppercase() else "U", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = editName, onValueChange = { editName = it; AppState.userName = it; AppState.save(context) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, shape = RoundedCornerShape(16.dp))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = AppState.userEmail, onValueChange = {}, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), readOnly = true, enabled = false, leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }, shape = RoundedCornerShape(16.dp))
            if (AppState.userRole == "student" && user != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Parent Link ID", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = user.uid, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), trailingIcon = { IconButton(onClick = { clipboardManager.setText(AnnotatedString(user.uid)); Toast.makeText(context, "ID Copied!", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.Share, contentDescription = "Copy") } })
                Text(text = "Share this ID with your parent to link accounts.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Dark Mode", fontSize = 16.sp)
                }
                Switch(checked = AppState.isDarkMode, onCheckedChange = { AppState.isDarkMode = it; AppState.save(context) })
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
            if (AppState.userRole == "student") {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account Permanently")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatRowPro(icon: ImageVector, label: String, seconds: Long, color: Color) {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    val timeString = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m ${secs}s"
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = color) }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Text(text = timeString, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun formatTimeCompact(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
