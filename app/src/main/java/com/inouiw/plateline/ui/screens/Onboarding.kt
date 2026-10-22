package com.inouiw.plateline.ui.screens

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inouiw.plateline.R
import com.inouiw.plateline.config.AppConfig
import com.inouiw.plateline.data.Profile
import com.inouiw.plateline.data.ProfileRepository
import com.inouiw.plateline.ui.*
import com.inouiw.plateline.ui.auth.PhoneAuth
import com.inouiw.plateline.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.55f))
        delay(1400)
        onDone()
    }
    ScreenBox(Yellow) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.scale(scale.value)) { LogoMark(Dim.logoLarge) }
            Spacer(Modifier.height(Dim.gapLarge))
            Box(Modifier.rise(300)) { Wordmark(40) }
            Spacer(Modifier.height(Dim.gapSmall))
            Text(stringResource(R.string.splash_tagline), Modifier.rise(500), style = dm(15, 500, Ink.copy(alpha = .7f)))
        }
        Text(stringResource(R.string.splash_demo), Modifier.align(Alignment.BottomCenter).padding(bottom = Dim.gapXLarge), style = dm(12, 400, Ink.copy(alpha = .55f)))
    }
}

@Composable
private fun PillField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
) {
    Column {
        Text(label, style = dm(13, 700, Color(0xFF3C414C)))
        Spacer(Modifier.height(Dim.gapSmall))
        Box(
            Modifier.fillMaxWidth().height(Dim.fieldHeight).clip(CircleShape).background(White)
                .border(1.dp, Line, CircleShape).padding(horizontal = Dim.fieldPadding),
            Alignment.CenterStart
        ) {
            BasicTextField(
                value, onChange, singleLine = true, textStyle = dm(16, 500),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = dm(16, 500, Color(0xFF9AA0AE)))
                    inner()
                }
            )
        }
    }
}

@Composable
private fun PillCta(label: String, onClick: () -> Unit) {
    Row(
        Modifier.height(Dim.buttonHeight).clip(CircleShape).background(Ink)
            .bounceClick(onClick = onClick).padding(start = Dim.pillStart, end = Dim.pillEnd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = sora(16, 600, White))
        Spacer(Modifier.width(Dim.gap + 2.dp))
        Box(Modifier.size(Dim.pillCircle).clip(CircleShape).background(Yellow), Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Ink, modifier = Modifier.size(Dim.iconSmall))
        }
    }
}

