package com.example.helpdesk.data

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderLabel: String = "", // "Staff" or "Student", for display
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
