package com.agroroute.core.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.navigation.navArgument
import com.agroroute.features.auth.LoginScreen
import com.agroroute.features.auth.RegisterScreen
import com.agroroute.features.auth.GoogleRoleSelectionScreen
import com.agroroute.features.auth.AuthViewModel
import com.agroroute.features.welcome.WelcomeScreen
import com.agroroute.core.model.UserRole
import com.agroroute.core.util.Constants
import com.agroroute.core.notification.NotificationRoute
import com.agroroute.core.notification.NotificationPermission
import com.agroroute.features.farmer.bids.ViewBidsScreen
import com.agroroute.features.farmer.bids.FarmerBidsScreen
import com.agroroute.features.farmer.home.FarmerHomeScreen
import com.agroroute.features.farmer.calendar.HarvestCalendarScreen
import com.agroroute.features.farmer.listing.CreateListingScreen
import com.agroroute.features.farmer.listing.MyListingsScreen
import com.agroroute.features.farmer.orders.FarmerOrderDetailScreen
import com.agroroute.features.farmer.orders.FarmerOrdersScreen
import com.agroroute.features.farmer.orderrequests.FarmerOrderRequestsScreen
import com.agroroute.features.farmer.profile.FarmerProfileScreen
import com.agroroute.features.farmer.shell.FarmerScaffold
import com.agroroute.features.farmer.shell.farmerRoutes
import com.agroroute.features.supplier.home.SupplierHomeScreen
import com.agroroute.features.supplier.bidding.PlaceBidScreen
import com.agroroute.features.supplier.marketplace.BrowseListingsScreen
import com.agroroute.features.supplier.orderrequest.PlaceOrderRequestScreen
import com.agroroute.features.supplier.orders.SupplierOrderDetailScreen
import com.agroroute.features.supplier.orders.SupplierOrdersScreen
import com.agroroute.features.supplier.profile.SupplierProfileScreen
import com.agroroute.features.supplier.bids.SupplierBidsScreen
import com.agroroute.features.supplier.shell.SupplierScaffold
import com.agroroute.features.supplier.shell.supplierRoutes
import com.agroroute.features.leaderboard.LeaderboardScreen
import com.agroroute.features.reviews.SubmitReviewScreen

private val authMotionRoutes = setOf(Screen.Welcome.route, Screen.Login.route, Screen.Register.route)

private val authRoutes = setOf(
    Screen.Welcome.route,
    Screen.Login.route,
    Screen.Register.route,
    Screen.RoleSelection.route
)

/**
 * Routes reachable by both roles (from either profile screen). They appear in
 * [farmerRoutes] and [supplierRoutes] for shell/title purposes only, so the
 * role guard must skip them or every visit would bounce back to role home.
 */
internal val sharedSettingsRoutes = setOf(
    Screen.HelpSupport.route,
    Screen.About.route,
    Screen.Privacy.route,
    Screen.NotificationSettings.route,
    Screen.NotificationInbox.route,
    Screen.FarmerLeaderboard.route,
    Screen.SupplierLeaderboard.route,
    Screen.SubmitReview.route
)

