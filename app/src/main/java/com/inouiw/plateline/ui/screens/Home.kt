package com.inouiw.plateline.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inouiw.plateline.ui.*
import com.inouiw.plateline.ui.theme.*

@Composable
fun GreetingRow(sub: String) {
    Row(Modifier.fillMaxWidth().padding(top = 26.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text("Hey Alex", style = sora(26, 700))
            Text(sub, style = dm(14, 400, Slate))
        }
        Box(Modifier.size(48.dp).clip(CircleShape).background(White), Alignment.Center) { Icon(Icons.Rounded.NotificationsNone, "Notifications", Modifier.size(22.dp)) }
    }
}

@Composable
fun HomeEmptyScreen(onValuation: (String) -> Unit, onTab: (Tab) -> Unit) {
    var plate by remember { mutableStateOf("") }
    ScreenBox {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Box(Modifier.rise(50)) { GreetingRow("Ready to sell your car?") }
            Spacer(Modifier.height(28.dp))
            Card(Modifier.rise(180).fillMaxWidth(), radius = 32.dp) {
                Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 16.dp)) {
                    Text("Sell your car", style = sora(24, 700))
                    Spacer(Modifier.height(6.dp))
                    Text("Enter your number plate for a price estimate.", style = dm(14, 400, Slate))
                    Spacer(Modifier.height(20.dp))
                    PlateInput(plate) { plate = it }
                    Spacer(Modifier.height(16.dp))
                    PrimaryCta("Get my valuation") { onValuation(plate.ifBlank { "MH 12 AB 1234" }) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.rise(300).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(White).bounceClick { onTab(Tab.Lane) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LaneAvatar(44.dp, ping = true)
                Column(Modifier.weight(1f)) {
                    Text("Ask Lane", style = dm(15, 700)); Text("How selling works, fees, papers", style = dm(13, 400, Slate))
                }
                YellowDot(Icons.Rounded.NorthEast)
            }
            Spacer(Modifier.weight(1f))
            BottomNav(Tab.Home, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 20.dp), onTab)
        }
    }
}

@Composable
fun PlateInput(value: String, onChange: (String) -> Unit) {
    val plateStyle = androidx.compose.ui.text.TextStyle(fontFamily = Barlow, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 42.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center, color = Ink)
    // The cursor always stays at the end, so added spaces never move the typing position.
    var field by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(value, androidx.compose.ui.text.TextRange(value.length))) }
    LaunchedEffect(value) {
        if (value != field.text) field = androidx.compose.ui.text.input.TextFieldValue(value, androidx.compose.ui.text.TextRange(value.length))
    }
    Box {
        Plate("", Modifier.fillMaxWidth(), 92.dp, 42)
        Row(Modifier.matchParentSize().padding(start = 42.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(field, { typed ->
                val formatted = formatPlate(typed.text)
                field = androidx.compose.ui.text.input.TextFieldValue(formatted, androidx.compose.ui.text.TextRange(formatted.length))
                onChange(formatted)
            }, singleLine = true,
                textStyle = plateStyle,
                keyboardOptions = KeyboardOptions(keyboardType = if (expectsDigit(field.text)) KeyboardType.Number else KeyboardType.Ascii), modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner -> Box(Modifier.fillMaxWidth(), Alignment.Center) { if (value.isEmpty()) Text("AP 12 PK 1234", style = plateStyle.copy(color = Ink.copy(alpha = .28f))); inner() } })
        }
    }
}

/** True when the next plate character must be a digit (groups: 2 letters, 2 digits, 2 letters, 4 digits). */
fun expectsDigit(text: String): Boolean {
    val n = text.count { it.isLetterOrDigit() }
    return n in 2..3 || n >= 6
}

/** Fixed Indian plate groups: 2 letters, 2 digits, 2 letters, 4 digits (e.g. AP 12 PK 1234). Spaces are added as you type. */
fun formatPlate(raw: String): String {
    val sizes = intArrayOf(2, 2, 2, 4)
    val isLetterGroup = booleanArrayOf(true, false, true, false)
    val groups = Array(4) { StringBuilder() }
    var g = 0
    for (c in raw.uppercase().filter { it.isLetterOrDigit() }) {
        while (g < 4 && groups[g].length == sizes[g]) g++
        if (g >= 4) break
        if (c.isLetter() == isLetterGroup[g]) {
            groups[g].append(c)
        } else if (groups[g].isNotEmpty() && g < 3) {
            g++
            if (c.isLetter() == isLetterGroup[g]) groups[g].append(c) else break
        } else {
            break
        }
    }
    return groups.filter { it.isNotEmpty() }.joinToString(" ") { it.toString() }
}

@Composable
fun HomeScreen(onTab: (Tab) -> Unit, onPhotos: () -> Unit, onOffers: () -> Unit) {
    ScreenBox {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Box(Modifier.rise(50)) { GreetingRow("Your car is live") }
            Spacer(Modifier.height(24.dp))
            Card(Modifier.rise(180).fillMaxWidth(), radius = 32.dp) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Ford Fiesta Zetec", style = sora(20, 700)); Text("2017 · 41,200 km", style = dm(14, 400, Slate))
                        }
                        Box(Modifier.clip(CircleShape).background(Yellow).padding(horizontal = 12.dp, vertical = 6.dp)) { Text("Live", style = dm(12, 700)) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Plate("MH 12 AB 1234", Modifier.fillMaxWidth(), 72.dp, 44)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stat("3", "offers", Modifier.weight(1f).bounceClick(onClick = onOffers)); Stat("—", "best", Modifier.weight(1f)); Stat("21h", "left", Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.rise(320).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(White).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LaneAvatar(44.dp, ping = true)
                Column(Modifier.weight(1f)) { Text("Lane", style = dm(15, 700)); Text("One thing missing.", style = dm(13, 400, Slate)) }
                Box(Modifier.clip(CircleShape).background(Ink).bounceClick(onClick = onPhotos).padding(horizontal = 16.dp, vertical = 11.dp)) { Text("Show me", style = dm(13, 700, White)) }
            }
            Spacer(Modifier.weight(1f))
            BottomNav(Tab.Home, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 20.dp), onTab)
        }
    }
}

@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Paper).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = sora(18, 700)); Text(label, style = dm(12, 400, Slate))
    }
}
