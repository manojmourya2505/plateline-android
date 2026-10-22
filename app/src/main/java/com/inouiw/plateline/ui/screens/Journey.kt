package com.inouiw.plateline.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.ui.res.stringResource
import com.inouiw.plateline.R
import com.inouiw.plateline.data.AgentApi
import com.inouiw.plateline.data.Profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inouiw.plateline.ui.*
import com.inouiw.plateline.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedTextField

private data class CarDetails(val name: String, val make: String, val model: String, val year: Int?, val fuel: String?)

// Reads make, model, year and fuel from a lookup result or an RC reading (both use the same keys).
private fun carDetails(r: org.json.JSONObject, fallbackName: String): CarDetails {
    val make = r.optString("make").takeIf { it != "null" }.orEmpty()
    val model = r.optString("model").takeIf { it != "null" }.orEmpty()
    val name = listOf(make, model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { fallbackName }
    val year = Regex("(19|20)\\d{2}").find(r.optString("registrationDate"))?.value?.toIntOrNull()
    val fuel = r.optString("fuel").takeIf { it != "null" }
    return CarDetails(name, make, model, year, fuel)
}

// Step 2 of the seller's journey: the kilometres step. Kilometres are required before any value is worked out.
@Composable
fun ValuationScreen(plate: String, onBack: () -> Unit, onEstimate: (String, Int, Int) -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var km by remember { mutableStateOf("") }
    var cond by remember { mutableStateOf("Good") }
    var busy by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf<CarDetails?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    var needRc by remember { mutableStateOf(false) }
    val lookupFailed = stringResource(R.string.lookup_failed)
    val priceFailed = stringResource(R.string.price_failed)
    val rcFailed = stringResource(R.string.docs_read_failed)
    val kmValue = km.toIntOrNull()?.takeIf { it in 1..999_999 }

    // Gets the estimate for the car details and the kilometres the seller entered, then shows the result.
    fun estimate(d: CarDetails, kmNow: Int) {
        busy = true; problem = null
        scope.launch {
            try {
                val v = AgentApi.valuation(d.make, d.model, d.year, d.fuel, kmNow, cond)
                details = d
                onEstimate(d.name, v.getInt("low"), v.getInt("high"))
                busy = false
                onNext()
            } catch (e: Exception) {
                problem = priceFailed
                busy = false
            }
        }
    }

    // Look the car up by plate, then estimate. Only runs after the seller enters kilometres.
    fun lookupThenEstimate(kmNow: Int) {
        busy = true; problem = null; needRc = false
        scope.launch {
            val r = try { AgentApi.lookup(plate) } catch (e: Exception) { null }
            if (r == null || (r.has("error") && !r.isNull("error"))) {
                problem = r?.optString("error")?.takeIf { it.isNotBlank() && it != "null" } ?: lookupFailed
                needRc = true
                busy = false
                return@launch
            }
            busy = false
            estimate(carDetails(r, plate), kmNow)
        }
    }

    // Fallback when the plate can't be found: the seller shows the RC and the agent reads it.
    val pickRc = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val kmNow = km.toIntOrNull()?.takeIf { it in 1..999_999 } ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true; problem = null
        scope.launch {
            try {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("empty file")
                val r = AgentApi.readDocument(bytes, if (mime.contains("png")) "rc.png" else "rc.jpg", mime)
                if (r.has("error") && !r.isNull("error")) {
                    problem = r.getString("error"); busy = false
                } else {
                    estimate(carDetails(r, plate), kmNow)
                }
            } catch (e: Exception) {
                problem = rcFailed; busy = false
            }
        }
    }

    ScreenBox {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackButton(onBack)
                Text(stringResource(R.string.valuation_step), style = dm(13, 400, Slate))
            }
            Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(Line)) {
                Box(Modifier.fillMaxWidth(0.5f).fillMaxHeight().clip(CircleShape).background(Yellow))
            }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.valuation_heading), style = sora(26, 800))
                Text(stringResource(R.string.valuation_sub), style = dm(14, 400, Slate))
                Plate(plate, Modifier.fillMaxWidth(), 80.dp, 50)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(White).padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.valuation_km), style = dm(13, 700, Slate), modifier = Modifier.weight(1f))
                        Box(Modifier.clip(CircleShape).background(Yellow).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(stringResource(R.string.valuation_required), style = dm(11, 700, Ink))
                        }
                    }
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
                        BasicTextField(km, { km = it.filter(Char::isDigit) }, Modifier.weight(1f), singleLine = true,
                            textStyle = sora(44, 800).copy(color = Ink), cursorBrush = SolidColor(Ink),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            decorationBox = { inner ->
                                if (km.isEmpty()) Text("0", style = sora(44, 800), color = Slate)
                                inner()
                            })
                        Text("km", style = sora(18, 700), color = Slate, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(Yellow))
                    Text(stringResource(R.string.valuation_km_help), style = dm(13, 400, Slate), modifier = Modifier.padding(top = 10.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.valuation_condition), style = dm(13, 700, Slate))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Great", "Good", "Fair").forEach { Chip(it, cond == it) { cond = it } } }
                }
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                problem?.let { Text(it, style = dm(14, 500, Ink)) }
                if (needRc && !busy) {
                    SecondaryButton(label = stringResource(R.string.valuation_rc_instead), onClick = {
                        if (kmValue != null) pickRc.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                }
                PrimaryCta(
                    if (busy) stringResource(R.string.lane_thinking) else stringResource(R.string.valuation_get),
                    enabled = !busy && kmValue != null,
                    onClick = {
                        val k = kmValue
                        val d = details
                        if (k != null) { if (d == null) lookupThenEstimate(k) else estimate(d, k) }
                    },
                )
                Text(stringResource(R.string.valuation_note_short), style = dm(12, 400, Slate), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

// Step 3: the estimate, shown on its own screen before photos.
// Dark showroom look: spotlight glow, count-up value, animated range bar, yellow action.
@Composable
fun EstimateScreen(car: String, low: Int, high: Int, onBack: () -> Unit, onNext: () -> Unit) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(low, high) { reveal.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
    val t = reveal.value
    val pulse = rememberInfiniteTransition(label = "estimate-glow")
    val glow by pulse.animateFloat(
        initialValue = 0.22f, targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val backdrop = Brush.verticalGradient(listOf(Color(0xFF0B0C0F), Color(0xFF1B1E26), Color(0xFF0B0C0F)))
    val muted = Color.White.copy(alpha = 0.62f)

    Box(Modifier.fillMaxSize().background(backdrop)) {
        // Spotlight behind the car name.
        Box(
            Modifier.fillMaxWidth().height(460.dp).align(Alignment.TopCenter)
                .background(Brush.radialGradient(listOf(Yellow.copy(alpha = glow), Color.Transparent)))
        )
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = White) }
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.estimate_title), style = sora(16, 700), color = White)
            }

            Spacer(Modifier.height(36.dp))
            Column(Modifier.rise(40)) {
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Yellow).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(stringResource(R.string.estimate_label).uppercase(), style = dm(11, 700, Ink))
                }
                Spacer(Modifier.height(14.dp))
                Text(car, style = sora(30, 800), color = White, lineHeight = 36.sp)
            }

            Spacer(Modifier.height(28.dp))
            Column(
                Modifier.fillMaxWidth().rise(120)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
                    .padding(22.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("From", style = dm(12, 500, muted))
                        Text("₹%,d".format((low * t).toInt()), style = sora(22, 800), color = White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("To", style = dm(12, 500, muted))
                        Text("₹%,d".format((high * t).toInt()), style = sora(22, 800), color = Yellow)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))) {
                    Box(
                        Modifier.fillMaxWidth(t.coerceIn(0.02f, 1f)).fillMaxHeight().clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(YellowDeep, Yellow)))
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.valuation_note), style = dm(12, 400, muted))
            }

            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().rise(260)
                    .clip(RoundedCornerShape(50))
                    .background(Yellow)
                    .clickable(onClick = onNext)
                    .padding(start = 26.dp, top = 10.dp, bottom = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add photos", style = sora(16, 700), color = Ink, modifier = Modifier.weight(1f))
                Box(Modifier.size(46.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Yellow)
                }
            }
        }
    }
}

