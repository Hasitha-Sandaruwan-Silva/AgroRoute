package com.agroroute.features.auth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.agroroute.R
import com.agroroute.core.navigation.Screen
import com.agroroute.core.widgets.LoadingButton
import kotlinx.coroutines.delay

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val tint: Color
)

private val PrimaryGreen = Color(0xFF2E7D32)
private val DarkGreenText = Color(0xFF14532D)
private val SoftMint = Color(0xFFE8F5E9)
private val PageBg = Color(0xFFF6FAF6)
private val BodyTextCharcoal = Color(0xFF4B5563)
private val LineGray = Color(0xFFE5E7EB)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Default.Grass,
        title = "List your harvest",
        body = "Showcase fresh produce from your farm to buyers across Sri Lanka.",
        tint = Color(0xFF2E7D32)
    ),
    OnboardingPage(
        icon = Icons.Default.MonetizationOn,
        title = "Get the best bids",
        body = "Let suppliers compete fairly so you receive the price you deserve.",
        tint = Color(0xFF238A3B)
    ),
    OnboardingPage(
        icon = Icons.Default.LocalShipping,
        title = "Track your orders",
        body = "Follow every order from pickup to delivery in one place.",
        tint = Color(0xFF1B5E20)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(navController: NavController) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    var logoVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(80)
        logoVisible = true
    }

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.82f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "logoAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White,
                        SoftMint.copy(alpha = 0.65f),
                        PageBg
                    )
                )
            )
    ) {
        // Safe ambient background circles (no negative padding crashes)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = 20.dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(SoftMint.copy(alpha = 0.8f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = (-80).dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(PrimaryGreen.copy(alpha = 0.05f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Brand Header with App Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "AgroRoute Logo",
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .size(76.dp)
            )

            Text(
                text = "AgroRoute",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                ),
                color = DarkGreenText,
                modifier = Modifier
                    .alpha(logoAlpha)
                    .padding(top = 6.dp)
            )

            Text(
                text = "From farm to market, fairly priced",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = PrimaryGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(logoAlpha)
                    .padding(top = 2.dp, bottom = 20.dp)
            )

            // Pristine White Card Container for Pager
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = PrimaryGreen.copy(alpha = 0.12f),
                        spotColor = PrimaryGreen.copy(alpha = 0.12f)
                    ),
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, LineGray.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
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

            Spacer(Modifier.height(22.dp))

            // Action Buttons
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

            Spacer(Modifier.height(4.dp))

            TextButton(
                onClick = {
                    navController.navigate(Screen.Login.route) {
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "I already have an account",
                        color = PrimaryGreen,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            page.tint.copy(alpha = 0.12f),
                            SoftMint
                        )
                    )
                )
                .border(1.dp, page.tint.copy(alpha = 0.18f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(page.tint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            ),
            color = DarkGreenText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = BodyTextCharcoal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
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
            val width by animateFloatAsState(
                targetValue = if (selected) 26f else 8f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "dotWidth"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(8.dp)
                    .width(width.dp)
                    .clip(CircleShape)
                    .background(if (selected) PrimaryGreen else Color(0xFFCBD5E1))
            )
        }
    }
}