package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONObject

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    LIST,
    GET,
    WRITE
}

fun handleFirestoreError(
    error: Exception,
    operationType: OperationType,
    path: String
) {
    val user = Firebase.auth.currentUser
    val payload = JSONObject().apply {
        put("error", error.message ?: "Unknown Firestore error")
        put("operationType", operationType.name)
        put("path", path)
        put("authInfo", JSONObject().apply {
            put("userId", user?.uid ?: "")
            put("email", user?.email ?: "")
            put("emailVerified", user?.isEmailVerified ?: false)
            put("providerCount", user?.providerData?.size ?: 0)
        })
    }
    Log.e("FirestoreError", "Firestore Error: $payload", error)
}
