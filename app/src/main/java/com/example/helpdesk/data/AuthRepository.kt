package com.example.helpdesk.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.FirebaseApp

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val role: String = "", // "STUDENT" , "STAFF" or "ADMIN"
    val active: Boolean = true,
)

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val usersRef = FirebaseFirestore.getInstance().collection("users")

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val currentUserEmail: String?
        get() = auth.currentUser?.email

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun signUp(email: String, password: String, role: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Sign up failed")
            usersRef.document(uid).set(AppUser(uid = uid, email = email, role = role)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val uid = auth.currentUser?.uid ?: throw Exception("Login failed")
            val doc = usersRef.document(uid).get().await()
            val isActive = doc.getBoolean("active") ?: true
            if (!isActive) {
                auth.signOut()
                return Result.failure(Exception("This account has been disabled. Contact an Admin."))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserRole(): String? {
        val uid = currentUserId ?: return null
        val doc = usersRef.document(uid).get().await()
        return doc.getString("role")
    }

    suspend fun getAllUsers(): List<AppUser> {
        val snapshot = usersRef.get().await()
        return snapshot.documents.mapNotNull { it.toObject(AppUser::class.java) }
    }

    suspend fun setUserActive(uid: String, active: Boolean) {
        usersRef.document(uid).update("active", active).await()
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun createUserAsAdmin(email: String, password: String, role: String): Result<Unit> {
        return try {
            val mainApp = FirebaseApp.getInstance()
            val secondary = try {
                FirebaseApp.getInstance("secondary")
            } catch (e: IllegalStateException) {
                FirebaseApp.initializeApp(mainApp.applicationContext, mainApp.options, "secondary")
            }
            val secondaryAuth = FirebaseAuth.getInstance(secondary)
            val created = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = created.user?.uid ?: throw Exception("Account creation failed")
            secondaryAuth.signOut()
            usersRef.document(uid).set(AppUser(uid = uid, email = email, role = role, active = true)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}