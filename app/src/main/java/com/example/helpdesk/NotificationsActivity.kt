package com.example.helpdesk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.helpdesk.data.AuthRepository
import com.example.helpdesk.data.Notification
import com.example.helpdesk.data.NotificationRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember

class NotificationsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notifRepo = NotificationRepository()
        val authRepo = AuthRepository()
        val userId = authRepo.currentUserId ?: ""

        setContent {
            NotificationsScreen(
                notifRepo = notifRepo,
                userId = userId
            )
        }
    }
}

@Composable
fun NotificationsScreen(notifRepo: NotificationRepository, userId: String) {
    val notifFlow = remember(userId) { notifRepo.getNotificationsForUser(userId) }
    val notifications by notifFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = "Notifications", subtitle = "${notifications.count { !it.read }} unread")

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (notifications.isEmpty()) {
                Text("No notifications yet")
            } else {
                LazyColumn {
                    items(notifications, key = { it.id }) { notif ->
                        NotificationListItem(notification = notif) {
                            if (!notif.read) {
                                scope.launch { notifRepo.markAsRead(notif.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationPermissionPrompt() {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return

    val context = androidx.compose.ui.platform.LocalContext.current
    val show = remember {
        androidx.compose.runtime.mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { show.value = false }

    if (show.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { show.value = false },
            title = { Text("Notifications") },
            text = { Text("Allow HelpDesk to notify you when a ticket is submitted, claimed or updated.") },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Allow") }
            },
            dismissButton = {
                androidx.compose.material3.Button(onClick = { show.value = false }) { Text("Not Now") }
            }
        )
    }
}