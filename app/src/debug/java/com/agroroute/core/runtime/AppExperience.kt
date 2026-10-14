package com.agroroute.core.runtime

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agroroute.core.database.AppDatabase
import com.agroroute.core.model.UserRole
import com.agroroute.core.navigation.NavGraph
import com.agroroute.demo.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val LocalEntry = staticCompositionLocalOf<(() -> Unit)?> { null }
class DemoSessionViewModel : ViewModel() {
    val manager = DemoSessionManager()
    override fun onCleared() { BackgroundNetworkAccess.resume() }
}

@Composable fun AppEntryAction(tint: Color = Color.White) {
    val enter = LocalEntry.current ?: return
    TextButton(onClick = enter, modifier = Modifier.fillMaxWidth()) { Text("Try Demo Mode", color = tint) }
}

@Composable fun AppExperience(notificationRoute: String? = null, notificationEventId: String? = null,
    onNotificationRouteHandled: (String?) -> Unit = {}) {
    val holder: DemoSessionViewModel = viewModel()
    val manager = holder.manager
    val state by manager.state.collectAsState()
    var hasEntered by remember { mutableStateOf(false) }
    LaunchedEffect(state.enabled) {
        if (state.enabled) { hasEntered = true; BackgroundNetworkAccess.pause() }
        else BackgroundNetworkAccess.resume()
    }
    val context = LocalContext.current
    val db = remember(context) { DemoDatabase.open(context) }
    DisposableEffect(db) { onDispose { db.close() } }
    DemoExperience(manager,db, onProduction = { enter ->
        CompositionLocalProvider(LocalEntry provides enter) {
            NavGraph(notificationRoute = notificationRoute, notificationEventId = notificationEventId,
                onNotificationRouteHandled = onNotificationRouteHandled, resumeSessionAtStart = !hasEntered)
        }
    })
}

