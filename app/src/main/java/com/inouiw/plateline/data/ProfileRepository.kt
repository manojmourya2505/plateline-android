package com.inouiw.plateline.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Profiles live in Firestore, collection "users", document id = phone number (e.g. +916281511277). */
object ProfileRepository {
    private fun users() = FirebaseFirestore.getInstance().collection("users")

    /** The user signed in with their phone OTP. Their id stays the same across reinstalls. */
    private fun uid(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: throw IllegalStateException("Not signed in")

    suspend fun find(phone: String): Profile? {
        val doc = users().document(phone).get().await()
        if (!doc.exists()) return null
        return Profile(
            phone = phone,
            name = doc.getString("name").orEmpty(),
            email = doc.getString("email").orEmpty(),
            pin = doc.getString("pin").orEmpty(),
        )
    }

    /** Firestore queues the write when offline, so this does not wait for the server. */
    fun save(profile: Profile) {
        users().document(profile.phone).set(
            mapOf(
                "uid" to uid(),
                "name" to profile.name,
                "email" to profile.email,
                "pin" to profile.pin,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )
    }
}