@Composable
fun LoginScreen(onContinue: (String, Profile?) -> Unit, onOtpSent: (String) -> Unit, onGoogle: () -> Unit) {
    val activity = LocalContext.current as Activity
    var phone by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val country = stringResource(R.string.login_country)
    val networkError = stringResource(R.string.login_network_error)
    val label = when {
        busy -> stringResource(R.string.login_sending)
        AppConfig.OTP_ENABLED -> stringResource(R.string.login_send)
        else -> stringResource(R.string.login_continue)
    }
    val scope = rememberCoroutineScope()
    val submit: () -> Unit = {
        busy = true
        error = null
        val full = country + phone
        if (!AppConfig.OTP_ENABLED) {
            scope.launch {
                try {
                    val existing = withTimeout(8000) { ProfileRepository.find(full) }
                    busy = false
                    onContinue(full, existing)
                } catch (e: Exception) {
                    busy = false
                    error = networkError
                }
            }
        } else {
            PhoneAuth.send(activity, full, onCodeSent = { busy = false; onOtpSent(full) }, onError = { busy = false; error = it })
        }
    }
    ScreenBox {
        Column(Modifier.fillMaxSize().padding(horizontal = Dim.screenPadding)) {
            Spacer(Modifier.height(Dim.topSpace))
            Box(Modifier.rise(50)) { LogoLockup() }
            Spacer(Modifier.height(Dim.gapXLarge))
            Column(Modifier.rise(150)) {
                Text(stringResource(R.string.login_title), style = sora(32, 700))
                Spacer(Modifier.height(Dim.gap))
                Text(stringResource(R.string.login_subtitle), style = dm(15, 400, Slate))
            }
            Spacer(Modifier.height(Dim.gapXLarge))
            Column(Modifier.rise(280)) {
                Text(stringResource(R.string.login_label), style = dm(13, 700, Color(0xFF3C414C)))
                Spacer(Modifier.height(Dim.gapSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(Dim.gap)) {
                    Box(Modifier.height(Dim.fieldHeight).clip(CircleShape).background(White).border(1.dp, Line, CircleShape).padding(horizontal = Dim.gapMedium), Alignment.Center) {
                        Text(country, style = dm(16, 700))
                    }
                    Box(Modifier.weight(1f).height(Dim.fieldHeight).clip(CircleShape).background(White).border(1.dp, Line, CircleShape).padding(horizontal = Dim.fieldPadding), Alignment.CenterStart) {
                        BasicTextField(
                            phone, { phone = it.filter(Char::isDigit).take(10) }, singleLine = true, textStyle = dm(18, 500),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (phone.isEmpty()) Text(stringResource(R.string.login_placeholder), style = dm(18, 500, Color(0xFF9AA0AE)))
                                inner()
                            }
                        )
                    }
                }
                error?.let {
                    Spacer(Modifier.height(Dim.gap))
                    Text(it, style = dm(13, 500, Color(0xFFB3261E)))
                }
                Spacer(Modifier.height(Dim.gapMedium))
                PrimaryCta(label = label, enabled = phone.length == 10 && !busy, onClick = submit)
                Row(Modifier.padding(vertical = Dim.gapLarge - 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dim.gap)) {
                    Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFD9DCE3)))
                    Text(stringResource(R.string.login_or), style = dm(13, 400, Slate))
                    Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFD9DCE3)))
                }
                Box(Modifier.fillMaxWidth().height(Dim.buttonHeight).clip(CircleShape).background(White).border(1.dp, Line, CircleShape).bounceClick(onClick = onGoogle), Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("G", style = sora(20, 800, Color(0xFF4285F4)))
                        Text(stringResource(R.string.login_google), style = dm(16, 700))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.login_terms), Modifier.fillMaxWidth().padding(bottom = Dim.gapLarge), style = dm(12, 400, Slate), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun OtpScreen(onBack: () -> Unit, onVerified: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val submit: () -> Unit = {
        busy = true
        error = null
        PhoneAuth.verify(code, onDone = { busy = false; onVerified() }, onError = { busy = false; error = it })
    }
    LaunchedEffect(Unit) { if (!AppConfig.OTP_ENABLED) onVerified() }
    LaunchedEffect(code) { if (code.length == 6 && !busy) submit() }
    ScreenBox {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(Dim.gapMedium + 4.dp)) { BackButton(onBack) }
            Column(Modifier.padding(horizontal = Dim.screenPadding)) {
                Column(Modifier.rise(50)) {
                    Text(stringResource(R.string.otp_title), style = sora(32, 700))
                    Spacer(Modifier.height(Dim.gap))
                    Text(stringResource(R.string.otp_sent, PhoneAuth.displayPhone), style = dm(15, 400, Slate))
                }
                Spacer(Modifier.height(Dim.gapXLarge))
                BasicTextField(
                    code, { code = it.filter(Char::isDigit).take(6) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.rise(150),
                    decorationBox = {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dim.gap - 2.dp)) {
                            repeat(6) { i ->
                                val active = i == code.length
                                val shape = RoundedCornerShape(18.dp)
                                Box(
                                    Modifier.weight(1f).height(64.dp).clip(shape).background(White)
                                        .border(if (active) 2.dp else 1.dp, if (active) Ink else Line, shape),
                                    Alignment.Center
                                ) {
                                    if (i < code.length) Text(code[i].toString(), style = sora(24, 700))
                                }
                            }
                        }
                    }
                )
                error?.let {
                    Spacer(Modifier.height(Dim.gapMedium))
                    Text(it, style = dm(14, 500, Color(0xFFB3261E)))
                }
                Spacer(Modifier.height(Dim.gapLarge))
                Text(stringResource(R.string.otp_hint), style = dm(14, 700, Slate))
                Spacer(Modifier.height(Dim.gapXLarge - 12.dp))
                PrimaryCta(
                    label = if (busy) stringResource(R.string.otp_checking) else stringResource(R.string.otp_verify),
                    enabled = code.length == 6 && !busy,
                    onClick = submit
                )
            }
        }
    }
}

private val introTitles = listOf(R.string.intro_title_1, R.string.intro_title_2, R.string.intro_title_3)
private val introBodies = listOf(R.string.intro_body_1, R.string.intro_body_2, R.string.intro_body_3)

