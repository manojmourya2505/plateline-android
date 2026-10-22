package com.inouiw.plateline.dynamic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.inouiw.plateline.ui.*
import com.inouiw.plateline.ui.screens.PlateInput
import com.inouiw.plateline.ui.screens.Stat
import com.inouiw.plateline.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

/** Loads a layout by id and renders it. */
@Composable
fun DynamicRoute(id: String, vars: Map<String, String>, onAction: (JSONObject) -> Unit) {
    val context = LocalContext.current
    val spec by produceState<JSONObject?>(initialValue = null, key1 = id) {
        value = ScreenRepository.load(context, id)
    }
    spec?.let { DynamicScreen(fill(it, vars), onAction) }
}

fun tabFromName(name: String): Tab = when (name) {
    "offers" -> Tab.Offers
    "lane" -> Tab.Lane
    "profile" -> Tab.Profile
    else -> Tab.Home
}

@Composable
fun DynamicScreen(spec: JSONObject, onAction: (JSONObject) -> Unit) {
    val state = remember(spec.optString("id")) { mutableStateMapOf<String, String>() }
    ScreenBox {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ChildList(spec, state, onAction)
            }
            val nav = spec.optString("nav")
            if (nav.isNotEmpty()) {
                BottomNav(tabFromName(nav), Modifier.align(Alignment.CenterHorizontally).padding(vertical = 20.dp)) { t ->
                    onAction(JSONObject().put("type", "tab").put("tab", t.name.lowercase()))
                }
            }
        }
    }
}

@Composable
private fun ChildList(n: JSONObject, state: MutableMap<String, String>, onAction: (JSONObject) -> Unit) {
    val kids = n.optJSONArray("children") ?: return
    for (i in 0 until kids.length()) DynNode(kids.getJSONObject(i), state, onAction)
}

@Composable
private fun DynNode(n: JSONObject, state: MutableMap<String, String>, onAction: (JSONObject) -> Unit) {
    when (n.optString("type")) {
        "greeting" -> Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(n.optString("title"), style = sora(26, 700))
                Text(n.optString("subtitle"), style = dm(14, 400, Slate))
            }
            Box(Modifier.size(48.dp).clip(CircleShape).background(White).bounceClick { n.optJSONObject("action")?.let { onAction(resolve(it, state)) } }, Alignment.Center) {
                Icon(Icons.Rounded.NotificationsNone, "Notifications", Modifier.size(22.dp))
            }
        }
        "heading" -> Column {
            Text(n.optString("title"), style = sora(n.optInt("size", 28), 800))
            val sub = n.optString("subtitle")
            if (sub.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(sub, style = dm(14, 400, Slate))
            }
        }
        "text" -> Text(
            n.optString("text"),
            style = dm(n.optInt("size", 15), n.optInt("weight", 400), if (n.optString("tone") == "muted") Slate else Ink)
        )
        "spacer" -> Spacer(Modifier.height(n.optInt("height", 12).dp))
        "card" -> Card(Modifier.fillMaxWidth().cardAction(n, state, onAction), radius = n.optInt("radius", 28).dp, bg = if (n.optString("tone") == "dark") Ink else White) {
            Column(Modifier.padding(n.optInt("padding", 20).dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChildList(n, state, onAction)
            }
        }
        "plate" -> Plate(n.optString("text"), Modifier.fillMaxWidth(), n.optInt("height", 80).dp, n.optInt("fontSize", 50))
        "plate_input" -> PlateInput(state["plate"] ?: "") { state["plate"] = it }
        "button" -> PrimaryCta(label = n.optString("label"), modifier = Modifier.fillMaxWidth(),
            onClick = { onAction(resolve(n.optJSONObject("action"), state)) })
        "secondary" -> SecondaryButton(label = n.optString("label"), onClick = { onAction(resolve(n.optJSONObject("action"), state)) })
        "lane_row" -> {
            val go: () -> Unit = { onAction(resolve(n.optJSONObject("action"), state)) }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(White).bounceClick(onClick = go).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LaneAvatar(44.dp, ping = true)
                Column(Modifier.weight(1f)) {
                    Text(n.optString("title"), style = dm(15, 700))
                    Text(n.optString("subtitle"), style = dm(13, 400, Slate))
                }
                val btn = n.optString("buttonLabel")
                if (btn.isNotEmpty()) {
                    Box(Modifier.clip(CircleShape).background(Ink).padding(horizontal = 16.dp, vertical = 11.dp)) {
                        Text(btn, style = dm(13, 700, White))
                    }
                } else {
                    YellowDot(Icons.Rounded.NorthEast)
                }
            }
        }
        "stats" -> {
            val items = n.optJSONArray("items") ?: JSONArray()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0 until items.length()) {
                    val it2 = items.getJSONObject(i)
                    Stat(it2.optString("value"), it2.optString("label"), Modifier.weight(1f))
                }
            }
        }
        "tip" -> LaneTip(n.optString("text"))
        "offer" -> {
            val best = n.optBoolean("best")
            val fg = if (best) White else Ink
            val subColor = if (best) Color(0xFFC9CCD6) else Slate
            val go: () -> Unit = { onAction(resolve(n.optJSONObject("action"), state)) }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(if (best) Ink else White).bounceClick(onClick = go).padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.optString("dealer"), Modifier.weight(1f), style = dm(15, 700, fg))
                    if (best) {
                        Box(Modifier.clip(CircleShape).background(Yellow).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("Best", style = dm(12, 700))
                        }
                    }
                }
                Text(n.optString("price"), style = sora(30, 800, if (best) Yellow else Ink))
                Text(n.optString("collect"), style = dm(13, 400, subColor))
            }
        }
        else -> Unit
    }
}

/** Replaces "$name" in action args with the value typed on this screen. */
private fun resolve(a: JSONObject?, state: Map<String, String>): JSONObject {
    val out = JSONObject()
    if (a == null) return out
    val keys = a.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        if (k != "args") out.put(k, a.get(k))
    }
    val args = a.optJSONObject("args")
    if (args != null) {
        val r = JSONObject()
        val ak = args.keys()
        while (ak.hasNext()) {
            val k = ak.next()
            val v = args.optString(k)
            r.put(k, if (v.startsWith('$')) (state[v.drop(1)] ?: "") else v)
        }
        out.put("args", r)
    }
    return out
}

/** Replaces {name} tokens in the layout with values from the saved profile. */
private fun fill(spec: JSONObject, vars: Map<String, String>): JSONObject {
    var text = spec.toString()
    vars.forEach { (key, value) -> text = text.replace("{$key}", JSONObject.quote(value).drop(1).dropLast(1)) }
    return JSONObject(text)
}

@Composable
private fun Modifier.cardAction(n: JSONObject, state: MutableMap<String, String>, onAction: (JSONObject) -> Unit): Modifier {
    val a = n.optJSONObject("action") ?: return this
    return this.bounceClick { onAction(resolve(a, state)) }
}
