package com.example.toda.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.auth.EmailAuthProvider

@Singleton
class FirebaseAuthService @Inject constructor() {

    private val auth: FirebaseAuth = Firebase.auth

    suspend fun createUserWithPhoneNumber(phoneNumber: String, password: String): Result<String> {
        return try {
            // Since Firebase Auth requires email, we'll use phone as email format
            val email = "${phoneNumber}@toda.local"

            // Check if user already exists first
            try {
                val existingResult = auth.signInWithEmailAndPassword(email, password).await()
                if (existingResult.user != null) {
                    // User already exists, return existing user ID
                    return Result.success(existingResult.user!!.uid)
                }
            } catch (e: Exception) {
                // User doesn't exist or wrong password, continue with creation
            }

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user.uid)
            } else {
                Result.failure(Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            // If creation fails due to email already exists, try to sign in instead
            if (e.message?.contains("email address is already in use") == true) {
                try {
                    val email = "${phoneNumber}@toda.local"
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    val user = result.user
                    if (user != null) {
                        Result.success(user.uid)
                    } else {
                        Result.failure(Exception("Phone number already registered with different password"))
                    }
                } catch (signInException: Exception) {
                    Result.failure(Exception("Phone number already registered with different password"))
                }
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun signInWithPhoneNumber(phoneNumber: String, password: String): Result<String> {
        return try {
            val email = "${phoneNumber}@toda.local"
            println("DEBUG: Attempting login with email: $email") // Debug log
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                println("DEBUG: Login successful for user: ${user.uid}") // Debug log
                Result.success(user.uid)
            } else {
                Result.failure(Exception("Authentication succeeded but user is null"))
            }
        } catch (e: Exception) {
            println("DEBUG: Login failed with error: ${e.message}") // Debug log
            Result.failure(Exception("Authentication failed: ${e.message}"))
        }
    }

    suspend fun createUserWithEmail(email: String, password: String): Result<String> {
        return try {
            // Check if user already exists first
            try {
                val existingResult = auth.signInWithEmailAndPassword(email, password).await()
                if (existingResult.user != null) {
                    // User already exists, return existing user ID
                    return Result.success(existingResult.user!!.uid)
                }
            } catch (e: Exception) {
                // User doesn't exist or wrong password, continue with creation
            }

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user.uid)
            } else {
                Result.failure(Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            // If creation fails due to email already exists, try to sign in instead
            if (e.message?.contains("email address is already in use") == true) {
                try {
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    val user = result.user
                    if (user != null) {
                        Result.success(user.uid)
                    } else {
                        Result.failure(Exception("Email already registered with different password"))
                    }
                } catch (signInException: Exception) {
                    Result.failure(Exception("Email already registered with different password"))
                }
            } else {
                Result.failure(e)
            }
        }
    }

    // New: sign in with email and password
    suspend fun signInWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) Result.success(user.uid) else Result.failure(Exception("Authentication succeeded but user is null"))
        } catch (e: Exception) {
            Result.failure(Exception("Authentication failed: ${e.message}"))
        }
    }

    // New: link email/password credential to the currently signed-in user (e.g., after phone OTP)
    suspend fun linkEmailPasswordToCurrentUser(email: String, password: String): Result<String> {
        val user = auth.currentUser ?: return Result.failure(Exception("No authenticated user to link to"))
        return try {
            val cred = EmailAuthProvider.getCredential(email, password)
            user.linkWithCredential(cred).await()
            Result.success(user.uid)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to link email/password"))
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun signOut() = auth.signOut()

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    // New: send password reset email for email/password accounts
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to send password reset email"))
        }
    }
}