@Composable
fun IntroScreen(onDone: () -> Unit) {
    val count = introTitles.size
    val pager = rememberPagerState { count }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage
    ScreenBox {
        Column(Modifier.fillMaxSize().padding(Dim.screenPadding)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.intro_count, page + 1), style = dm(14, 700, Slate))
                Text(stringResource(R.string.intro_skip), Modifier.bounceClick(onClick = onDone).padding(Dim.gapSmall), style = dm(15, 700))
            }
            Spacer(Modifier.height(Dim.gapLarge))
            HorizontalPager(
                pager,
                Modifier.fillMaxWidth().height(Dim.heroHeight).clip(RoundedCornerShape(Dim.heroRadius)).background(White)
            ) { i ->
                when (i) {
                    0 -> IntroPlate()
                    1 -> IntroChat()
                    else -> IntroOffers()
                }
            }
            Spacer(Modifier.height(Dim.gapXLarge))
            Text(stringResource(introTitles[page]), style = sora(30, 700))
            Spacer(Modifier.height(Dim.gap))
            Text(stringResource(introBodies[page]), style = dm(16, 400, Slate))
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dim.gapSmall), verticalAlignment = Alignment.CenterVertically) {
                    repeat(count) { i ->
                        val w by animateDpAsState(if (i == page) Dim.dotActive else Dim.dot, label = "dot")
                        Box(Modifier.height(Dim.dot).width(w).clip(CircleShape).background(if (i == page) Ink else Color(0xFFC9CDD6)))
                    }
                }
                PillCta(if (page == count - 1) stringResource(R.string.intro_start) else stringResource(R.string.intro_next)) {
                    if (page == count - 1) onDone() else scope.launch { pager.animateScrollToPage(page + 1) }
                }
            }
            Spacer(Modifier.height(Dim.gapLarge))
        }
    }
}

@Composable
private fun IntroPlate() {
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(White, Color(0xFFECEEF2), Color(0xFFE0E3E9)))), Alignment.Center) {
        Plate("KA 01 MN 2025", Modifier.width(Dim.heroPlateWidth), Dim.heroPlateHeight, 52)
        Text(
            "₹8,450", style = sora(18, 700, White),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = Dim.screenPadding)
                .clip(RoundedCornerShape(22.dp)).background(Ink).padding(horizontal = Dim.gapMedium, vertical = Dim.gap - 2.dp)
        )
        Text(
            "Ford Fiesta · 2019", style = dm(13, 700),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = Dim.screenPadding, bottom = 44.dp)
                .clip(RoundedCornerShape(18.dp)).background(White).padding(horizontal = Dim.gapMedium - 2.dp, vertical = Dim.gapSmall)
        )
    }
}

@Composable
private fun IntroChat() {
    Column(Modifier.fillMaxSize().background(Color(0xFFF6F7F9)).padding(Dim.heroPadding), verticalArrangement = Arrangement.spacedBy(Dim.gapMedium)) {
        Text(
            stringResource(R.string.intro_chat_question), style = dm(15, 400, White),
            modifier = Modifier.align(Alignment.End).width(210.dp)
                .clip(RoundedCornerShape(topStart = Dim.bubbleRadius, topEnd = Dim.bubbleRadius, bottomEnd = Dim.bubbleTail, bottomStart = Dim.bubbleRadius))
                .background(Ink).padding(horizontal = Dim.gapMedium, vertical = Dim.gap + 2.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dim.gap - 2.dp), verticalAlignment = Alignment.Bottom) {
            YellowDot(Icons.Rounded.AutoAwesome, Dim.avatarSmall)
            Text(
                stringResource(R.string.intro_chat_answer), style = dm(15, 400).copy(lineHeight = 21.sp),
                modifier = Modifier.clip(RoundedCornerShape(topStart = Dim.bubbleRadius, topEnd = Dim.bubbleRadius, bottomEnd = Dim.bubbleRadius, bottomStart = Dim.bubbleTail))
                    .background(White).padding(horizontal = Dim.gapMedium, vertical = Dim.gap + 2.dp).widthIn(max = 230.dp)
            )
        }
        Row(Modifier.padding(start = Dim.chipGutter), horizontalArrangement = Arrangement.spacedBy(Dim.gapSmall)) {
            Text(stringResource(R.string.intro_chip_logbook), style = dm(13, 700),
                modifier = Modifier.clip(CircleShape).background(Yellow).padding(horizontal = Dim.gapMedium - 2.dp, vertical = Dim.gap - 2.dp))
            Text(stringResource(R.string.intro_chip_what), style = dm(13, 700),
                modifier = Modifier.clip(CircleShape).background(White).padding(horizontal = Dim.gapMedium - 2.dp, vertical = Dim.gap - 2.dp))
        }
    }
}