internal fun roleHomeDestination(role: UserRole?): String? = when (role) {
    UserRole.FARMER -> Screen.FarmerHome.route
    UserRole.SUPPLIER -> Screen.SupplierHome.route
    null -> null
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    notificationRoute: String? = null,
    notificationEventId: String? = null,
    onNotificationRouteHandled: (String?) -> Unit = {},
    resumeSessionAtStart: Boolean = true
) {
    var currentUserRole by remember { mutableStateOf<UserRole?>(null) }
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        com.agroroute.core.notification.NotificationDeliveryRefresh.refresh()
    }
    val mutationAllowed = com.agroroute.core.runtime.LocalMutationAllowed.current

    var showNotificationRationale by remember(authState.user?.userId) { mutableStateOf(false) }
    LaunchedEffect(authState.sessionResolved, authState.user?.userId) {
        if (mutationAllowed && authState.sessionResolved && authState.user != null) {
            (context as? Activity)?.let { activity ->
                showNotificationRationale = !NotificationPermission.isGranted(activity)
            }
        }
    }
    if (showNotificationRationale && mutationAllowed && authState.sessionResolved && authState.user != null) {
        com.agroroute.features.settings.NotificationRationale(
            onEnable = {
                showNotificationRationale = false
                (context as? Activity)?.let { activity -> NotificationPermission.requestIfNeeded(activity) }
            },
            onDismiss = { showNotificationRationale = false }
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        authViewModel.restoreSession()
    }

    LaunchedEffect(authState.user?.role, authState.sessionResolved, currentUserRole, currentRoute, notificationRoute) {
        if (authState.sessionResolved) currentUserRole = authState.user?.role
        val role = if (authState.sessionResolved) authState.user?.role else currentUserRole
        val destination = roleHomeDestination(role)
        when {
            resumeSessionAtStart && destination != null && currentRoute == Screen.Welcome.route && notificationRoute == null -> {
                navController.navigate(destination) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            destination != null && role == UserRole.FARMER &&
                currentRoute in supplierRoutes && currentRoute !in sharedSettingsRoutes -> {
                navController.navigate(destination) { popUpTo(0) { inclusive = true } }
            }
            destination != null && role == UserRole.SUPPLIER &&
                currentRoute in farmerRoutes && currentRoute !in sharedSettingsRoutes -> {
                navController.navigate(destination) { popUpTo(0) { inclusive = true } }
            }
            authState.sessionResolved && destination == null && currentRoute !in authRoutes -> {
                navController.navigate(Screen.Welcome.route) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    LaunchedEffect(notificationRoute, notificationEventId, authState.sessionResolved, authState.user?.role) {
        val requestedRoute = notificationRoute ?: return@LaunchedEffect
        if (!authState.sessionResolved) return@LaunchedEffect
        val role = authState.user?.role
        val allowed = role != null && NotificationRoute.isAllowed(requestedRoute, role)
        if (allowed) {
            if (NotificationRoute.isChatRoute(requestedRoute) && role != null) {
                roleHomeDestination(role)?.let { home ->
                    navController.navigate(home) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                navController.navigate(requestedRoute) { launchSingleTop = true }
            } else navController.navigate(requestedRoute) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
        onNotificationRouteHandled(notificationEventId.takeIf { allowed })
    }

    val hostContent: @Composable (PaddingValues) -> Unit = { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Welcome.route, // 👈 නිවැරදිව Welcome එකට යොමු කර ඇත (කොමාව සහිතව)
                modifier = Modifier.padding(paddingValues).consumeWindowInsets(paddingValues)
            ) {

                composable(com.agroroute.features.participants.ParticipantRoutes.PROFILE,
                    arguments = listOf(navArgument("participantId") { type = NavType.StringType })) { entry ->
                    entry.arguments?.getString("participantId")?.let { ParticipantPresentation(currentUserRole) { com.agroroute.features.participants.ParticipantProfileScreen(it, navController) } }
                }
                composable(com.agroroute.features.participants.ParticipantRoutes.CHAT,
                    arguments = listOf(navArgument("participantId") { type = NavType.StringType },
                        navArgument("orderId") { type = NavType.StringType; defaultValue = "" })) { entry ->
                    entry.arguments?.getString("participantId")?.let { ParticipantPresentation(currentUserRole) { com.agroroute.features.participants.ChatScreen(it, navController, orderId = entry.arguments?.getString("orderId").orEmpty()) } }
                }
                composable(com.agroroute.features.participants.ParticipantRoutes.MESSAGES) {
                    ParticipantPresentation(currentUserRole) { com.agroroute.features.participants.MessagesScreen(navController) }
                }

                composable(Screen.Welcome.route,
                    enterTransition = { if (initialState.destination.route in authMotionRoutes)
                        fadeIn(tween(260)) + slideInHorizontally(tween(260)) { it / 14 } else null },
                    exitTransition = { if (targetState.destination.route in authMotionRoutes)
                        fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { -it / 18 } else null },
                    popEnterTransition = { if (initialState.destination.route in authMotionRoutes)
                        fadeIn(tween(260)) + slideInHorizontally(tween(260)) { -it / 14 } else null },
                    popExitTransition = { if (targetState.destination.route in authMotionRoutes)
                        fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { it / 18 } else null }
                ) {
                    WelcomeScreen(navController = navController)
                }

                composable(Screen.Login.route,
                    enterTransition = { if (initialState.destination.route in authMotionRoutes)
                        fadeIn(tween(260)) + slideInHorizontally(tween(260)) { it / 14 } else null },
                    exitTransition = { if (targetState.destination.route in authMotionRoutes)
                        fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { -it / 18 } else null },
                    popEnterTransition = { if (initialState.destination.route in authMotionRoutes)
                        fadeIn(tween(260)) + slideInHorizontally(tween(260)) { -it / 14 } else null },
                    popExitTransition = { if (targetState.destination.route in authMotionRoutes)
                        fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { it / 18 } else null }
                ) {
                    LoginScreen(
                        navController = navController,
                        onLoginSuccess = { role -> currentUserRole = role },
                        onGoogleRoleRequired = {
                            navController.navigate(Screen.RoleSelection.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.RoleSelection.route) {
                    GoogleRoleSelectionScreen(
                        navController = navController,
                        viewModel = authViewModel
                    )
                }

                composable(Screen.Register.route,
                    enterTransition = { if (initialState.destination.route in authMotionRoutes)
                        fadeIn(tween(260)) + slideInHorizontally(tween(260)) { it / 14 } else null },
                    exitTransition = { if (targetState.destination.route in authMotionRoutes)
                        fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { -it / 18 } else null },
                    popEnterTransition = { if (initialState.destination.route in authMotionRoutes)
                        fadeIn(tween(260)) + slideInHorizontally(tween(260)) { -it / 14 } else null },
                    popExitTransition = { if (targetState.destination.route in authMotionRoutes)
                        fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { it / 18 } else null }
                ) {
                    RegisterScreen(
                        navController = navController,
                        onRegisterSuccess = { role -> currentUserRole = role }
                    )
                }

                composable(Screen.FarmerHome.route) {
                    FarmerHomeScreen(navController = navController)
                }

                composable(Screen.SupplierHome.route) {
                    SupplierHomeScreen(navController = navController)
                }

                composable(Screen.FarmerProfile.route) {
                    FarmerProfileScreen(
                        navController = navController,
                        userRole = currentUserRole
                    )
                }

                composable(Screen.SupplierProfile.route) {
                    SupplierProfileScreen(
                        navController = navController,
                        userRole = currentUserRole
                    )
                }

                composable(Screen.CreateListing.route) {
                    CreateListingScreen(navController = navController)
                }

                composable(
                    route = Screen.EditListing.route,
                    arguments = listOf(navArgument(Constants.ARG_LISTING_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val listingId = backStackEntry.arguments?.getString(Constants.ARG_LISTING_ID)
                        ?: return@composable
                    CreateListingScreen(navController = navController, editListingId = listingId)
                }

                composable(Screen.MyListings.route) {
                    MyListingsScreen(navController = navController)
                }

                composable(Screen.FarmerBids.route) {
                    FarmerBidsScreen(navController = navController)
                }

                composable(Screen.HarvestCalendar.route) {
                    HarvestCalendarScreen(navController = navController)
                }

                composable(
                    route = Screen.ViewBids.route,
                    arguments = listOf(navArgument(Constants.ARG_LISTING_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val listingId = backStackEntry.arguments?.getString(Constants.ARG_LISTING_ID) ?: return@composable
                    ViewBidsScreen(listingId = listingId, navController = navController)
                }

                composable(Screen.FarmerOrders.route) {
                    FarmerOrdersScreen(navController = navController)
                }

                composable(Screen.FarmerOrderRequests.route) {
                    FarmerOrderRequestsScreen(navController = navController)
                }

                composable(
                    route = Screen.FarmerOrderDetail.route,
                    arguments = listOf(navArgument(Constants.ARG_ORDER_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString(Constants.ARG_ORDER_ID) ?: return@composable
                    FarmerOrderDetailScreen(orderId = orderId, navController = navController)
                }

                composable(Screen.SupplierMarketplace.route) {
                    BrowseListingsScreen(navController = navController)
                }

                composable(
                    route = Screen.PlaceBid.route,
                    arguments = listOf(navArgument(Constants.ARG_LISTING_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val listingId = backStackEntry.arguments?.getString(Constants.ARG_LISTING_ID) ?: return@composable
                    PlaceBidScreen(listingId = listingId, navController = navController)
                }

                composable(
                    route = Screen.PlaceOrderRequest.route,
                    arguments = listOf(navArgument(Constants.ARG_LISTING_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val listingId = backStackEntry.arguments?.getString(Constants.ARG_LISTING_ID) ?: return@composable
                    PlaceOrderRequestScreen(listingId = listingId, navController = navController)
                }

                composable(Screen.SupplierOrders.route) {
                    SupplierOrdersScreen(navController = navController)
                }

                composable(Screen.SupplierBids.route) {
                    SupplierBidsScreen(navController = navController)
                }

                composable(
                    route = Screen.SupplierOrderDetail.route,
                    arguments = listOf(navArgument(Constants.ARG_ORDER_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString(Constants.ARG_ORDER_ID) ?: return@composable
                    SupplierOrderDetailScreen(orderId = orderId, navController = navController)
                }

                composable(Screen.HelpSupport.route) {
                    val isFarmer = currentUserRole == UserRole.FARMER
                    com.agroroute.features.settings.HelpSupportScreen(isFarmer = isFarmer)
                }

                composable(Screen.About.route) {
                    com.agroroute.features.settings.AboutScreen()
                }

                composable(Screen.Privacy.route) {
                    com.agroroute.features.settings.PrivacyScreen()
                }

                composable(Screen.NotificationSettings.route) {
                    if (mutationAllowed) com.agroroute.features.settings.NotificationSettingsScreen()
                    else androidx.compose.material3.Text(com.agroroute.core.runtime.LocalActionUnavailableMessage.current.orEmpty())
                }

                composable(Screen.NotificationInbox.route) {
                    com.agroroute.features.settings.NotificationInboxScreen(navController)
                }

                composable(Screen.FarmerLeaderboard.route) { entry ->
                    LeaderboardScreen(role = entry.savedStateHandle.get<String>("leaderboardRole") ?: "FARMER", onParticipant = { id -> navController.navigate(com.agroroute.features.participants.ParticipantRoutes.profile(id)) })
                }

                composable(Screen.SupplierLeaderboard.route) { entry ->
                    LeaderboardScreen(role = entry.savedStateHandle.get<String>("leaderboardRole") ?: "SUPPLIER", onParticipant = { id -> navController.navigate(com.agroroute.features.participants.ParticipantRoutes.profile(id)) })
                }

                composable(
                    route = Screen.SubmitReview.route,
                    arguments = listOf(
                        navArgument(Constants.ARG_ORDER_ID) { type = NavType.StringType },
                        navArgument(Constants.ARG_USER_ID) { type = NavType.StringType },
                        navArgument(Constants.ARG_USER_NAME) { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString(Constants.ARG_ORDER_ID) ?: return@composable
                    val revieweeId = backStackEntry.arguments?.getString(Constants.ARG_USER_ID) ?: return@composable
                    val revieweeName = backStackEntry.arguments?.getString(Constants.ARG_USER_NAME) ?: return@composable
                    SubmitReviewScreen(
                        orderId = orderId,
                        revieweeId = revieweeId,
                        revieweeName = revieweeName,
                        navController = navController
                    )
                }
            }
        }
    }

    val shellRole = if (authState.sessionResolved) authState.user?.role else currentUserRole

    when {
        currentRoute in sharedSettingsRoutes && shellRole == UserRole.SUPPLIER -> SupplierScaffold(
            currentRoute = currentRoute.orEmpty(),
            navController = navController,
            onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(Screen.SupplierHome.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            content = hostContent
        )
        currentRoute in farmerRoutes -> FarmerScaffold(
            currentRoute = currentRoute.orEmpty(),
            navController = navController,
            onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(Screen.FarmerHome.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            content = hostContent
        )
        currentRoute in supplierRoutes -> SupplierScaffold(
            currentRoute = currentRoute.orEmpty(),
            navController = navController,
            onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(Screen.SupplierHome.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            content = hostContent
        )
        else -> hostContent(PaddingValues())
    }
}

@Composable
private fun ParticipantPresentation(role: UserRole?, content: @Composable () -> Unit) {
    if (role == UserRole.SUPPLIER) androidx.compose.runtime.CompositionLocalProvider(com.agroroute.features.supplier.components.LocalSupplierPresentation provides true, content = content)
    else com.agroroute.features.farmer.components.FarmerPresentation(content = content)
}
