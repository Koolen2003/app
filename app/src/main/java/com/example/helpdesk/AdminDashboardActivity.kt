package com.example.helpdesk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.helpdesk.data.AppUser
import com.example.helpdesk.data.AuthRepository
import kotlinx.coroutines.launch

class AdminDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authRepo = AuthRepository()

        setAppContent {
            AdminDashboardScreen(
                authRepo = authRepo,
                onLogoutClick = {
                    authRepo.logout()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun AdminDashboardScreen(authRepo: AuthRepository, onLogoutClick: () -> Unit) {
    var users by remember { mutableStateOf<List<AppUser>>(emptyList()) }
    var resetEmail by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        users = authRepo.getAllUsers()
    }
    LaunchedEffect(Unit) { refresh() }

    if (showCreateDialog) {
        CreateUserDialog(
            authRepo = authRepo,
            onDismiss = { showCreateDialog = false },
            onCreated = { email ->
                showCreateDialog = false
                statusMessage = "Account created for $email"
                scope.launch { refresh() }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = "Admin Dashboard", subtitle = "Manage staff and students")

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Create User")
            }

            Text(
                "Send Password Reset",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp)
            )
            OutlinedTextField(
                value = resetEmail,
                onValueChange = { resetEmail = it },
                label = { Text("User's email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
            Button(
                onClick = {
                    scope.launch {
                        val result = authRepo.sendPasswordReset(resetEmail.trim())
                        statusMessage = if (result.isSuccess) "Reset email sent to ${resetEmail.trim()}"
                        else "Failed: ${result.exceptionOrNull()?.message}"
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Text("Send Reset Email")
            }
            statusMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                "All Users",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 20.dp)
            )
            LazyColumn(modifier = Modifier.padding(top = 4.dp).weight(1f)) {
                items(users) { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.email, style = MaterialTheme.typography.bodyMedium)
                            Text(user.role, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = user.active,
                            onCheckedChange = { newValue ->
                                scope.launch {
                                    authRepo.setUserActive(user.uid, newValue)
                                    refresh()
                                }
                            }
                        )
                    }
                }
            }

            ThemeSettingsButton(modifier = Modifier.padding(top = 8.dp))

            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Log Out")
            }
        }
    }
}

@Composable
fun CreateUserDialog(
    authRepo: AuthRepository,
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("STUDENT") }
    var error by remember { mutableStateOf<String?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Create User") },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Temporary password (min 6)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text("Role", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                Row(modifier = Modifier.padding(top = 6.dp)) {
                    listOf("STUDENT", "STAFF", "ADMIN").forEach { r ->
                        val selected = role == r
                        Text(
                            r.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (selected) Color.White else Color(0xFF475569),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .background(
                                    if (selected) IndigoPrimary else Color(0xFFF1F5F9),
                                    RoundedCornerShape(50)
                                )
                                .clickable { role = r }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                error?.let {
                    Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isCreating,
                onClick = {
                    if (email.isBlank() || password.length < 6) {
                        error = "Enter an email and a password of at least 6 characters"
                        return@Button
                    }
                    isCreating = true
                    error = null
                    scope.launch {
                        val result = authRepo.createUserAsAdmin(email.trim(), password, role)
                        isCreating = false
                        result.onSuccess { onCreated(email.trim()) }
                        result.onFailure { error = it.message ?: "Could not create account" }
                    }
                }
            ) { Text(if (isCreating) "Creating..." else "Create") }
        },
        dismissButton = {
            Button(enabled = !isCreating, onClick = onDismiss) { Text("Cancel") }
        }
    )
}