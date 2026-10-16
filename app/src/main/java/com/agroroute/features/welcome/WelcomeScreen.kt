package com.agroroute.features.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.agroroute.R
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.ForestDeep
import com.agroroute.features.auth.AgroAuthBrand
import com.agroroute.features.auth.AuthReveal
import com.github.thelacspace.icons.Lucide
import kotlinx.coroutines.launch

private data class OnboardingPage(val image: Int, val title: String, val body: String)

// Welcome-only photographs; local assets avoid network/loading delays.
private val onboardingPages = listOf(
    OnboardingPage(R.drawable.welcome_harvest, "Grow with Confidence",
        "Manage your harvests with a smarter farming experience."),
    OnboardingPage(R.drawable.welcome_marketplace, "Trade Made Simple",
        "Connect farmers and suppliers in one trusted marketplace."),
    OnboardingPage(R.drawable.welcome_delivery, "From Farm to Destination",
        "Track orders, deliveries, and every step in between.")
)

@Composable
fun WelcomeScreen(navController: NavController) {
    val pager = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    fun movePage(delta: Int) {
        scope.launch { pager.animateScrollToPage((pager.currentPage + delta).coerceIn(0, onboardingPages.lastIndex)) }
    }
    val background = MaterialTheme.colorScheme.background
    Surface(Modifier.fillMaxSize(), color = background) {
        BoxWithConstraints(Modifier.fillMaxSize().testTag("welcomeScreen")
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Next welcome page") { movePage(1); true },
                    CustomAccessibilityAction("Previous welcome page") { movePage(-1); true })
            }.pointerInput(pager) {
                var travel = 0f
                detectHorizontalDragGestures(onDragStart = { travel = 0f },
                    onHorizontalDrag = { change, amount -> change.consume(); travel += amount },
                    onDragEnd = { if (kotlin.math.abs(travel) > 48.dp.toPx()) movePage(if (travel < 0) 1 else -1) })
            }) {
            val viewportHeight = maxHeight
            val safePadding = WindowInsets.safeDrawing.asPaddingValues()
            val contentHeight = viewportHeight - safePadding.calculateTopPadding() - safePadding.calculateBottomPadding()
            AuthReveal(delayMillis = 70, modifier = Modifier.fillMaxSize()) {
                HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = false) { index ->
                    Box(Modifier.fillMaxSize()) {
                        Image(painterResource(onboardingPages[index].image), null,
                            Modifier.fillMaxWidth().fillMaxHeight(.66f), contentScale = ContentScale.Crop)
                    }
                }
            }
            // Continuous photo-to-surface fade: no enclosing hero card or illustration.
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to ForestDeep.copy(alpha = .24f),
                .28f to background.copy(alpha = .02f),
                .61f to background.copy(alpha = .97f),
                .76f to background,
                1f to background)))
            Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 520.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).heightIn(min = contentHeight)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween) {
                    AuthReveal { AgroAuthBrand(onPhoto = true) }
                    Column(Modifier.fillMaxWidth().padding(top = 150.dp)) {
                        AuthReveal(delayMillis = 160) {
                            AnimatedContent(pager.currentPage, transitionSpec = {
                                fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                            }, label = "welcomePage") { page ->
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(onboardingPages[page].title,
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontSize = 38.sp, lineHeight = 41.sp, letterSpacing = (-1).sp),
                                        fontWeight = FontWeight.ExtraBold)
                                    Text(onboardingPages[page].body,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        AuthReveal(delayMillis = 260, modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { navController.navigate(Screen.Register.route) { launchSingleTop = true } },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .14f),
                                    contentColor = MaterialTheme.colorScheme.onPrimary) {
                                    Icon(Lucide.ArrowRight, null, Modifier.padding(12.dp).size(20.dp))
                                }
                                Text("Get Started", Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Spacer(Modifier.width(44.dp))
                            }
                        }
                        AuthReveal(delayMillis = 340, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.semantics {
                                    contentDescription = "Page ${pager.currentPage + 1} of ${onboardingPages.size}"
                                }, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    repeat(onboardingPages.size) { index ->
                                        val selected = index == pager.currentPage
                                        val width by animateDpAsState(if (selected) 22.dp else 6.dp,
                                            tween(220), label = "pageWidth")
                                        val color by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant, tween(220), label = "pageColor")
                                        Box(Modifier.size(width, 6.dp).clip(CircleShape).background(color))
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { navController.navigate(Screen.Login.route) { launchSingleTop = true } },
                                    modifier = Modifier.weight(3f)) {
                                    Text("I already have an account", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        com.agroroute.core.runtime.AppEntryAction(tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