private data class CarShot(val key: String, val label: String, val required: Boolean)

private val carShots = listOf(
    CarShot("front", "Front", true),
    CarShot("rear", "Rear", true),
    CarShot("left", "Left side", true),
    CarShot("right", "Right side", true),
    CarShot("front_seats", "Front seats", false),
    CarShot("dashboard", "Dashboard", false),
    CarShot("mileage", "Mileage", false),
    CarShot("boot", "Boot", false),
    CarShot("wheels", "Wheels", false),
    CarShot("back_seats", "Back seats", false),
    CarShot("engine", "Engine bay", false),
    CarShot("keys", "Keys", false),
)

// Photos: saved ones show as tiles with the picture. Tap a saved photo to view it full screen,
// tap an empty tile to pick one from the gallery. What is saved on the Mac loads each time.
@Composable
fun PhotosScreen(plate: String, onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved = remember { mutableStateMapOf<String, ByteArray>() }
    var loading by remember { mutableStateOf(true) }
    var uploading by remember { mutableStateOf<String?>(null) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf<String?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    val uploadFailed = stringResource(R.string.photo_upload_failed)
    val loadFailed = "Could not load your photos. Check that the Lane server is running."

    LaunchedEffect(plate) {
        loading = true
        try {
            AgentApi.listPhotos(plate).forEach { slot -> saved[slot] = AgentApi.photoBytes(plate, slot) }
        } catch (e: Exception) {
            problem = loadFailed
        }
        loading = false
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val key = chosen ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = key; problem = null
        scope.launch {
            try {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("empty file")
                AgentApi.uploadPhoto(plate, key, bytes, mime)
                saved[key] = bytes
            } catch (e: Exception) {
                problem = uploadFailed
            }
            uploading = null
        }
    }
    fun pick(key: String) {
        chosen = key
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val count = carShots.count { saved.containsKey(it.key) }
    val requiredDone = carShots.filter { it.required }.all { saved.containsKey(it.key) }

    ScreenBox {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Spacer(Modifier.weight(1f))
                Text("$count of ${carShots.size}", style = dm(13, 700, Slate))
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(stringResource(R.string.photos_heading), style = sora(26, 800))
                Text(stringResource(R.string.photos_sub), style = dm(14, 400, Slate))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Line)) {
                    Box(Modifier.fillMaxWidth(count.toFloat() / carShots.size).fillMaxHeight().clip(CircleShape).background(Yellow))
                }
                if (loading) Text("Loading your photos…", style = dm(13, 500, Slate))
                carShots.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { shot ->
                            val bytes = saved[shot.key]
                            val isDone = bytes != null
                            val isUploading = uploading == shot.key
                            val thumb = remember(bytes) { bytes?.let { decodeImage(it, sample = 4) } }
                            Box(
                                Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(22.dp))
                                    .background(if (isDone) Ink else White)
                                    .clickable(enabled = uploading == null && !loading) {
                                        if (isDone) viewing = shot.key else pick(shot.key)
                                    },
                            ) {
                                if (thumb != null) {
                                    Image(thumb, shot.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    Box(
                                        Modifier.fillMaxSize().background(
                                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))
                                        ),
                                    )
                                }
                                if (isDone) {
                                    Box(
                                        Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp).clip(CircleShape).background(Yellow),
                                        Alignment.Center,
                                    ) { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = Ink) }
                                } else {
                                    Box(
                                        Modifier.align(Alignment.Center).size(44.dp).clip(CircleShape).background(Paper),
                                        Alignment.Center,
                                    ) {
                                        if (isUploading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Ink)
                                        else Icon(Icons.Rounded.Add, null, Modifier.size(22.dp), tint = Ink)
                                    }
                                }
                                Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                                    Text(shot.label, style = dm(14, 700, if (isDone) White else Ink))
                                    Text(
                                        when {
                                            isDone -> stringResource(R.string.photos_added)
                                            shot.required -> stringResource(R.string.photos_required)
                                            else -> stringResource(R.string.photos_optional)
                                        },
                                        style = dm(12, 400, if (isDone) White.copy(alpha = 0.85f) else Slate),
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                problem?.let { Text(it, style = dm(14, 500, Ink)) }
                Spacer(Modifier.height(8.dp))
            }
            Box(Modifier.padding(20.dp)) {
                PrimaryCta(
                    if (requiredDone) stringResource(R.string.photos_continue) else stringResource(R.string.photos_need),
                    enabled = requiredDone && uploading == null && !loading,
                    onClick = onNext,
                )
            }
        }
    }

    viewing?.let { key ->
        saved[key]?.let { bytes ->
            FullScreenPhoto(
                title = carShots.first { it.key == key }.label,
                bytes = bytes,
                onClose = { viewing = null },
                onReplace = { viewing = null; pick(key) },
            )
        }
    }
}

