package com.example.helpdesk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.helpdesk.data.AuthRepository
import com.example.helpdesk.data.NotificationRepository
import com.example.helpdesk.data.Ticket
import com.example.helpdesk.data.TicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.verticalScroll

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = TicketRepository()
        val authRepo = AuthRepository()
        val notifRepo = NotificationRepository()
        val currentStudentId = authRepo.currentUserId ?: ""
        val currentEmail = authRepo.currentUserEmail ?: ""
        NotificationWatcher.start(this, currentStudentId)

        setContent {
            StudentHomeScreen(
                ticketsFlow = repository.getTicketsForStudent(currentStudentId),
                repository = repository,
                notifRepo = notifRepo,
                authRepo = authRepo,
                studentId = currentStudentId,
                studentEmail = currentEmail,
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
fun StudentHomeScreen(
    ticketsFlow: Flow<List<Ticket>>,
    repository: TicketRepository,
    notifRepo: NotificationRepository,
    authRepo: AuthRepository,
    studentId: String,
    studentEmail: String,
    onLogoutClick: () -> Unit,
    onBellClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(StudentTab.ISSUES) }
    val tickets by ticketsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current
    val notifFlow = remember(studentId) { notifRepo.getNotificationsForUser(studentId) }
    val unreadCount by notifFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    NotificationPermissionPrompt()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                StudentTab.HOME -> HomeTab(
                    tickets = tickets,
                    repository = repository,
                    unread = unreadCount.count { !it.read },
                    onBellClick = onBellClick,
                    onSeeAllClick = { selectedTab = StudentTab.ISSUES },
                    onNewClick = { selectedTab = StudentTab.NEW }
                )
                StudentTab.ISSUES -> MyIssuesTab(
                    tickets,
                    repository,
                    onChatClick = { ticketId ->
                        val intent = android.content.Intent(context, ChatActivity::class.java)
                        intent.putExtra("ticketId", ticketId)
                        intent.putExtra("senderLabel", "Student")
                        context.startActivity(intent)
                    }
                )
                StudentTab.NEW -> NewTicketTab(
                    repository = repository,
                    notifRepo = notifRepo,
                    authRepo = authRepo,
                    studentId = studentId,
                    onDone = { selectedTab = StudentTab.ISSUES }
                )
                StudentTab.PROFILE -> ProfileTab(
                    email = studentEmail,
                    onLogoutClick = onLogoutClick
                )
            }
        }
        StudentTabBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
    }
}

@Composable
fun HomeTab(
    tickets: List<Ticket>,
    repository: TicketRepository,
    unread: Int,
    onBellClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    onNewClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(
            title = "Welcome back",
            subtitle = "Here's your overview",
            trailingContent = { NotificationBell(unreadCount = unread, onClick = onBellClick) }
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            StatsPanel(tickets)

            Button(onClick = onNewClick, modifier = Modifier.fillMaxWidth()) {
                Text("+ Report a new issue")
            }

            Text(
                "Recent Tickets",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tickets.take(3)) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        allowRating = true,
                        onRate = { stars ->
                            scope.launch {
                                repository.updateTicket(ticket.copy(rating = stars))
                            }
                        }
                    )
                }
            }

            Button(
                onClick = onSeeAllClick,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("See All Tickets")
            }
        }
    }
}

