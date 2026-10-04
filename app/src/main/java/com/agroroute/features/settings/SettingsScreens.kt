package com.agroroute.features.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agroroute.R
import com.agroroute.core.widgets.AppCard

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
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
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
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Version",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = versionLabel, style = MaterialTheme.typography.bodyMedium)
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
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "What this app stores",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your account details (name, email, role), the listings you create, " +
                    "bids, order requests and orders. Drafts you save stay on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Photos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Listing photos you pick are uploaded to Cloudinary and served from " +
                    "their CDN so buyers can see your harvest.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "How it is shared",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
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
private fun SettingsPage(
    title: String,
    intro: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = intro,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
        Spacer(Modifier.height(8.dp))
    }
}
