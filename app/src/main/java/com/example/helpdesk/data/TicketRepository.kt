package com.example.helpdesk.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.catch

class TicketRepository {
    private val db = FirebaseFirestore.getInstance()
    private val ticketsRef = db.collection("tickets")

    // Live list of ALL tickets (staff view) - updates in real time
    fun getAllTickets(): Flow<List<Ticket>> = callbackFlow {
        val listener = ticketsRef
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val tickets = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Ticket::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(tickets)
            }
        awaitClose { listener.remove() }
    }.catch {emit(emptyList())}

    // Live list of tickets for ONE student
    fun getTicketsForStudent(studentId: String): Flow<List<Ticket>> = callbackFlow {
        val listener = ticketsRef
            .whereEqualTo("studentId", studentId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val tickets = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Ticket::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(tickets)
            }
        awaitClose { listener.remove() }
    }.catch {emit(emptyList())}

    // Live single ticket (detail screen)
    fun getTicketById(ticketId: String): Flow<Ticket?> = callbackFlow {
        val listener = ticketsRef.document(ticketId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val ticket = snapshot?.toObject(Ticket::class.java)?.copy(id = snapshot.id)
                trySend(ticket)
            }
        awaitClose { listener.remove() }
    }.catch {emit(null)}

    fun getMessages(ticketId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = ticketsRef.document(ticketId).collection("messages")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }.catch { emit(emptyList()) }

    suspend fun sendMessage(ticketId: String, senderId: String, senderLabel: String, text: String) {
        ticketsRef.document(ticketId).collection("messages").add(
            ChatMessage(senderId = senderId, senderLabel = senderLabel, text = text)
        ).await()
    }

    suspend fun insertTicket(ticket: Ticket) {
        ticketsRef.add(ticket).await()
    }

    suspend fun updateTicket(ticket: Ticket) {
        ticketsRef.document(ticket.id).set(ticket).await()
    }
}