@Composable
private fun IntroOffers() {
    Column(Modifier.fillMaxSize().background(Color(0xFFF6F7F9)).padding(Dim.heroPadding), verticalArrangement = Arrangement.spacedBy(Dim.gap - 2.dp)) {
        Text(stringResource(R.string.intro_offers_count), style = dm(13, 700, Slate))
        IntroOfferRow(stringResource(R.string.intro_dealer_a), "₹8,200", stringResource(R.string.intro_collect_fri), best = false)
        IntroOfferRow(stringResource(R.string.intro_dealer_best), "—", stringResource(R.string.intro_accept), best = true)
        IntroOfferRow(stringResource(R.string.intro_dealer_c), "—", stringResource(R.string.intro_collect_sat), best = false)
    }
}

@Composable
private fun IntroOfferRow(title: String, price: String, note: String, best: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(if (best) Yellow else White).padding(Dim.gapMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = dm(13, 700, if (best) Ink else Slate))
            Text(price, style = if (best) sora(24, 800) else sora(20, 700))
        }
        if (best) {
            Text(note, style = dm(13, 700, White), modifier = Modifier.clip(CircleShape).background(Ink).padding(horizontal = Dim.gapMedium, vertical = Dim.gap - 2.dp))
        } else {
            Text(note, style = dm(13, 400, Slate))
        }
    }
}

@Composable
fun RegisterScreen(phone: String, onBack: () -> Unit, onDone: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(false) }
    ScreenBox {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = Dim.gapMedium, end = Dim.screenPadding, top = Dim.gapMedium + 4.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dim.gap)
            ) {
                BackButton(onBack)
                Box(Modifier.weight(1f)) { StepBar(0.5f) }
                Text(stringResource(R.string.reg_progress), style = dm(13, 700, Slate))
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Dim.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Dim.gapMedium)
            ) {
                Spacer(Modifier.height(Dim.gapLarge - 4.dp))
                Column(Modifier.rise(50)) {
                    Text(stringResource(R.string.reg_title), style = sora(30, 700))
                    Spacer(Modifier.height(Dim.gapSmall))
                    Text(stringResource(R.string.reg_subtitle), style = dm(15, 400, Slate))
                }
                PillField(stringResource(R.string.reg_name), name, { name = it }, stringResource(R.string.reg_name_hint))
                PillField(stringResource(R.string.reg_email), email, { email = it }, stringResource(R.string.reg_email_hint), KeyboardType.Email)
                PillField(stringResource(R.string.reg_phone), phone, { }, stringResource(R.string.reg_phone_hint), KeyboardType.Phone)
                PillField(stringResource(R.string.reg_pin), pin, { pin = it.filter(Char::isDigit).take(6) }, stringResource(R.string.reg_pin_hint), KeyboardType.Number)
                Row(
                    Modifier.fillMaxWidth().bounceClick { terms = !terms }.padding(top = Dim.gapSmall),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(Dim.gap)
                ) {
                    Box(
                        Modifier.size(Dim.checkbox).clip(RoundedCornerShape(9.dp)).background(if (terms) Ink else White)
                            .border(2.dp, Ink, RoundedCornerShape(9.dp)),
                        Alignment.Center
                    ) {
                        if (terms) Icon(Icons.Rounded.Check, null, tint = Yellow, modifier = Modifier.size(14.dp))
                    }
                    Text(stringResource(R.string.reg_terms), style = dm(14, 400, Color(0xFF3C414C)), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(Dim.gapXLarge))
            }
            Box(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Paper.copy(alpha = 0f), Paper)))
                    .padding(start = Dim.screenPadding, end = Dim.screenPadding, top = Dim.gapMedium, bottom = Dim.gapXLarge - 8.dp)
            ) {
                PrimaryCta(label = stringResource(R.string.reg_cta), modifier = Modifier.fillMaxWidth(), enabled = terms, onClick = { onDone(name.trim(), email.trim(), pin) })
            }
        }
    }
}