/** Public test seam takes an existing AppDatabase; production UI content remains the real NavGraph. */
@Composable fun DemoExperience(manager: DemoSessionManager, db: AppDatabase,
    onProduction: @Composable (() -> Unit) -> Unit) {
    val state by manager.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var currentNavigation by remember { mutableStateOf<androidx.navigation.NavHostController?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    fun exit() { manager.exit(); BackgroundNetworkAccess.resume(); menu = false; currentNavigation = null }
    fun manage(clear: Boolean, reset: Boolean = false) {
        scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val seeder = DemoDataSeeder(db)
                    if (clear || reset) seeder.clearDemoData()
                    if (!clear) seeder.loadDemoData()
                }
                loaded = !clear
                generation++
                message = if (clear) "Demo data cleared" else "Demo data loaded"
                if (clear) manager.enter()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Could not update local demo data. Try again." }
            finally { busy = false }
        }
    }
    LaunchedEffect(state.enabled) {
        if (state.enabled && !loaded) {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    if (db.harvestListingDao().getListingById("demo_listing_001").first() == null) DemoDataSeeder(db).loadDemoData()
                }
                loaded = true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Could not load local samples. Try Load Demo Data." }
            finally { busy = false }
        }
    }
    if (!state.enabled) {
        onProduction { BackgroundNetworkAccess.pause(); manager.enter() }
        return
    }
    BackHandler(enabled = state.enabled) { exit() }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                    Text("DEMO MODE | ${state.selectedRole?.name ?: "LOCAL"}", style = MaterialTheme.typography.labelMedium)
                    Text(DemoRepositories.UNAVAILABLE, style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = { menu = true }) { Text("Demo menu", maxLines = 1) }
            }
        }
        if (state.selectedRole == null) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Explore AgroRoute", style = MaterialTheme.typography.headlineSmall)
                Text("Local sample records. Backend actions are unavailable.")
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Button(onClick = { manager.select(UserRole.FARMER) }, enabled = loaded && !busy) { Text("Continue as Farmer") }
                Button(onClick = { manager.select(UserRole.SUPPLIER) }, enabled = loaded && !busy) { Text("Continue as Supplier") }
                TextButton(onClick = { manage(false) }, enabled = !busy) { Text("Load Demo Data") }
                TextButton(onClick = ::exit) { Text("Exit Demo Mode") }
                message?.let { Text(it) }
            }
        } else key(state.selectedRole,state.selectedDemoUserId,generation) {
            val navigation = androidx.navigation.compose.rememberNavController()
            SideEffect { currentNavigation = navigation }
            val owner = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
            val factory = remember { DemoViewModelFactory(context,DemoRepositories(db,manager)) }
            DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
            val lookup: suspend (String) -> Pair<String?,String?> = { id ->
                val profile = db.userProfileDao().getUserProfile(id).first()
                profile?.displayName to profile?.phone?.takeIf { it.isNotBlank() }
            }
            CompositionLocalProvider(LocalScreenFactory provides factory, LocalScreenOwner provides owner,
                LocalListingSource provides factory.repositories.repository(com.agroroute.core.repository.HarvestListingRepository::class.java),
                LocalMutationAllowed provides false, LocalActionUnavailableMessage provides DemoRepositories.UNAVAILABLE, LocalContactLookup provides lookup) {
                Box(Modifier.weight(1f)) { NavGraph(navController = navigation) }
            }
        }
    }
    if (menu) AlertDialog(onDismissRequest = { menu = false }, title = { Text("Local Demo Mode") }, text = {
        Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(DemoRepositories.UNAVAILABLE + ": backend transactions, profile edits and notifications.")
            UserRole.entries.forEach { role ->
                TextButton(onClick = { manager.select(role); menu = false }) { Text("Switch to ${role.name.lowercase().replaceFirstChar { it.uppercase() }}") }
            }
            if (state.selectedRole != null) {
                val farmer = state.selectedRole == UserRole.FARMER
                val links = listOf(
                    "Browse listings" to if (farmer) com.agroroute.core.navigation.Screen.MyListings.route else com.agroroute.core.navigation.Screen.SupplierMarketplace.route,
                    "Browse bids" to if (farmer) com.agroroute.core.navigation.Screen.FarmerBids.route else com.agroroute.core.navigation.Screen.SupplierBids.route,
                    "Browse requests" to if (farmer) com.agroroute.core.navigation.Screen.FarmerOrderRequests.route else com.agroroute.core.navigation.Screen.SupplierOrders.route,
                    "Orders and history" to if (farmer) com.agroroute.core.navigation.Screen.FarmerOrders.route else com.agroroute.core.navigation.Screen.SupplierOrders.route,
                    "Profile summary" to if (farmer) com.agroroute.core.navigation.Screen.FarmerProfile.route else com.agroroute.core.navigation.Screen.SupplierProfile.route)
                links.forEach { (label,route) -> TextButton(onClick = {
                    currentNavigation?.navigate(route) { launchSingleTop = true }; menu = false
                }) { Text(label) } }
            }
            // Other fixtures are available to inspect every lifecycle stage while retaining role-correct ownership.
            if (state.selectedRole != null) (1..3).forEach { number ->
                TextButton(onClick = {
                    manager.select(state.selectedRole!!,"demo_${state.selectedRole!!.name.lowercase()}_${number.toString().padStart(3,'0')}")
                    menu = false
                }) { Text("Use sample ${state.selectedRole!!.name.lowercase()} $number") }
            }
            TextButton(onClick = { manage(false) }, enabled = !busy) { Text("Load Demo Data") }
            TextButton(onClick = { manage(false,reset = true) }, enabled = !busy) { Text("Reset Demo Data") }
            TextButton(onClick = { manage(true) }, enabled = !busy) { Text("Clear Demo Data") }
            message?.let { Text(it) }
        }
    }, confirmButton = { TextButton(onClick = ::exit) { Text("Exit Demo Mode") } },
        dismissButton = { TextButton(onClick = { menu = false }) { Text("Close") } })
}