@Composable
fun MyIssuesTab(tickets: List<Ticket>, repository: TicketRepository, onChatClick: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = "My Tickets", subtitle = "Track your submitted issues")

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tickets) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        allowRating = true,
                        onRate = { stars ->
                            scope.launch {
                                repository.updateTicket(ticket.copy(rating = stars))
                            }
                        },
                        onChatClick = if (!ticket.assignedStaffEmail.isNullOrBlank()) {
                            { onChatClick(ticket.id) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
fun NewTicketTab(
    repository: TicketRepository,
    notifRepo: NotificationRepository,
    authRepo: AuthRepository,
    studentId: String,
    onDone: () -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("OTHER") }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showCameraRationale by remember { mutableStateOf(false) }
    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            photoBase64 = uriToCompressedBase64(context, cameraUri!!)
        }
    }

    fun launchCamera() {
        val file = java.io.File.createTempFile("ticket_photo_", ".jpg", context.cacheDir)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            val canAskAgain = activity?.let {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    it, android.Manifest.permission.CAMERA
                )
            } ?: true
            if (!canAskAgain) {
                showPermanentlyDeniedDialog = true
            }
        }
    }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            photoBase64 = uriToCompressedBase64(context, uri)
        }
    }

    fun onCameraButtonClick() {
        val alreadyGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            launchCamera()
        } else {
            showCameraRationale = true
        }
    }

    if (showCameraRationale) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCameraRationale = false },
            title = { Text("Camera Permission") },
            text = { Text("HelpDesk needs camera access so you can attach a photo of the issue to your ticket.") },
            confirmButton = {
                Button(onClick = {
                    showCameraRationale = false
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }) { Text("Allow") }
            },
            dismissButton = {
                Button(onClick = { showCameraRationale = false }) { Text("Not Now") }
            }
        )
    }

    if (showPermanentlyDeniedDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPermanentlyDeniedDialog = false },
            title = { Text("Camera Blocked") },
            text = { Text("Camera access was denied. Please enable it manually in Settings > Apps > HelpDesk > Permissions.") },
            confirmButton = {
                Button(onClick = {
                    showPermanentlyDeniedDialog = false
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                Button(onClick = { showPermanentlyDeniedDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = "New Ticket", subtitle = "Report an issue")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(16.dp)
        ) {

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Describe the issue") },
                modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp)
            )

            Text("Category", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            LazyRow(modifier = Modifier.padding(top = 8.dp)) {
                items(CATEGORIES) { cat ->
                    val selected = category == cat.key
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                if (selected) IndigoPrimary else Color(0xFFF1F5F9),
                                RoundedCornerShape(50)
                            )
                            .clickable { category = cat.key }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "${cat.icon} ${cat.label}",
                            color = if (selected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            Text("Photo (optional)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))

            if (photoBase64 != null) {
                val bitmap = remember(photoBase64) { base64ToBitmap(photoBase64!!) }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Photo preview",
                        modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Button(
                    onClick = { onCameraButtonClick() },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Text("📷 Camera")
                }
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Text("🖼 Gallery")
                }
            }

            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    isSubmitting = true
                    scope.launch {
                        repository.insertTicket(
                            Ticket(
                                studentId = studentId,
                                subject = subject,
                                description = description,
                                category = category,
                                attachmentUrl = photoBase64
                            )
                        )
                        val staffUsers = authRepo.getAllUsers().filter { it.role == "STAFF" }
                        staffUsers.forEach { staff ->
                            notifRepo.send(
                                userId = staff.uid,
                                message = "New ticket submitted: $subject",
                                ticketId = ""
                            )
                        }
                        subject = ""
                        description = ""
                        category = "OTHER"
                        photoBase64 = null
                        isSubmitting = false
                        onDone()
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp)
            ) {
                Text(if (isSubmitting) "Submitting..." else "Submit Ticket")
            }
        }
    }
}
@Composable
fun ProfileTab(email: String, onLogoutClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = "Profile", subtitle = "Your account")

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Email", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            Text(email, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))

            Text("Role", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            Text("Student", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 24.dp))

            Button(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth()) {
                Text("Log Out")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudentHomeScreenPreview() {
    val sampleTickets = listOf(
        Ticket(id = "1", subject = "Broken chain", status = "OPEN", studentId = "student_001"),
        Ticket(id = "2", subject = "Projector not working", status = "IN_PROGRESS", studentId = "student_002", staffReply = "Technician notified"),
        Ticket(id = "3", subject = "Wifi issue in library", status = "RESOLVED", studentId = "student_003")
    )
    StudentHomeScreen(
        ticketsFlow = kotlinx.coroutines.flow.flowOf(sampleTickets),
        repository = TicketRepository(),
        notifRepo = NotificationRepository(),
        authRepo = AuthRepository(),
        studentId = "student_001",
        studentEmail = "student@test.com",
        onLogoutClick = {},
        onBellClick = {}
    )
}