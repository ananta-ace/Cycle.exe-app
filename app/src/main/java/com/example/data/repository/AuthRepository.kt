package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.repository.FirestoreMappers.toUserProfileSafe
import com.example.util.DateUtils
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SignInCancelledException(message: String) : Exception(message)

object SuperAdminConfig {
    val AUTHORIZED_EMAILS = setOf(
        "ananta.base@gmail.com",
        "towhidulislamananta@gmail.com"
    )
    const val DESIGNATED_EMAIL = "ananta.base@gmail.com"

    fun isSuperAdminEmail(email: String?): Boolean {
        val clean = email?.trim()?.lowercase() ?: return false
        return clean in AUTHORIZED_EMAILS
    }
}

class AuthRepository(
    private val context: Context,
    private val firebase: FirebaseProvider
) {
    private val auth: FirebaseAuth = firebase.auth
    private val db = firebase.db

    fun observeAuthUser(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val ref = db.collection("users").document(uid)
        val listener = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, "users/$uid")
                close(error)
                return@addSnapshotListener
            }
            if (snap != null && snap.exists()) {
                trySend(snap.toUserProfileSafe())
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }.catch { e ->
        val cause = (e as? CancellationException)?.cause ?: e
        if (cause is FirebaseFirestoreException) {
            Log.w("AuthRepository", "Profile observer terminated: ${cause.message}")
            emit(null)
        } else {
            throw e
        }
    }

    suspend fun ensureUserProfile(user: FirebaseUser): UserProfile? {
        val userRef = db.collection("users").document(user.uid)
        val now = DateUtils.nowIsoString()
        return try {
            val snap = userRef.get().await()
            if (!snap.exists()) {
                val isOwnerEmail = SuperAdminConfig.isSuperAdminEmail(user.email)
                val initialRole = if (isOwnerEmail && user.isEmailVerified) "SUPER_ADMIN" else "PUBLIC"
                val newProfile = UserProfile(
                    id = user.uid,
                    email = user.email.orEmpty(),
                    emailVerified = user.isEmailVerified,
                    displayName = user.displayName?.ifBlank { null }
                        ?: user.email?.substringBefore("@")
                        ?: "Cyclist",
                    photoURL = user.photoUrl?.toString(),
                    bio = "",
                    role = initialRole,
                    isAdminMember = false,
                    isSuspended = false,
                    adminMembershipStatus = "NONE",
                    createdAt = now,
                    updatedAt = now
                )
                userRef.set(
                    mapOf(
                        "id" to newProfile.id,
                        "email" to newProfile.email,
                        "emailVerified" to newProfile.emailVerified,
                        "displayName" to newProfile.displayName,
                        "photoURL" to (newProfile.photoURL ?: ""),
                        "bio" to "",
                        "role" to newProfile.role,
                        "isSuspended" to false,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                ).await()

                try {
                    userRef.collection("private").document("info").set(
                        mapOf(
                            "email" to (user.email ?: ""),
                            "emailVerified" to user.isEmailVerified,
                            "lastLoginAt" to now,
                            "loginCount" to 1
                        )
                    ).await()
                } catch (_: Exception) {
                }

                newProfile
            } else {
                val existing = snap.toUserProfileSafe()
                try {
                    userRef.collection("private").document("info").set(
                        mapOf(
                            "email" to (user.email ?: ""),
                            "emailVerified" to user.isEmailVerified,
                            "lastLoginAt" to now
                        ),
                        SetOptions.merge()
                    ).await()
                } catch (_: Exception) {
                }

                if (SuperAdminConfig.isSuperAdminEmail(user.email) && user.isEmailVerified) {
                    if (existing != null && existing.role != "SUPER_ADMIN") {
                        try {
                            userRef.update(
                                mapOf(
                                    "role" to "SUPER_ADMIN",
                                    "isSuspended" to false,
                                    "updatedAt" to now
                                )
                            ).await()
                        } catch (_: Exception) {
                        }
                    }
                    try {
                        syncAllSuperAdminAccountsInternal(now)
                    } catch (_: Exception) {
                    }
                }
                existing
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${user.uid}")
            null
        }
    }

    suspend fun trySilentGoogleSignIn(activityContext: Context): Result<FirebaseUser?> {
        if (auth.currentUser != null) return Result.success(auth.currentUser)
        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activityContext, request)
            val cred = result.credential
            if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCred).await()
                Result.success(authResult.user)
            } else {
                Result.success(null)
            }
        } catch (_: GetCredentialException) {
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleInteractive(activityContext: Context): Result<FirebaseUser> {
        val credentialManager = CredentialManager.create(activityContext)
        val signInOption = GetSignInWithGoogleOption.Builder(
            context.getString(R.string.default_web_client_id)
        ).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        val startElapsedMs = SystemClock.elapsedRealtime()
        return try {
            val result = credentialManager.getCredential(activityContext, request)
            val cred = result.credential
            if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCred).await()
                val user = authResult.user
                    ?: return Result.failure(IllegalStateException("Google Sign-In returned no Firebase user."))
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type received."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            val elapsedMs = SystemClock.elapsedRealtime() - startElapsedMs
            Log.w("AuthRepository", "Credential Manager cancelled after ${elapsedMs}ms: ${e.message}", e)
            // Instant cancellation (< 850ms) typically happens when the APK SHA-1 isn't registered
            // for native One-Tap or Play Services rejects Credential Manager immediately.
            // Automatically fall back to Browser Google Sign-In so sign-in still succeeds!
            if (elapsedMs < 850L && activityContext is Activity) {
                Log.i("AuthRepository", "Falling back to Browser Google Sign-In after instant Credential Manager rejection.")
                signInWithGoogleBrowser(activityContext)
            } else {
                Result.failure(
                    SignInCancelledException("Account selection was closed. Tap Continue with Google or Browser Sign-In whenever you're ready.")
                )
            }
        } catch (e: NoCredentialException) {
            Log.w("AuthRepository", "No Google credentials available on device; using Browser Sign-In: ${e.message}", e)
            if (activityContext is Activity) {
                signInWithGoogleBrowser(activityContext)
            } else {
                Result.failure(IllegalStateException("No device Google account found. Please use Browser Google Sign-In below."))
            }
        } catch (e: GetCredentialException) {
            Log.w("AuthRepository", "Credential Manager error (${e.type}); falling back to Browser Sign-In: ${e.message}", e)
            if (activityContext is Activity) {
                signInWithGoogleBrowser(activityContext)
            } else {
                Result.failure(IllegalStateException("Google Sign-In unavailable (${e.type}). Please use Browser Google Sign-In below."))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Unexpected sign-in error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleBrowser(activity: Activity): Result<FirebaseUser> {
        return try {
            val pending = auth.pendingAuthResult
            val pendingUser = if (pending != null) {
                runCatching { pending.await().user }.getOrNull()
            } else {
                null
            }
            if (pendingUser != null) {
                return Result.success(pendingUser)
            }

            val provider = OAuthProvider.newBuilder("google.com")
                .addCustomParameter("prompt", "select_account")
                .build()
            val authResult = auth.startActivityForSignInWithProvider(activity, provider).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Browser Google Sign-In returned no Firebase user."))
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthException) {
            val code = e.errorCode
            val msg = e.message.orEmpty()
            Log.w("AuthRepository", "Browser Google Sign-In FirebaseAuthException ($code): $msg", e)
            if (code == "ERROR_WEB_CONTEXT_CANCELED" ||
                msg.contains("canceled", ignoreCase = true) ||
                msg.contains("cancelled", ignoreCase = true)
            ) {
                Result.failure(SignInCancelledException("Browser Google Sign-In was closed before completion."))
            } else {
                Result.failure(IllegalStateException(msg.ifBlank { "Browser Google Sign-In failed ($code)." }))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Browser Google Sign-In failed: ${e.message}", e)
            val msg = e.message.orEmpty()
            if (msg.contains("canceled", ignoreCase = true) || msg.contains("cancelled", ignoreCase = true)) {
                Result.failure(SignInCancelledException("Browser Google Sign-In was closed before completion."))
            } else {
                Result.failure(IllegalStateException(msg.ifBlank { "Browser Google Sign-In failed. Please check your connection and try again." }))
            }
        }
    }

    suspend fun signOut(activityContext: Context) {
        withContext(NonCancellable) {
            auth.signOut()
            try {
                CredentialManager.create(activityContext).clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
            }
        }
    }

    suspend fun syncSuperAdminAccess(): Pair<Boolean, String> {
        val user = auth.currentUser
            ?: return false to "Please sign in first."
        if (!SuperAdminConfig.isSuperAdminEmail(user.email)) {
            return false to "Only authorized Super Admin emails can sync Super Admin access."
        }
        if (!user.isEmailVerified) {
            return false to "Please verify your email address first."
        }

        val now = DateUtils.nowIsoString()
        return try {
            val userRef = db.collection("users").document(user.uid)
            val snap = userRef.get().await()
            if (snap.exists() && snap.getString("role") != "SUPER_ADMIN") {
                userRef.update(
                    mapOf(
                        "role" to "SUPER_ADMIN",
                        "isSuspended" to false,
                        "updatedAt" to now
                    )
                ).await()
            }
            val updatedCount = syncAllSuperAdminAccountsInternal(now)
            if (updatedCount > 0) {
                true to "Synced SUPER_ADMIN database clearance for $updatedCount account(s)!"
            } else {
                true to "Your Super Admin database clearance is active!"
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${user.uid}")
            false to "Database rules blocked self-promotion (${e.message ?: "Permission denied"}). Sign in once as towhidulislamananta@gmail.com to auto-grant database SUPER_ADMIN clearance."
        }
    }

    private suspend fun syncAllSuperAdminAccountsInternal(now: String): Int {
        var updatedCount = 0
        val allSnap = db.collection("users").get().await()
        for (docSnap in allSnap.documents) {
            val email = docSnap.getString("email")
            val role = docSnap.getString("role")
            val shouldBeSuper = SuperAdminConfig.isSuperAdminEmail(email)
            if (shouldBeSuper && role != "SUPER_ADMIN") {
                db.collection("users").document(docSnap.id).update(
                    mapOf(
                        "role" to "SUPER_ADMIN",
                        "isSuspended" to false,
                        "updatedAt" to now
                    )
                ).await()
                updatedCount++
            } else if (!shouldBeSuper && role == "SUPER_ADMIN") {
                db.collection("users").document(docSnap.id).update(
                    mapOf(
                        "role" to "PUBLIC",
                        "updatedAt" to now
                    )
                ).await()
            }
        }
        return updatedCount
    }

    suspend fun updateProfileDetails(
        displayName: String,
        bio: String,
        photoURL: String? = null,
        customTitle: String? = null
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanName = displayName.trim().ifBlank { "Cyclist" }.take(80)
        val now = DateUtils.nowIsoString()
        val updates = mutableMapOf<String, Any>(
            "displayName" to cleanName,
            "bio" to bio.trim(),
            "updatedAt" to now
        )
        if (photoURL != null) updates["photoURL"] = photoURL
        if (customTitle != null) updates["customTitle"] = customTitle.trim()

        return try {
            db.collection("users").document(user.uid).update(updates).await()
            try {
                val changeReq = userProfileChangeRequest {
                    this.displayName = cleanName
                }
                user.updateProfile(changeReq).await()
            } catch (_: Exception) {
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${user.uid}")
            Result.failure(e)
        }
    }

    suspend fun dismissWelcomeModal(roleToken: String) {
        val user = auth.currentUser ?: return
        try {
            db.collection("users").document(user.uid).update(
                mapOf(
                    "welcomeSeenForRole" to roleToken,
                    "updatedAt" to DateUtils.nowIsoString()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${user.uid}")
        }
    }
}
