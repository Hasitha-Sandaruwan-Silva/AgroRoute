package com.agroroute.features.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.agroroute.R
import com.agroroute.core.navigation.Screen
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.shadow

private data class OnboardingPage(val imageUrl: String, val title: String, val body: String)

// HIGH QUALITY images - w=1920 q=90
private val onboardingPages = listOf(
    OnboardingPage(
        imageUrl = "https://images.unsplash.com/photo-1500382017468-9049fed747ef?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=90",
        title = "YOUR FARM,\nSMARTER.",
        body = "Monitor crop health, optimize resources, and increase yield with data driven insights."
    ),
    OnboardingPage(
        imageUrl = "https://images.unsplash.com/photo-1605000797499-95a51c5269ae?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=90",
        title = "GET THE BEST\nBIDS.",
        body = "Showcase your harvest and let verified suppliers compete for the best market price."
    ),
    OnboardingPage(
        imageUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=90",
        title = "TRACK YOUR\nORDERS.",
        body = "Follow every transaction seamlessly from farm pickup to final buyer delivery."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(navController: NavController) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(onboardingPages[page].imageUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.05f), Color.Black.copy(0.15f), Color.Black.copy(0.8f)))))
            }
        }

        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(420.dp).blur(28.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.4f), Color.Black.copy(0.95f)))))

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
            // LOGO ලොකුයි + TEXT එක ලං කරලා ලොකුයි
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.agroroute),
                    contentDescription = "Logo",
                    modifier = Modifier.size(68.dp).clip(CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text("AgroRoute", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = 0.3.sp)
            }

            Spacer(Modifier.weight(1f))
            val currentPage = onboardingPages[pagerState.currentPage]
            Column(Modifier.fillMaxWidth()) {
                Text(currentPage.title, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp), color = Color.White)
                Spacer(Modifier.height(10.dp))
                Text(currentPage.body, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 21.sp), color = Color.White.copy(0.85f))
            }
            Spacer(Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().height(56.dp).shadow(12.dp, RoundedCornerShape(50.dp)),
                shape = RoundedCornerShape(50.dp),
                color = Color(0xFF8BC34A),
                onClick = { navController.navigate(Screen.Register.route) { launchSingleTop = true } }
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Color.Black) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Get Started", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Text(">>>", color = Color.Black.copy(0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    repeat(onboardingPages.size) { index ->
                        val selected = index == pagerState.currentPage
                        Box(Modifier.padding(end = 6.dp).height(6.dp).width(if (selected) 24.dp else 6.dp).clip(CircleShape).background(if (selected) Color.White else Color.White.copy(0.4f)))
                    }
                }
                TextButton(onClick = { navController.navigate(Screen.Login.route) { launchSingleTop = true } }) {
                    Text("I already have an account", color = Color.White.copy(0.9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}