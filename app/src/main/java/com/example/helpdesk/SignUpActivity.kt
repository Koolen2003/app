package com.example.helpdesk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.helpdesk.data.AuthRepository
import kotlinx.coroutines.launch

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authRepo = AuthRepository()

        setContent {
            SignUpScreen(
                authRepo = authRepo,
                onSignUpSuccess = { role ->
                    val destination = when (role) {
                        "STAFF" -> StaffDashboardActivity::class.java
                        "ADMIN" -> AdminDashboardActivity::class.java
                        else -> MainActivity::class.java
                    }
                    val intent = Intent(this, destination)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                },
                onGoToLogin = {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun SignUpScreen(
    authRepo: AuthRepository,
    onSignUpSuccess: (String) -> Unit,
    onGoToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("STUDENT") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = "Create Account", subtitle = "Join the Help Desk")

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Text("I am a:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Button(
                    onClick = { role = "STUDENT" },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(if (role == "STUDENT") "✓ Student" else "Student")
                }
                Button(
                    onClick = { role = "STAFF" },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(if (role == "STAFF") "✓ Staff" else "Staff")
                }

            }

            errorMessage?.let {
                Text(it, color = androidx.compose.ui.graphics.Color.Red, modifier = Modifier.padding(top = 8.dp))
            }

            Button(
                onClick = {
                    scope.launch {
                        val result = authRepo.signUp(email, password, role)
                        result.onSuccess { onSignUpSuccess(role) }
                        result.onFailure { errorMessage = it.message ?: "Sign up failed" }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Sign Up")
            }

            TextButton(
                onClick = onGoToLogin,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Already have an account? Log in")
            }
        }
    }
}