@Composable
fun EligibilityScreen(car: String, plate: String, onBack: () -> Unit, onNext: () -> Unit) {
    val checks = listOf("Identity confirmed", "RC matches the plate", "No outstanding loan", "Insurance is valid", "No accident record")
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { repeat(checks.size) { delay(700); shown = it + 1 } }
    ScreenBox {
        Column(Modifier.fillMaxSize()) {
            TopBar("Checks", onBack = onBack)
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (shown == checks.size) "All clear" else "Checking your car", style = sora(28, 800))
                Text("$car · $plate", style = dm(14, 400, Slate)); Spacer(Modifier.height(6.dp))
                checks.forEachIndexed { i, c ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(White).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(28.dp), Alignment.Center) {
                            AnimatedContent(i < shown, transitionSpec = { scaleIn(spring(0.5f)) togetherWith fadeOut() }, label = "chk") { ok ->
                                if (ok) Tick() else Box(Modifier.size(22.dp).border(2.dp, Line, CircleShape))
                            }
                        }
                        Text(c, style = dm(15, if (i < shown) 700 else 500, if (i < shown) Ink else Slate))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.padding(20.dp)) { PrimaryCta("Go live", enabled = shown == checks.size, onClick = onNext) }
        }
    }
}

@Composable
fun LiveScreen(plate: String, onOffers: () -> Unit, onHome: () -> Unit) {
    val s = remember { Animatable(0f) }
    LaunchedEffect(Unit) { s.animateTo(1f, spring(0.5f)) }
    ScreenBox(Yellow) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.scale(s.value).size(96.dp).clip(CircleShape).background(Ink), Alignment.Center) { Icon(Icons.Rounded.Check, null, tint = Yellow, modifier = Modifier.size(54.dp)) }
            Spacer(Modifier.height(24.dp))
            Text("Your car is live", Modifier.rise(200), style = sora(32, 800))
            Spacer(Modifier.height(8.dp))
            Text("Dealers can bid for the next 24 hours.", Modifier.rise(300), style = dm(16, 500, Ink.copy(alpha = .7f)))
            Spacer(Modifier.height(32.dp))
            Plate(plate, Modifier.fillMaxWidth().rise(400), 80.dp, 50)
            Spacer(Modifier.weight(1f))
            PrimaryCta("See offers", onClick = onOffers)
            Spacer(Modifier.height(12.dp))
            Text("Back to home", Modifier.bounceClick(onClick = onHome).padding(12.dp), style = dm(15, 700))
        }
    }
}

