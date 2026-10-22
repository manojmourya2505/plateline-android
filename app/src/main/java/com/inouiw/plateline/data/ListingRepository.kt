package com.inouiw.plateline.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** One listing per phone number, stored in collection "listings". */
object ListingRepository {
    private fun listings() = FirebaseFirestore.getInstance().collection("listings")

    suspend fun find(phone: String): Listing? {
        val doc = listings().document(phone).get().await()
        if (!doc.exists()) return null
        return Listing(
            plate = doc.getString("plate").orEmpty(),
            car = doc.getString("car").orEmpty(),
            price = (doc.getLong("price") ?: 0L).toInt(),
        )
    }

    fun save(phone: String, listing: Listing) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        listings().document(phone).set(
            mapOf(
                "uid" to uid,
                "plate" to listing.plate,
                "car" to listing.car,
                "price" to listing.price.toLong(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )
    }
}
