package com.example.helpdesk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.helpdesk.data.AuthRepository
import com.example.helpdesk.data.NotificationRepository
import com.example.helpdesk.data.TicketRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview

class TicketDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = TicketRepository()
        val notifRepo = NotificationRepository()
        val authRepo = AuthRepository()
        val ticketId = intent.getStringExtra("ticketId") ?: ""

        setContent {
            TicketDetailScreen(
                repository = repository,
                notifRepo = notifRepo,
                ticketId = ticketId,
                currentStaffEmail = authRepo.currentUserEmail ?: "",
                onDone = { finish() },
                onOpenChat = { tId ->
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("ticketId", tId)
                    intent.putExtra("senderLabel", "Staff")
                    startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun TicketDetailScreen(
    repository: TicketRepository,
    notifRepo: NotificationRepository,
    ticketId: String,
    currentStaffEmail: String,
    onDone: () -> Unit,
    onOpenChat: (String) -> Unit = {}
) {
    val ticketFlow = remember(ticketId) { repository.getTicketById(ticketId) }
    val ticket by ticketFlow.collectAsStateWithLifecycle(initialValue = null)
    var status by remember { mutableStateOf("OPEN") }
    var reply by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(ticket) {
        if (!initialized && ticket != null) {
            status = ticket!!.status
            reply = ticket!!.staffReply ?: ""
            initialized = true
        }
    }

    val current = ticket
    if (current == null) {
        Text("Loading...", modifier = Modifier.padding(16.dp))
        return
    }

    val isUnclaimed = current.assignedStaffEmail.isNullOrBlank()
    val isClaimedByMe = current.assignedStaffEmail == currentStaffEmail
    val canEdit = isClaimedByMe

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = "Ticket Detail", subtitle = current.subject)

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(current.description, modifier = Modifier.padding(top = 8.dp))
            Text("From: ${current.studentId}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))

            if (isUnclaimed) {
                Button(
                    onClick = {
                        scope.launch {
                            repository.updateTicket(current.copy(assignedStaffEmail = currentStaffEmail))
                            notifRepo.send(
                                userId = current.studentId,
                                message = "Your ticket '${current.subject}' was claimed by staff",
                                ticketId = current.id
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text("Claim This Ticket")
                }
            } else if (isClaimedByMe) {
                Text(
                    "You claimed this ticket",
                    color = IndigoPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Button(
                    onClick = { onOpenChat(current.id) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("💬 Open Chat")
                }
            } else {
                Text(
                    "Claimed by: ${current.assignedStaffEmail} — view only",
                    color = Color(0xFF64748B),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Text("Status", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            listOf("OPEN", "IN_PROGRESS", "RESOLVED").forEach { option ->
                Button(
                    onClick = { if (canEdit) status = option },
                    enabled = canEdit,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(if (status == option) "✓ $option" else option)
                }
            }

            OutlinedTextField(
                value = reply,
                onValueChange = { if (canEdit) reply = it },
                label = { Text("Reply to student") },
                enabled = canEdit,
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 16.dp)
            )

            if (canEdit) {
                Button(
                    onClick = {
                        scope.launch {
                            repository.updateTicket(current.copy(status = status, staffReply = reply))
                            notifRepo.send(
                                userId = current.studentId,
                                message = "Your ticket '${current.subject}' was updated: $status",
                                ticketId = current.id
                            )
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TicketDetailScreenPreview() {
    TicketDetailScreen(
        repository = TicketRepository(),
        notifRepo = NotificationRepository(),
        ticketId = "1",
        currentStaffEmail = "staff@test.com",
        onDone = {}
    )
}