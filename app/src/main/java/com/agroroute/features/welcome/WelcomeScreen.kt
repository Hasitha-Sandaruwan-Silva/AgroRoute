package com.agroroute.features.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.agroroute.R
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.EarthAmber
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.agroroute.core.theme.LeafAccent
import com.agroroute.core.components.LoadingButton
import com.github.thelacspace.icons.Lucide
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val imageUrl: String,
    val title: String,
    val body: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        imageUrl = "https://images.unsplash.com/photo-1464226184884-fa280b87c399?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
        title = "List Your Harvest",
        body = "Showcase fresh produce directly from your farm to verified buyers across Sri Lanka."
    ),
    OnboardingPage(
        // 2 වෙනි එක අලුත් 3D bidding image එක
        imageUrl = "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
        title = "Get The Best Bids",
        body = "Let suppliers compete fairly in open bidding so you receive the true market value."
    ),
    OnboardingPage(
        imageUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
        title = "Track Your Orders",
        body = "Follow every transaction seamlessly from farm pickup to final buyer delivery."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(navController: NavController) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val logoScale = remember { Animatable(0.3f) }
    val logoAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(60f) }
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(400)) }
        launch { logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        delay(350)
        launch { contentAlpha.animateTo(1f, tween(500)) }
        launch { contentOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(Brush.linearGradient(listOf(ForestDeep, ForestMid)))
            ) {
                Box(
                    modifier = Modifier.align(Alignment.TopCenter).size(280.dp).offset(y = (-60).dp)
                        .background(Brush.radialGradient(listOf(Color.White.copy(0.08f), Color.Transparent)))
                )
                Icon(Lucide.Leaf, null, tint = LeafAccent.copy(0.10f), modifier = Modifier.align(Alignment.TopEnd).offset(x = 30.dp, y = (-10).dp).size(160.dp))
                Box(Modifier.align(Alignment.BottomStart).offset(x = 28.dp, y = (-28).dp).size(10.dp).clip(CircleShape).background(EarthAmber.copy(0.85f)))

                Column(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 28.dp, bottom = 58.dp).scale(logoScale.value).alpha(logoAlpha.value),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // LOGO - යට කිසිම circle / shadow නෑ, clean
                    Image(
                        painter = painterResource(id = R.drawable.agroroute1),
                        contentDescription = "AgroRoute Logo",
                        modifier = Modifier.size(142.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("AgroRoute", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, fontSize = 30.sp), color = Color.White)
                    Spacer(Modifier.height(2.dp))
                    Text("From farm to market, fairly priced", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp), color = LeafAccent, textAlign = TextAlign.Center)
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().offset(y = (contentOffsetY.value - 28).dp).alpha(contentAlpha.value).navigationBarsPadding().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .shadow(elevation = 20.dp, shape = RoundedCornerShape(32.dp), ambientColor = ForestDeep.copy(0.15f), spotColor = ForestDeep.copy(0.15f)),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                            OnboardingPageContent(onboardingPages[page], page, isDark)
                        }
                        Spacer(Modifier.height(12.dp))
                        PagerIndicator(onboardingPages.size, pagerState.currentPage)
                    }
                }
                Spacer(Modifier.height(22.dp))
                LoadingButton(text = "Get Started", isLoading = false, onClick = { navController.navigate(Screen.Register.route) { launchSingleTop = true } }, modifier = Modifier.fillMaxWidth().height(56.dp))
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { navController.navigate(Screen.Login.route) { launchSingleTop = true } }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("I already have an account", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp))
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, index: Int, isDark: Boolean) {
    val context = LocalContext.current
    val icons = listOf(Lucide.Leaf, Lucide.DollarSign, Lucide.Truck)
    val stepText = "STEP 0${index + 1}"

    Column(Modifier.fillMaxSize().padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(16.dp))) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context).data(page.imageUrl).crossfade(true).crossfade(400).memoryCacheKey(page.imageUrl).diskCacheKey(page.imageUrl).build(),
                contentDescription = page.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                    }
                }
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(page.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.sp, fontSize = 22.sp), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(page.body, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp, fontSize = 14.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 6.dp))
    }
}

@Composable
private fun PagerIndicator(pageCount: Int, currentPage: Int) {
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(if (selected) 28.dp else 8.dp, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "dotWidth")
            Box(Modifier.padding(horizontal = 4.dp).height(8.dp).width(width).clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)))
        }
    }
}

