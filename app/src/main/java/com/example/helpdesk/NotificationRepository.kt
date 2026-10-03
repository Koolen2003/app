package com.example.helpdesk.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notifRef = db.collection("notifications")

    fun getNotificationsForUser(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = notifRef
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }.catch { emit(emptyList()) }

    suspend fun send(userId: String, message: String, ticketId: String) {
        notifRef.add(Notification(userId = userId, message = message, ticketId = ticketId)).await()
    }

    suspend fun markAsRead(notificationId: String) {
        notifRef.document(notificationId).update("read", true).await()
    }
}