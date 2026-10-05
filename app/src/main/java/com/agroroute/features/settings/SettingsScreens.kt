package com.agroroute.features.settings

import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agroroute.R
import com.agroroute.core.widgets.AppCard
import com.github.thelacspace.icons.Lucide

private data class FaqItem(val question: String, val answer: String)

private val farmerFaq = listOf(
    FaqItem(
        "How do listings work?",
        "Create a listing with the crop, quantity, reserve price and a photo. Suppliers can bid or send direct order requests, depending on the bidding mode you switch on."
    ),
    FaqItem(
        "What is an expected harvest?",
        "Choose “Expected in the future” when the crop is still growing. Quantity and date stay estimates until you mark the harvest ready and enter the actual quantity."
    ),
    FaqItem(
        "Bids vs order requests",
        "Bids arrive while bidding is open — accept the offer you like to create an order. Order requests arrive on fixed-price listings and must be accepted before an order exists."
    ),
    FaqItem(
        "Marking a harvest ready or failed",
        "Open My Listings and use Mark ready on an expected listing to confirm the real quantity, or Mark failed to make it unavailable and cancel pending pre-harvest requests."
    )
)

private val supplierFaq = listOf(
    FaqItem(
        "How does the marketplace work?",
        "Browse listings, compare price, quantity and location, then place a bid when bidding is open, or send an order request on a fixed-price listing."
    ),
    FaqItem(
        "Bids vs order requests",
        "A bid is an offer per unit that you choose. An order request asks for a fixed quantity at the listed price. Neither one is a payment."
    ),
    FaqItem(
        "How do orders work?",
        "Once a farmer accepts your bid or request, the order appears under Orders with a status timeline: awaiting pickup, in transit, delivered or cancelled."
    ),
    FaqItem(
        "Is any payment taken?",
        "No. Orders are not payments. No payment is processed in this app — agree payment directly with the other party."
    )
)

@Composable
fun HelpSupportScreen(isFarmer: Boolean) {
    val faq = if (isFarmer) farmerFaq else supplierFaq
    SettingsPage(
        title = "Help & Support",
        intro = if (isFarmer) {
            "Quick answers for farmers: listings, expected harvests, bids and order requests."
        } else {
            "Quick answers for suppliers: the marketplace, bids vs order requests, and orders."
        }
    ) {
        faq.forEach { item ->
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.question,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = item.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val versionLabel = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            "${info.versionName} ($code)"
        }.getOrDefault("Unknown")
    }

    SettingsPage(
        title = "About",
        intro = "A fair farm-to-market place for farmers and suppliers in Sri Lanka."
    ) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "AgroRoute logo",
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Version",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = versionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Orders are not payments. No payment is processed in this app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PrivacyScreen() {
    SettingsPage(
        title = "Privacy & Terms",
        intro = "A plain-language summary — not legal advice and not a full privacy policy."
    ) {
        PrivacyCard(icon = Lucide.Description, heading = "What this app stores") {
            Text(
                text = "Your account details (name, email, role), the listings you create, " +
                    "bids, order requests and orders. Drafts you save stay on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        PrivacyCard(icon = Lucide.Image, heading = "Photos") {
            Text(
                text = "Listing photos you pick are uploaded to Cloudinary and served from " +
                    "their CDN so buyers can see your harvest.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        PrivacyCard(icon = Lucide.Lock, heading = "How it is shared") {
            Text(
                text = "Your data is readable only by your own account and the farmers or " +
                    "suppliers you transact with, as enforced by the app's database security rules. " +
                    "No payment details are collected because the app never processes payments.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyCard(
    icon: ImageVector,
    heading: String,
    content: @Composable () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                content()
            }
        }
    }
}

@Composable
private fun SettingsPage(
    title: String,
    intro: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = intro,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        content()
        Spacer(Modifier.height(8.dp))
    }
}
