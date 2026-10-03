package com.example.helpdesk

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object NotificationWatcher {
    private const val CHANNEL_ID = "helpdesk_updates"
    private var registration: ListenerRegistration? = null
    private var watchingUserId: String? = null

    fun start(context: Context, userId: String) {
        if (userId.isBlank() || watchingUserId == userId) return
        stop()
        val appContext = context.applicationContext
        createChannel(appContext)
        watchingUserId = userId

        var firstSnapshot = true
        registration = FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                // Skip the initial load so old notifications don't pop up on launch
                if (firstSnapshot) {
                    firstSnapshot = false
                    return@addSnapshotListener
                }
                snapshot.documentChanges
                    .filter { it.type == DocumentChange.Type.ADDED }
                    .forEach { change ->
                        val message = change.document.getString("message") ?: return@forEach
                        show(appContext, message, change.document.id.hashCode())
                    }
            }
    }

    fun stop() {
        registration?.remove()
        registration = null
        watchingUserId = null
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Ticket updates", NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun show(context: Context, message: String, id: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        // LoginActivity auto-routes to the right screen when already signed in
        val intent = Intent(context, LoginActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("HelpDesk")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}