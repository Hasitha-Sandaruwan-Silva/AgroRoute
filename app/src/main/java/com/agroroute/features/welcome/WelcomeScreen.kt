package com.agroroute.features.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.agroroute.core.widgets.LoadingButton
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
        imageUrl = "https://sambodhi.co.in/wp-content/uploads/2023/06/Untitled-design-100-1.png",
        title = "Get The Best Bids",
        body = "Let suppliers compete fairly in open bidding so you receive the true market value."
    ),
    OnboardingPage(
        imageUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
        title = "Track Your Orders",
        body = "Follow every transaction seamlessly from farm pickup to final buyer delivery."
    )
)

private val PrimaryGreen = Color(0xFF2E7D32)
private val SoftMint = Color(0xFFE8F5E9)
private val PageBg = Color(0xFFF6FAF6)
private val DarkText = Color(0xFF14532D)
private val BodyText = Color(0xFF4B5563)
private val LineGray = Color(0xFFE5E7EB)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(navController: NavController) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })

    // 🌟 Staggered Entrance Animation States
    val logoScale = remember { Animatable(0.3f) }
    val logoAlpha = remember { Animatable(0f) }

    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(60f) }

    LaunchedEffect(Unit) {
        // Step 1: Logo Pop & Scale Spring
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(400))
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // Step 2: Main Card & Buttons Slide Up & Fade In
        delay(350)
        launch {
            contentAlpha.animateTo(1f, animationSpec = tween(500))
        }
        launch {
            contentOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.White, SoftMint.copy(alpha = 0.7f), PageBg)
                )
            )
    ) {
        // Decorative background blobs
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = 36.dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(SoftMint.copy(alpha = 0.85f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-24).dp, y = (-90).dp)
                .size(170.dp)
                .clip(CircleShape)
                .background(PrimaryGreen.copy(alpha = 0.06f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))

            // 🌟 1. Animated Logo Header (First to appear)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .padding(bottom = 14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.agroroute1),
                    contentDescription = "AgroRoute Logo",
                    modifier = Modifier.size(92.dp)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "AgroRoute",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    ),
                    color = DarkText
                )

                Text(
                    text = "From farm to market, fairly priced",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PrimaryGreen,
                    textAlign = TextAlign.Center
                )
            }

            // 🌟 2. Animated Main Card & Buttons (Slides up smoothly after logo)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .offset(y = contentOffsetY.value.dp)
                    .alpha(contentAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = PrimaryGreen.copy(alpha = 0.10f),
                            spotColor = PrimaryGreen.copy(alpha = 0.10f)
                        ),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineGray.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) { page ->
                            OnboardingPageContent(onboardingPages[page])
                        }

                        Spacer(Modifier.height(10.dp))
                        PagerIndicator(
                            pageCount = onboardingPages.size,
                            currentPage = pagerState.currentPage
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                LoadingButton(
                    text = "Get Started",
                    isLoading = false,
                    onClick = {
                        navController.navigate(Screen.Register.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )

                Spacer(Modifier.height(2.dp))

                TextButton(
                    onClick = {
                        navController.navigate(Screen.Login.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "I already have an account",
                        color = PrimaryGreen,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .shadow(5.dp, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = SoftMint
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(page.imageUrl)
                    .crossfade(true)
                    .crossfade(400)
                    .memoryCacheKey(page.imageUrl)
                    .diskCacheKey(page.imageUrl)
                    .build(),
                contentDescription = page.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(SoftMint, Color.White, SoftMint)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = PrimaryGreen,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            ),
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = BodyText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun PagerIndicator(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(8.dp)
                    .width(if (selected) 26.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (selected) PrimaryGreen else Color(0xFFCBD5E1))
            )
        }
    }
}