package com.example.helpdesk.data

data class Ticket(
    val id: String = "", // Firestore document ID, filled in after creation
    val email: String = "",
    val studentId: String = "",
    val subject: String = "",
    val description: String = "",
    val category: String = "OTHER", // ELECTRICAL, PLUMBING, IT, FURNITURE, OTHER
    val attachmentUrl: String? = null,
    val status: String = "OPEN", // OPEN, IN_PROGRESS, RESOLVED
    val staffReply: String? = null,
    val assignedStaffEmail: String? = null,
    val rating: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)