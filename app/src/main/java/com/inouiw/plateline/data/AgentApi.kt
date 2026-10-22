package com.inouiw.plateline.data

import com.inouiw.plateline.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Talks to the Lane server running on the Mac (see server.py in plateline-agent).
 * The API keys stay on the Mac. The phone only sends the secret token.
 * Every call runs off the main thread and throws IOException if the server can't be reached.
 */
object AgentApi {
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // the AI and the vehicle lookup can take a while
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private suspend fun send(path: String, body: RequestBody): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BuildConfig.AGENT_URL + path)
            .header("X-Plateline-Token", BuildConfig.AGENT_TOKEN)
            .post(body)
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Lane server returned ${response.code}")
            JSONObject(text)
        }
    }

    /** Ask Lane a question. history is the earlier turns as (role, text) pairs, oldest first. */
    suspend fun chat(message: String, history: List<Pair<String, String>>, context: String = ""): String {
        val turns = JSONArray()
        history.takeLast(12).forEach { (role, text) ->
            turns.put(JSONObject().put("role", role).put("content", text))
        }
        val body = JSONObject().put("message", message).put("history", turns)
        if (context.isNotBlank()) body.put("context", context)
        return send("chat", body.toString().toRequestBody(jsonType)).getString("reply")
    }

    /** Look up a car by plate. The result has "error" if it can't be found, otherwise the car's details. */
    suspend fun lookup(plate: String): JSONObject {
        val body = JSONObject().put("plate", plate)
        return send("lookup", body.toString().toRequestBody(jsonType))
    }

    /** Read an RC or insurance photo. Returns plate, make, model, fuel, registrationDate, insuranceUpto or "error". */
    suspend fun readDocument(bytes: ByteArray, fileName: String, mimeType: String): JSONObject {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, bytes.toRequestBody(mimeType.toMediaType()))
            .build()
        return send("document", body)
    }

    /** Upload one car photo to the Lane server. slot is the photo's name, e.g. "front". */
    suspend fun uploadPhoto(plate: String, slot: String, bytes: ByteArray, mimeType: String): JSONObject {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("plate", plate)
            .addFormDataPart("slot", slot)
            .addFormDataPart("file", "$slot.jpg", bytes.toRequestBody(mimeType.toMediaType()))
            .build()
        return send("photo", body)
    }

    /** Estimate a rupee range from model, year, fuel, km and condition. AI estimate, not live prices. */
    suspend fun valuation(make: String, model: String, year: Int?, fuel: String?, km: Int?, condition: String): JSONObject {
        val body = JSONObject()
            .put("make", make)
            .put("model", model)
            .put("year", year ?: JSONObject.NULL)
            .put("fuel", fuel ?: JSONObject.NULL)
            .put("km", km ?: JSONObject.NULL)
            .put("condition", condition)
        return send("valuation", body.toString().toRequestBody(jsonType))
    }

    /** Work out the net amount of an offer and the checks to make. */
    suspend fun offer(
        dealer: String,
        offerAmount: Int,
        deductions: List<Pair<String, Int>>,
        validUntil: String?,
    ): JSONObject {
        val items = JSONArray()
        deductions.forEach { (label, amount) -> items.put(JSONObject().put("label", label).put("amount", amount)) }
        val body = JSONObject()
            .put("dealer", dealer)
            .put("offer_amount", offerAmount)
            .put("deductions", items)
            .put("valid_until", validUntil ?: JSONObject.NULL)
        return send("offer", body.toString().toRequestBody(jsonType))
    }

    private fun plateKey(plate: String) = plate.filter { it.isLetterOrDigit() }

    private suspend fun getBytes(path: String): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BuildConfig.AGENT_URL + path)
            .header("X-Plateline-Token", BuildConfig.AGENT_TOKEN)
            .get()
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Lane server returned ${response.code}")
            response.body?.bytes() ?: ByteArray(0)
        }
    }

    private suspend fun deleteRequest(path: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BuildConfig.AGENT_URL + path)
            .header("X-Plateline-Token", BuildConfig.AGENT_TOKEN)
            .delete()
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Lane server returned ${response.code}")
        }
    }

    /** Car photo slots already saved on the Mac for this plate, e.g. "front". */
    suspend fun listPhotos(plate: String): List<String> {
        val names = JSONObject(String(getBytes("photos/${plateKey(plate)}"), Charsets.UTF_8)).getJSONArray("photos")
        return (0 until names.length()).map { names.getString(it) }
    }

    suspend fun photoBytes(plate: String, slot: String): ByteArray = getBytes("photo/${plateKey(plate)}/$slot")

    /** Papers already saved on the Mac for this plate: rc, puc, service, spare_key. */
    suspend fun listDocuments(plate: String): List<String> {
        val keys = JSONObject(String(getBytes("documents/${plateKey(plate)}"), Charsets.UTF_8)).getJSONArray("documents")
        return (0 until keys.length()).map { keys.getString(it) }
    }

    suspend fun documentBytes(plate: String, key: String): ByteArray = getBytes("document-file/${plateKey(plate)}/$key")

    /** Save one paper on the Mac. key is rc, puc, service or spare_key. */
    suspend fun uploadDocument(plate: String, key: String, bytes: ByteArray, mimeType: String): JSONObject {
        val ext = if (mimeType.contains("png")) "png" else "jpg"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("plate", plate)
            .addFormDataPart("key", key)
            .addFormDataPart("file", "$key.$ext", bytes.toRequestBody(mimeType.toMediaType()))
            .build()
        return send("document-save", body)
    }

    suspend fun deleteDocument(plate: String, key: String) = deleteRequest("document-file/${plateKey(plate)}/$key")
}
