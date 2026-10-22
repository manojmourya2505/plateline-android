package com.inouiw.plateline.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inouiw.plateline.data.Listing
import com.inouiw.plateline.data.ListingRepository
import com.inouiw.plateline.data.Profile
import com.inouiw.plateline.data.ProfileRepository
import com.inouiw.plateline.data.Session
import com.inouiw.plateline.dynamic.DynamicRoute
import com.inouiw.plateline.dynamic.tabFromName
import com.inouiw.plateline.ui.screens.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.json.JSONObject

@Composable
fun PlatelineApp() {
    val context = LocalContext.current
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    var plate by remember { mutableStateOf("MH 12 AB 1234") }
    var carName by remember { mutableStateOf("") }
    var estPrice by remember { mutableIntStateOf(0) }
    var estLow by remember { mutableIntStateOf(0) }
    var estHigh by remember { mutableIntStateOf(0) }
    var offer by remember { mutableIntStateOf(0) }
    var listed by remember { mutableStateOf(false) }
    var listing by remember { mutableStateOf<Listing?>(null) }
    var pendingPhone by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf<Profile?>(null) }
    val savedPhone = remember { Session.phone(context) }

    // Returning user: load profile and listing. A live listing opens home straight away.
    LaunchedEffect(savedPhone) {
        if (savedPhone != null) {
            profile = try { ProfileRepository.find(savedPhone) } catch (e: Exception) { null }
            listing = try { ListingRepository.find(savedPhone) } catch (e: Exception) { null }
            listing?.let { carName = it.car; estPrice = it.price }
            if (listing != null) {
                listed = true
                nav.navigate("home") { popUpTo(nav.graph.id) { inclusive = true } }
            }
        }
    }

    fun tab(t: Tab) {
        val route = when (t) {
            Tab.Home -> if (listed) "home" else "homeEmpty"
            Tab.Offers -> "offers"; Tab.Lane -> "lane"; Tab.Profile -> "profile"
        }
        nav.navigate(route) { popUpTo(if (listed) "home" else "homeEmpty") { inclusive = false }; launchSingleTop = true }
    }
    val back: () -> Unit = { nav.popBackStack() }

    val handle: (JSONObject) -> Unit = { a ->
        when (a.optString("type")) {
            "tab" -> tab(tabFromName(a.optString("tab")))
            "navigate" -> {
                val args = a.optJSONObject("args")
                args?.optString("plate")?.let { plate = it.ifBlank { "MH 12 AB 1234" } }
                args?.optString("offer")?.toIntOrNull()?.let { offer = it }
                nav.navigate(a.optString("route"))
            }
        }
    }

    // Existing customer goes straight home. New customer goes through intro and register.
    val finishLogin: (String, Profile?) -> Unit = { phone, existing ->
        pendingPhone = phone
        if (existing != null) {
            profile = existing
            Session.save(context, phone)
            nav.navigate("homeEmpty") { popUpTo(nav.graph.id) { inclusive = true } }
        } else {
            nav.navigate("intro")
        }
    }

    // Sample offers are worked out from the estimate. They are not real dealer offers.
    val base = listing?.price ?: 0
    val laneContext = if (base <= 0) "" else buildString {
        append("Car: $carName, plate $plate. Estimated value about Rs $base (AI estimate). ")
        append("Sample dealer offers (not real): ")
        append("Sharma Motors Rs ${offerRupees(base, 0.95)}, collection Friday; ")
        append("Kapoor Auto Traders Rs ${offerRupees(base, 0.97)}, collection Saturday; ")
        append("Verma Car Bazaar Rs ${offerRupees(base, 0.92)}, collection Monday.")
    }

    val vars = mapOf(
        "offerA" to offerText(base, 0.95),
        "offerB" to offerText(base, 0.97),
        "offerC" to offerText(base, 0.92),
        "firstName" to profile?.firstName.orEmpty(),
        "name" to profile?.name.orEmpty(),
        "listingPlate" to listing?.plate.orEmpty(),
        "listingCar" to listing?.car.orEmpty(),
        "listingPrice" to listing?.priceText.orEmpty(),
        "listingPriceShort" to (listing?.price?.takeIf { it > 0 }?.let { "₹%.1f L".format(it / 100000.0) } ?: "—"),
    )

    NavHost(nav, if (savedPhone != null) "homeEmpty" else "splash",
        enterTransition = { slideInHorizontally(tween(350)) { it / 4 } + fadeIn(tween(350)) },
        exitTransition = { fadeOut(tween(250)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(300)) }) {
        composable("splash") { SplashScreen { nav.navigate("login") { popUpTo("splash") { inclusive = true } } } }
        composable("login") {
            LoginScreen(onContinue = finishLogin, onOtpSent = { phone -> pendingPhone = phone; nav.navigate("otp") }, onGoogle = {})
        }
        composable("otp") {
            OtpScreen(back) {
                scope.launch {
                    val existing = try { ProfileRepository.find(pendingPhone) } catch (e: Exception) { null }
                    finishLogin(pendingPhone, existing)
                }
            }
        }
        composable("intro") { IntroScreen { nav.navigate("register") } }
        composable("register") {
            RegisterScreen(phone = pendingPhone, onBack = back) { name, email, pin ->
                val p = Profile(pendingPhone, name, email, pin)
                profile = p
                ProfileRepository.save(p)
                Session.save(context, pendingPhone)
                nav.navigate("homeEmpty") { popUpTo(nav.graph.id) { inclusive = true } }
            }
        }
        composable("homeEmpty") { DynamicRoute("home_empty", vars, handle) }
        composable("valuation") {
            ValuationScreen(plate, back, onEstimate = { c, lo, hi ->
                carName = c; estLow = lo; estHigh = hi; estPrice = (lo + hi) / 2
                // The value is saved to Firebase straight away.
                val l = Listing(plate, c, estPrice)
                listing = l
                (profile?.phone ?: savedPhone)?.let { ListingRepository.save(it, l) }
            }) { nav.navigate("estimate") }
        }
        composable("estimate") {
            EstimateScreen(carName, estLow, estHigh, back) { nav.navigate("photos") }
        }
        composable("photos") { PhotosScreen(plate, back) { nav.navigate("documents") } }
        composable("documents") {
            DocumentsScreen(plate = plate, onBack = back, onNext = { nav.navigate("eligibility") }, onHelp = { nav.navigate("help") })
        }
        composable("help") { HelpScreen(back) }
        composable("notifications") { NotificationsScreen(back) }
        composable("eligibility") {
            EligibilityScreen(carName, plate, back) {
                listed = true
                val l = Listing(plate, carName, estPrice) // estPrice is the middle of the AI estimate
                listing = l
                (profile?.phone ?: savedPhone)?.let { ListingRepository.save(it, l) }
                nav.navigate("live")
            }
        }
        composable("listingDetail") {
            ListingDetailScreen(carName, plate, listing?.priceText.orEmpty(), back,
                onPhotos = { nav.navigate("photos") }, onDocuments = { nav.navigate("documents") },
                onOffers = { nav.navigate("offers") }, onLane = { nav.navigate("lane") })
        }
        composable("live") { LiveScreen(plate, { nav.navigate("offers") }, { nav.navigate("home") { popUpTo("homeEmpty") { inclusive = true } } }) }
        composable("home") { DynamicRoute("home_listed", vars, handle) }
        composable("offers") { DynamicRoute("offers", vars, handle) }
        composable("offerDetail") { OfferDetailScreen(offer, back) { nav.navigate("collection") } }
        composable("collection") { CollectionScreen(back) { nav.navigate("booked") } }
        composable("booked") { BookedScreen({ nav.navigate("lane") }, { nav.navigate("home") { popUpTo("home") { inclusive = false } } }) }
        composable("lane") { LaneChatScreen(back, laneContext) { nav.navigate("offerCheck") } }
        composable("offerCheck") { OfferCheckScreen(back) }
        composable("profile") {
            ProfileScreen(profile, ::tab) {
                Session.clear(context)
                profile = null
                listing = null
                listed = false
                nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
            }
        }
    }
}

private fun offerRupees(base: Int, factor: Double): Int = (base * factor).roundToInt()

private fun offerText(base: Int, factor: Double): String =
    if (base <= 0) "—" else "₹%,d".format(offerRupees(base, factor))
