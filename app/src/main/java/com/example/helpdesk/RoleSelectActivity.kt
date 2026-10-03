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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class RoleSelectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoleSelectScreen(
                onStudentClick = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
                onStaffClick = {
                    startActivity(Intent(this, StaffDashboardActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun RoleSelectScreen(
    onStudentClick: () -> Unit,
    onStaffClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to Help Desk", style = MaterialTheme.typography.headlineMedium)

        Button(
            onClick = onStudentClick,
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
        ) {
            Text("I'm a Student")
        }

        Button(
            onClick = onStaffClick,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("I'm Staff")
        }
    }
}