data class Offer(val dealer: String, val price: String, val day: String, val note: String)
val offers = listOf(
    Offer("Sharma Motors (sample)", "—", "Friday", "Sample offer"),
    Offer("Kapoor Auto Traders (sample)", "—", "Saturday", "Sample offer"),
    Offer("Verma Car Bazaar (sample)", "—", "Monday", "Sample offer")
)

@Composable
fun OffersScreen(onTab: (Tab) -> Unit, onOpen: (Int) -> Unit) {
    ScreenBox {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Column(Modifier.padding(top = 26.dp, bottom = 16.dp)) { Text("Offers", style = sora(30, 800)); Text("3 dealers want your Fiesta", style = dm(14, 400, Slate)) }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                offers.forEachIndexed { i, o ->
                    Column(Modifier.rise(i * 120).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(if (i == 0) Ink else White).bounceClick { onOpen(i) }.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(o.dealer, Modifier.weight(1f), style = dm(15, 700, if (i == 0) White else Ink))
                            if (i == 0) Box(Modifier.clip(CircleShape).background(Yellow).padding(horizontal = 10.dp, vertical = 4.dp)) { Text("Best", style = dm(12, 700)) }
                        }
                        Text(o.price, style = sora(30, 800, if (i == 0) Yellow else Ink))
                        Text("Collect ${o.day} · ${o.note}", style = dm(13, 400, if (i == 0) Color(0xFFC9CCD6) else Slate))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            BottomNav(Tab.Offers, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 20.dp), onTab)
        }
    }
}

