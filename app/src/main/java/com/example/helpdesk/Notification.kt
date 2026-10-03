package com.example.helpdesk.data

data class Notification(
    val id: String = "",
    val userId: String = "", // recipient's UID
    val message: String = "",
    val ticketId: String = "",
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)