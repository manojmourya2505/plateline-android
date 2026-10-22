package com.inouiw.plateline.ui.screens

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.inouiw.plateline.R
import com.inouiw.plateline.data.AgentApi
import com.inouiw.plateline.ui.*
import com.inouiw.plateline.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class DocItem(
    val title: Int,
    val hint: Int,
    val sample: String,
    val required: Boolean,
    val uploaded: Boolean = false,
    val detail: String = "",
)

private data class DocSlot(val key: String, val title: Int, val hint: Int, val required: Boolean)

private val docSlots = listOf(
    DocSlot("rc", R.string.doc_v5c, R.string.doc_v5c_hint, true),
    DocSlot("puc", R.string.doc_mot, R.string.doc_mot_hint, true),
    DocSlot("service", R.string.doc_service, R.string.doc_service_hint, false),
    DocSlot("spare_key", R.string.doc_key, R.string.doc_key_hint, false),
)

// Papers: tap + to add one from the gallery. Saved papers stay on the Mac, and tapping one opens it full screen.
@Composable
fun DocumentsScreen(plate: String, onBack: () -> Unit, onNext: () -> Unit, onHelp: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved = remember { mutableStateMapOf<String, ByteArray>() }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf<String?>(null) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf<String?>(null) }
    var rcDetail by remember { mutableStateOf<String?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    val readFailed = stringResource(R.string.docs_read_failed)
    val notClear = stringResource(R.string.docs_plate_unclear)
    val loadFailed = "Could not load your papers. Check that the Lane server is running."

    LaunchedEffect(plate) {
        loading = true
        try {
            AgentApi.listDocuments(plate).forEach { key -> saved[key] = AgentApi.documentBytes(plate, key) }
        } catch (e: Exception) {
            problem = loadFailed
        }
        loading = false
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val key = chosen ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        busy = key; problem = null
        scope.launch {
            try {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: error("empty file")
                AgentApi.uploadDocument(plate, key, bytes, mime)
                saved[key] = bytes
                if (key == "rc") {
                    // The RC also gives the plate and car details, which Lane reads from the photo.
                    val name = if (mime.contains("png")) "rc.png" else "rc.jpg"
                    val r = AgentApi.readDocument(bytes, name, mime)
                    if (r.has("error") && !r.isNull("error")) {
                        problem = r.getString("error")
                    } else {
                        val plateRead = r.optString("plate").takeIf { it.isNotBlank() && it != "null" } ?: notClear
                        val car = listOf(r.optString("make"), r.optString("model"))
                            .filter { it.isNotBlank() && it != "null" }.joinToString(" ")
                        rcDetail = if (car.isBlank()) plateRead else "$plateRead · $car"
                    }
                }
            } catch (e: Exception) {
                problem = readFailed
            }
            busy = null
        }
    }
    fun pick(key: String) {
        chosen = key
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val ready = docSlots.filter { it.required }.all { saved.containsKey(it.key) }

    ScreenBox {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.docs_title), onBack = onBack)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.docs_heading), style = sora(28, 800))
                Text(stringResource(R.string.docs_sub), style = dm(14, 400, Slate))
                Spacer(Modifier.height(4.dp))
                if (loading) Text("Loading your papers…", style = dm(13, 500, Slate))
                docSlots.forEach { slot ->
                    val isDone = saved.containsKey(slot.key)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(White)
                            .clickable(enabled = isDone && busy == null) { viewing = slot.key }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Paper), Alignment.Center) {
                            Icon(Icons.Rounded.Description, null, Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(slot.title), style = dm(15, 700))
                            Text(
                                when {
                                    isDone && slot.key == "rc" && rcDetail != null -> rcDetail.orEmpty()
                                    isDone -> "Uploaded · tap to open"
                                    else -> stringResource(slot.hint)
                                },
                                style = dm(13, 400, Slate),
                            )
                        }
                        when {
                            busy == slot.key -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Ink)
                            isDone -> {
                                Tick()
                                Icon(
                                    Icons.Rounded.Delete, stringResource(R.string.docs_remove),
                                    Modifier.size(24.dp).clickable(enabled = busy == null) {
                                        scope.launch {
                                            try {
                                                AgentApi.deleteDocument(plate, slot.key)
                                                saved.remove(slot.key)
                                                if (slot.key == "rc") rcDetail = null
                                            } catch (e: Exception) {
                                                problem = readFailed
                                            }
                                        }
                                    },
                                )
                            }
                            else -> Box(Modifier.bounceClick { if (busy == null && !loading) pick(slot.key) }) { YellowDot(Icons.Rounded.Add, 36.dp) }
                        }
                    }
                }
                problem?.let { Text(it, style = dm(13, 500, Ink)) }
                Text(stringResource(R.string.docs_demo_note), style = dm(12, 400, Slate))
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(label = stringResource(R.string.docs_help), onClick = onHelp)
                PrimaryCta(stringResource(R.string.docs_next), enabled = ready && busy == null && !loading, onClick = onNext)
            }
        }
    }

    viewing?.let { key ->
        saved[key]?.let { bytes ->
            FullScreenPhoto(
                title = stringResource(docSlots.first { it.key == key }.title),
                bytes = bytes,
                onClose = { viewing = null },
                onReplace = { viewing = null; pick(key) },
            )
        }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val faq = listOf(R.string.help_q1 to R.string.help_a1, R.string.help_q2 to R.string.help_a2, R.string.help_q3 to R.string.help_a3)
    ScreenBox {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.help_title), onBack = onBack)
            LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(faq) { (q, a) ->
                    var open by remember { mutableStateOf(false) }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(White).clickable { open = !open }.padding(16.dp)) {
                        Text(stringResource(q), style = dm(15, 700))
                        if (open) {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(a), style = dm(14, 400, Slate))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val list = listOf(R.string.notif_1 to R.string.notif_1_body, R.string.notif_2 to R.string.notif_2_body, R.string.notif_3 to R.string.notif_3_body)
    ScreenBox {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.notif_title), onBack = onBack)
            LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list) { (t, b) ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(White).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(Yellow), Alignment.Center) {
                            Icon(Icons.Rounded.Notifications, null, Modifier.size(22.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(t), style = dm(15, 700))
                            Text(stringResource(b), style = dm(13, 400, Slate))
                        }
                    }
                }
            }
        }
    }
}
