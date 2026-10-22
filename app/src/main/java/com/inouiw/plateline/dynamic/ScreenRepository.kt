package com.inouiw.plateline.dynamic

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * Loads a screen layout. Firestore first (collection "screens", field "json"),
 * then the copy bundled in assets so the app still works offline or before the DB exists.
 */
object ScreenRepository {
    suspend fun load(context: Context, id: String): JSONObject {
        val remote = try {
            withTimeoutOrNull(4000) {
                FirebaseFirestore.getInstance().collection("screens").document(id).get().await().getString("json")
            }
        } catch (e: Exception) {
            null
        }
        val text = remote ?: context.assets.open("screens/$id.json").bufferedReader().use { it.readText() }
        return JSONObject(text)
    }
}
