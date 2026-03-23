package com.example.scrollorstudy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollorstudy.ui.theme.ScrollOrStudyTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private val preferencesManager by lazy { (application as ScrollOrStudyApplication).container.preferencesManager }
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var launcher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startMainActivity()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        setContent {
            ScrollOrStudyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoginScreen()
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    lifecycleScope.launch {
                        preferencesManager.setUserRole("student")
                        startMainActivity()
                    }
                } else {
                    Toast.makeText(this, "Google login failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    @Composable
    fun LoginScreen() {
        var parentId by remember { mutableStateOf("") }
        var parentPassword by remember { mutableStateOf("") }
        var showParentLogin by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            com.example.scrollorstudy.ui.components.AnimatedMeshBackground()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🎓 Scroll or Study", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sign in to track your progress and build streaks",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
            
                        Spacer(modifier = Modifier.height(40.dp))
            
                        if (!showParentLogin) {
                            Button(
                                onClick = { 
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        launcher.launch(googleSignInClient.signInIntent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            TextButton(onClick = { showParentLogin = true }) {
                                Text("Parent? Access monitoring here", fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Text(text = "Parent Portal", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            OutlinedTextField(
                                value = parentId,
                                onValueChange = { parentId = it },
                                label = { Text("Enter Student ID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = parentPassword,
                                onValueChange = { parentPassword = it },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    if (parentPassword == "welcome") {
                                        lifecycleScope.launch {
                                            preferencesManager.setUserRole("parent")
                                            preferencesManager.setStudentUidForParent(parentId.trim())
                                            startMainActivity()
                                        }
                                    } else {
                                        Toast.makeText(this@LoginActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            ) {
                                Text("Login as Parent", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { showParentLogin = false }) {
                                Text("Back to Student Login")
                            }
                        }
                    }
                }
            }
        }
    }
}
