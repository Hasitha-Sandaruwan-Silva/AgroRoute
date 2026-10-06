package com.agroroute.core.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.core.theme.PillShape
import com.agroroute.core.theme.ThemeMode
import com.github.thelacspace.icons.Lucide

/**
 * Shared Settings block so Farmer and Supplier profiles stay identical.
 *
 * Order: Appearance (dark mode switch + System/Light/Dark), Change password,
 * Edit profile, Help & Support, About, Privacy & Terms. Callers keep their own
 * Account card above this section and the existing Log out button below it.
 */
@Composable
fun SettingsSection(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onChangePassword: () -> Unit,
    onEditProfile: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val systemDark = isSystemInDarkTheme()
    val darkEnabled = if (themeMode == ThemeMode.SYSTEM) systemDark else themeMode == ThemeMode.DARK

    AppCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        Text(
            text = "Appearance",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dark mode",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = darkEnabled,
                onCheckedChange = { checked ->
                    onThemeModeChange(if (checked) ThemeMode.DARK else ThemeMode.LIGHT)
                }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    shape = PillShape,
                    label = {
                        Text(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> "Use system setting"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            }
                        )
                    }
                )
            }
        }
        Text(
            text = "Applies instantly and is remembered on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        SettingsRow(
            icon = Lucide.Lock,
            title = "Change password",
            subtitle = "Email a reset link to your own address",
            onClick = onChangePassword
        )
        SettingsRow(
            icon = Lucide.User,
            title = "Edit profile",
            subtitle = "Name, phone and location",
            onClick = onEditProfile
        )
        SettingsRow(
            icon = Lucide.Info,
            title = "Help & Support",
            subtitle = "How listings, bids and orders work",
            onClick = onHelp
        )
        SettingsRow(
            icon = Lucide.Sprout,
            title = "About",
            subtitle = "Version and app notes",
            onClick = onAbout
        )
        SettingsRow(
            icon = Lucide.FileText,
            title = "Privacy & Terms",
            subtitle = "What this app stores",
            onClick = onPrivacy
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        SettingsRowText(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingsRowText(title: String, subtitle: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
    Icon(
        imageVector = Lucide.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun SettingsSectionPreviewLight() {
    AgroRouteTheme(darkTheme = false) { SettingsSectionPreviewContent() }
}

@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsSectionPreviewDark() {
    AgroRouteTheme(darkTheme = true) { SettingsSectionPreviewContent() }
}

@Composable
private fun SettingsSectionPreviewContent() {
    SettingsSection(
        themeMode = ThemeMode.SYSTEM,
        onThemeModeChange = {},
        onChangePassword = {},
        onEditProfile = {},
        onHelp = {},
        onAbout = {},
        onPrivacy = {},
        modifier = Modifier.padding(16.dp)
    )
}
