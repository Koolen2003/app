package com.example.helpdesk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.helpdesk.data.AuthRepository
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authRepo = AuthRepository()

        // If already logged in, skip straight past login
        if (authRepo.isLoggedIn) {
            lifecycleScope.launch {
                val role = authRepo.getCurrentUserRole()
                val destination = when (role) {
                    "STAFF" -> StaffDashboardActivity::class.java
                    "ADMIN" -> AdminDashboardActivity::class.java
                    else -> MainActivity::class.java
                }
                startActivity(Intent(this@LoginActivity, destination))
                finish()
            }
        }

        setAppContent {
            LoginScreen(
                authRepo = authRepo,
                onLoginSuccess = { role ->
                    val destination = when (role) {
                        "STAFF" -> StaffDashboardActivity::class.java
                        "ADMIN" -> AdminDashboardActivity::class.java
                        else -> MainActivity::class.java
                    }
                    val intent = Intent(this, destination)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                },
                onGoToSignUp = {
                    startActivity(Intent(this, SignUpActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun LoginScreen(
    authRepo: AuthRepository,
    onLoginSuccess: (String) -> Unit,
    onGoToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun attemptLogin() {
        if (isLoading) return
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Enter your email and password"
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = authRepo.login(email.trim(), password)
            result.onSuccess {
                val role = authRepo.getCurrentUserRole() ?: "STUDENT"
                onLoginSuccess(role)
            }
            result.onFailure { errorMessage = it.message ?: "Login failed" }
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = "Help Desk", subtitle = "Sign in to continue")

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        // both filled -> log in, otherwise move to the next box
                        if (email.isNotBlank() && password.isNotBlank()) attemptLogin()
                        else focusManager.moveFocus(FocusDirection.Down)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (email.isBlank()) focusManager.moveFocus(FocusDirection.Up)
                        else attemptLogin()
                    }
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            errorMessage?.let {
                Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }

            Button(
                onClick = { attemptLogin() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text(if (isLoading) "Signing in..." else "Log In")
            }

            TextButton(
                onClick = onGoToSignUp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Don't have an account? Sign up")
            }

            ThemeSettingsButton(modifier = Modifier.padding(top = 8.dp))
        }
    }
}