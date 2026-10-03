package com.example.helpdesk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.helpdesk.data.Ticket
import com.example.helpdesk.data.TicketRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.example.helpdesk.data.AuthRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

class CreateTicketActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = TicketRepository()
        val authRepo = AuthRepository()
        val currentStudentId = authRepo.currentUserId ?: ""

        setContent {
            CreateTicketScreen(
                repository = repository,
                studentId = currentStudentId,
                onDone = { finish() }
            )
        }
    }
}

@Composable
fun CreateTicketScreen(
    repository: TicketRepository,
    studentId: String,
    onDone: () -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("OTHER") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = "New Ticket", subtitle = "Report an issue")

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

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

            Button(
                onClick = {
                    scope.launch {
                        repository.insertTicket(
                            Ticket(
                                studentId = studentId,
                                subject = subject,
                                description = description,
                                category = category
                            )
                        )
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Submit Ticket")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateTicketScreenPreview() {
    CreateTicketScreen(
        repository = TicketRepository(),
        studentId = "student_001",
        onDone = {}
    )
}