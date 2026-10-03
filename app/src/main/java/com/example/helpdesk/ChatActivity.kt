package com.example.helpdesk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.helpdesk.data.AuthRepository
import com.example.helpdesk.data.ChatMessage
import com.example.helpdesk.data.NotificationRepository
import com.example.helpdesk.data.TicketRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalDensity

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = TicketRepository()
        val notifRepo = NotificationRepository()
        val authRepo = AuthRepository()
        val ticketId = intent.getStringExtra("ticketId") ?: ""
        val senderLabel = intent.getStringExtra("senderLabel") ?: "User"

        setContent {
            ChatScreen(
                repository = repository,
                notifRepo = notifRepo,
                authRepo = authRepo,
                ticketId = ticketId,
                senderId = authRepo.currentUserId ?: "",
                senderLabel = senderLabel
            )
        }
    }
}

@Composable
fun ChatScreen(
    repository: TicketRepository,
    notifRepo: NotificationRepository,
    authRepo: AuthRepository,
    ticketId: String,
    senderId: String,
    senderLabel: String
) {
    val messagesFlow = remember(ticketId) { repository.getMessages(ticketId) }
    val messages by messagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(messages.size, imeVisible) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        AppHeader(title = "Chat", subtitle = "Ticket conversation")

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(msg, isMine = msg.senderId == senderId)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        val text = input.trim()
                        input = ""
                        scope.launch {
                            repository.sendMessage(ticketId, senderId, senderLabel, text)

                            // notify the other person in the conversation
                            val ticket = repository.getTicketById(ticketId).first()
                            if (ticket != null) {
                                val recipientId = if (senderLabel == "Staff") {
                                    ticket.studentId
                                } else {
                                    authRepo.getAllUsers()
                                        .firstOrNull { it.email == ticket.assignedStaffEmail }
                                        ?.uid
                                }
                                if (!recipientId.isNullOrBlank()) {
                                    notifRepo.send(
                                        userId = recipientId,
                                        message = "New message on '${ticket.subject}': ${text.take(80)}",
                                        ticketId = ticketId
                                    )
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isMine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (isMine) IndigoPrimary else Color(0xFFF1F5F9),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (!isMine) {
                Text(message.senderLabel, fontSize = 10.sp, color = Color(0xFF64748B))
            }
            Text(message.text, color = if (isMine) Color.White else Color(0xFF1E293B))
        }
    }
}