@Composable
fun OfferDetailScreen(index: Int, onBack: () -> Unit, onAccept: () -> Unit) {
    val o = offers[index]
    ScreenBox {
        Column(Modifier.fillMaxSize()) {
            TopBar("Offer details", onBack = onBack)
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.rise(50).fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Ink).padding(24.dp)) {
                    Text(o.dealer, style = dm(15, 700, Color(0xFFC9CCD6)))
                    Text(o.price, style = sora(44, 800, Yellow))
                    Text("Collect ${o.day}", style = dm(14, 500, White))
                }
                Card(Modifier.rise(150).fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row2("Offer", o.price); Row2("Fees", "₹0"); Row2("Collection", "Free")
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                        Row2("You receive", o.price, bold = true)
                    }
                }
                LaneTip("This is a firm offer. Payment lands within 2 working days of collection.")
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryCta("Accept and book collection", onClick = onAccept)
                SecondaryButton("Back to offers", onClick = onBack)
            }
        }
    }
}

@Composable
private fun Row2(a: String, b: String, bold: Boolean = false) = Row { Text(a, Modifier.weight(1f), style = dm(15, if (bold) 700 else 400, if (bold) Ink else Slate)); Text(b, style = dm(15, 700)) }

@Composable
fun CollectionScreen(onBack: () -> Unit, onConfirm: () -> Unit) {
    var day by remember { mutableStateOf("Fri 24") }
    var slot by remember { mutableStateOf("10:00 – 12:00") }
    ScreenBox {
        Column(Modifier.fillMaxSize()) {
            TopBar("Book collection", onBack = onBack)
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("When suits you?", style = sora(28, 800))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Fri 24", "Sat 25", "Mon 27").forEach { Chip(it, day == it) { day = it } } }
                Text("Time slot", style = dm(13, 700, Color(0xFF3C414C)))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("10:00 – 12:00", "14:00 – 16:00").forEach { Chip(it, slot == it) { slot = it } } }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(White).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    YellowDot(Icons.Rounded.LocationOn, 44.dp)
                    Column { Text("Collection address", style = dm(13, 400, Slate)); Text("12 Canal Street, M1 4BT", style = dm(15, 700)) }
                }
                LaneTip("Lane will only book this after you confirm. You can change it later.")
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.padding(20.dp)) { PrimaryCta("Confirm collection", onClick = onConfirm) }
        }
    }
}

@Composable
fun BookedScreen(onLane: () -> Unit, onHome: () -> Unit) {
    val s = remember { Animatable(0f) }
    LaunchedEffect(Unit) { s.animateTo(1f, spring(0.5f)) }
    ScreenBox {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.scale(s.value).size(96.dp).clip(CircleShape).background(Yellow), Alignment.Center) { Icon(Icons.Rounded.Check, null, tint = Ink, modifier = Modifier.size(54.dp)) }
            Spacer(Modifier.height(24.dp))
            Text("Collection booked", Modifier.rise(200), style = sora(30, 800))
            Spacer(Modifier.height(20.dp))
            Card(Modifier.rise(300).fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row2("Dealer", "Sharma Motors (sample)"); Row2("When", "Fri 24, 10:00 – 12:00"); Row2("You receive", "—")
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Have your RC, keys and ID ready.", style = dm(14, 400, Slate))
            Spacer(Modifier.weight(1f))
            PrimaryCta("Ask Lane a question", onClick = onLane)
            Text("Back to home", Modifier.bounceClick(onClick = onHome).padding(14.dp), style = dm(15, 700))
        }
    }
}


@Composable
fun ProfileScreen(profile: Profile?, onTab: (Tab) -> Unit, onLogout: () -> Unit) {
    ScreenBox {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Column(Modifier.fillMaxWidth().padding(top = 32.dp).rise(50), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(88.dp).clip(CircleShape).background(Yellow), Alignment.Center) { Text(profile?.initial.orEmpty(), style = sora(36, 800)) }
                Spacer(Modifier.height(12.dp)); Text(profile?.name.orEmpty(), style = sora(22, 700))
                Text(profile?.displayPhone.orEmpty(), style = dm(14, 400, Slate))
            }
            Spacer(Modifier.height(24.dp))
            Card(Modifier.rise(200).fillMaxWidth()) {
                listOf(Icons.Rounded.DirectionsCar to "My listings", Icons.Rounded.Description to "Documents", Icons.Rounded.Notifications to "Notifications", Icons.Rounded.HelpOutline to "Help and support").forEach { (ic, t) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(ic, null, Modifier.size(22.dp)); Text(t, Modifier.weight(1f), style = dm(15, 500)); Icon(Icons.Rounded.ChevronRight, null, tint = Slate)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            SecondaryButton("Log out", onClick = onLogout)
            Spacer(Modifier.weight(1f))
            BottomNav(Tab.Profile, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 20.dp), onTab)
        }
    }
}
