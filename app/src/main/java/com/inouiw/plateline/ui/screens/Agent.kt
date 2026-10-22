package com.inouiw.plateline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.inouiw.plateline.R
import com.inouiw.plateline.data.AgentApi
import com.inouiw.plateline.ui.*
import com.inouiw.plateline.ui.theme.*
import kotlinx.coroutines.launch

/** One message in the Lane chat. mine = sent by the seller. */
data class LaneMsg(val text: String, val mine: Boolean)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LaneChatScreen(onBack: () -> Unit, context: String = "", onOfferCheck: () -> Unit) {
    val scope = rememberCoroutineScope()
    val greeting = stringResource(R.string.lane_greeting)
    val sellingChip = stringResource(R.string.lane_chip_selling)
    val papersChip = stringResource(R.string.lane_chip_papers)
    val offersChip = stringResource(R.string.lane_chip_offers)
    val msgs = remember { mutableStateListOf(LaneMsg(greeting, false)) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val imeVisible = WindowInsets.isImeVisible
    val errorText = stringResource(R.string.lane_error)

    LaunchedEffect(msgs.size) { list.animateScrollToItem(msgs.lastIndex) }

    fun send(text: String) {
        val question = text.trim()
        if (question.isBlank() || busy) return
        // Earlier turns go with the question so Lane remembers the chat. The greeting is not sent.
        val history = msgs.drop(1).map { (if (it.mine) "user" else "assistant") to it.text }
        msgs += LaneMsg(question, true)
        input = ""
        busy = true
        scope.launch {
            val reply = try { AgentApi.chat(question, history, context) } catch (e: Exception) { errorText }
            msgs += LaneMsg(reply, false)
            busy = false
        }
    }

    ScreenBox {
        Column(Modifier.fillMaxSize().imePadding()) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackButton(onBack); LaneAvatar(48.dp)
                Column { Text("Lane", style = sora(18, 700)); Text(stringResource(R.string.lane_sub), style = dm(12, 400, Slate)) }
            }
            LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), state = list, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(msgs) { m ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
                        if (!m.mine) { LaneAvatar(28.dp); Spacer(Modifier.width(8.dp)) }
                        Box(Modifier.widthIn(max = 270.dp).clip(RoundedCornerShape(22.dp)).background(if (m.mine) Ink else White).padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(m.text, style = dm(14, 400, if (m.mine) White else Ink))
                        }
                    }
                }
                if (busy) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LaneAvatar(28.dp)
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.lane_thinking), style = dm(13, 400, Slate))
                        }
                    }
                }
            }
            if (!imeVisible) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(sellingChip, false) { send(sellingChip) }
                Chip(papersChip, false) { send(papersChip) }
            }
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Chip(stringResource(R.string.lane_chip_offer), false, onOfferCheck)
                Chip(offersChip, false) { send(offersChip) }
            }
            }
            Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 8.dp).fillMaxWidth().height(60.dp).clip(CircleShape).background(White).padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(input, { input = it }, Modifier.weight(1f), singleLine = true, textStyle = dm(15, 400), cursorBrush = SolidColor(Ink),
                    decorationBox = { inner -> if (input.isEmpty()) Text(stringResource(R.string.lane_input_hint), style = dm(15, 400, Slate)); inner() })
                Box(Modifier.size(44.dp).clip(CircleShape).background(Yellow).bounceClick { send(input) }, Alignment.Center) { Icon(Icons.Rounded.Send, "Send", Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
fun OfferCheckScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var dealer by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var deductionLabel by remember { mutableStateOf("") }
    var deductionAmount by remember { mutableStateOf("") }
    var validUntil by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var netText by remember { mutableStateOf<String?>(null) }
    var checks by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val offerError = stringResource(R.string.offer_error)

    ScreenBox {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TopBar(stringResource(R.string.offer_check_title), onBack = onBack)
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.offer_check_sub), style = dm(14, 400, Slate))
                OutlinedTextField(dealer, { dealer = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.offer_dealer)) })
                OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.offer_amount)) })
                OutlinedTextField(deductionLabel, { deductionLabel = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.offer_deduction_label)) })
                OutlinedTextField(deductionAmount, { deductionAmount = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.offer_deduction_amount)) })
                OutlinedTextField(validUntil, { validUntil = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.offer_valid_until)) })

                netText?.let { Text(it, style = sora(20, 800)) }
                checks.forEach { Text("• $it", style = dm(14, 400, Ink)) }
                error?.let { Text(it, style = dm(14, 500, Ink)) }
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                PrimaryCta(
                    if (busy) stringResource(R.string.lane_thinking) else stringResource(R.string.offer_check_button),
                    enabled = !busy && dealer.isNotBlank() && amount.isNotBlank(),
                ) {
                    busy = true; error = null
                    val deductions = if (deductionLabel.isNotBlank() && deductionAmount.isNotBlank())
                        listOf(deductionLabel to deductionAmount.toInt()) else emptyList()
                    scope.launch {
                        try {
                            val r = AgentApi.offer(dealer.trim(), amount.toInt(), deductions, validUntil.trim().ifBlank { null })
                            netText = "₹%,d".format(r.getLong("net_after_deductions"))
                            val list = r.getJSONArray("checks")
                            checks = List(list.length()) { list.getString(it) }
                        } catch (e: Exception) {
                            error = offerError
                        }
                        busy = false
                    }
                }
            }
        }
    }
}

// "Your car": opened by tapping the car card on home. Each row goes to the place where that is handled.
@Composable
fun ListingDetailScreen(
    car: String, plate: String, priceText: String,
    onBack: () -> Unit, onPhotos: () -> Unit, onDocuments: () -> Unit, onOffers: () -> Unit, onLane: () -> Unit,
) {
    ScreenBox {
        Column(Modifier.fillMaxSize()) {
            TopBar("Your car", onBack = onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(car, style = sora(22, 800))
                Plate(plate, Modifier.fillMaxWidth(), 72.dp, 42)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.estimate_label), style = dm(13, 700, Slate))
                        Text(priceText, style = sora(24, 800))
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.valuation_note), style = dm(12, 400, Slate))
                    }
                }
                DetailRow(stringResource(R.string.detail_photos), stringResource(R.string.detail_photos_sub), onPhotos)
                DetailRow(stringResource(R.string.detail_documents), stringResource(R.string.detail_documents_sub), onDocuments)
                DetailRow(stringResource(R.string.detail_offers), stringResource(R.string.detail_offers_sub), onOffers)
                DetailRow(stringResource(R.string.detail_lane), stringResource(R.string.detail_lane_sub), onLane)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(White).clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = dm(15, 700))
            Text(sub, style = dm(13, 400, Slate))
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Slate)
    }
}
