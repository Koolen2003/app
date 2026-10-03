package com.example.helpdesk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.helpdesk.data.Ticket
import com.example.helpdesk.data.TicketRepository
import com.example.helpdesk.data.NotificationRepository
import androidx.compose.ui.tooling.preview.Preview
import com.example.helpdesk.data.AuthRepository

class StaffDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = TicketRepository()
        val notifRepo = NotificationRepository()
        val authRepo = AuthRepository()
        val staffId = authRepo.currentUserId ?: ""
        NotificationWatcher.start(this, staffId)

        setContent {
            StaffDashboardScreen(
                ticketsFlow = repository.getAllTickets(),
                notifRepo = notifRepo,
                staffId = staffId,
                onTicketClick = { ticketId ->
                    val intent = Intent(this, TicketDetailActivity::class.java)
                    intent.putExtra("ticketId", ticketId)
                    startActivity(intent)
                },
                onLogoutClick = {
                    NotificationWatcher.stop()
                    authRepo.logout()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                },
                onBellClick = {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                }
            )
        }
    }
}

@Composable
fun StaffDashboardScreen(
    ticketsFlow: kotlinx.coroutines.flow.Flow<List<Ticket>>,
    notifRepo: NotificationRepository,
    staffId: String,
    onTicketClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
    onBellClick: () -> Unit
) {
    val tickets by ticketsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedCategory by remember { mutableStateOf("ALL") }
    val notifFlow = remember(staffId) { notifRepo.getNotificationsForUser(staffId) }
    val unreadCount by notifFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val filteredTickets = if (selectedCategory == "ALL") tickets
    else tickets.filter { it.category == selectedCategory }
    NotificationPermissionPrompt()

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(
            title = "All Tickets",
            subtitle = "Staff dashboard",
            trailingContent = {
                NotificationBell(unreadCount = unreadCount.count { !it.read }, onClick = onBellClick)
            }
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            StatsPanel(tickets)

            LazyRow(modifier = Modifier.padding(bottom = 8.dp)) {
                item {
                    CategoryChip(label = "All", icon = "📋", selected = selectedCategory == "ALL") {
                        selectedCategory = "ALL"
                    }
                }
                items(CATEGORIES) { cat ->
                    CategoryChip(
                        label = cat.label,
                        icon = cat.icon,
                        selected = selectedCategory == cat.key
                    ) {
                        selectedCategory = cat.key
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredTickets) { ticket ->
                    StaffTicketRow(ticket, onClick = { onTicketClick(ticket.id) })
                }
            }

            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, icon: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(end = 8.dp)
            .background(
                if (selected) IndigoPrimary else Color(0xFFF1F5F9),
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text("$icon $label", color = if (selected) Color.White else Color(0xFF475569))
    }
}

@Composable
fun StaffTicketRow(ticket: Ticket, onClick: () -> Unit) {
    Box(modifier = Modifier.clickable { onClick() }) {
        TicketCard(ticket, subtitle = "From: ${ticket.studentId}")
    }
}

@Preview(showBackground = true)
@Composable
fun StaffDashboardScreenPreview() {
    val sampleTickets = listOf(
        Ticket(id = "1", subject = "Broken chain", status = "OPEN", studentId = "student_001", category = "OTHER"),
        Ticket(id = "2", subject = "Projector not working", status = "IN_PROGRESS", studentId = "student_002", category = "IT"),
        Ticket(id = "3", subject = "Wifi issue", status = "RESOLVED", studentId = "student_003", category = "IT")
    )
    StaffDashboardScreen(
        ticketsFlow = kotlinx.coroutines.flow.flowOf(sampleTickets),
        notifRepo = NotificationRepository(),
        staffId = "staff_001",
        onTicketClick = {},
        onLogoutClick = {},
        onBellClick = {}
    )
}