package com.agroroute.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.UserRole
import com.agroroute.core.theme.AgroRouteTheme
import com.github.thelacspace.icons.Lucide

private val chipTones = ChipTone.entries

@Composable
private fun WidgetGalleryPreview() {
    var sampleText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chipTones.take(4).forEach { tone -> StatusChip(label = tone.name, tone = tone) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chipTones.drop(4).forEach { tone -> StatusChip(label = tone.name, tone = tone) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoleBadge(role = UserRole.FARMER)
            RoleBadge(role = UserRole.SUPPLIER)
        }
        AgroTextField(
            value = sampleText,
            onValueChange = { sampleText = it },
            label = "Sample field",
            leadingIcon = Lucide.Mail
        )
        PriceInputField(
            value = sampleText,
            onValueChange = { sampleText = it }
        )
        StatCardRow {
            StatCard(
                label = "Active listings",
                value = "12",
                icon = Lucide.Wheat,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Points earned",
                value = "340",
                icon = Lucide.Trophy,
                modifier = Modifier.weight(1f)
            )
        }
        SectionTitle(title = "Fresh harvest", actionLabel = "See all", onAction = {})
        OrderStatusTimeline(currentStatus = OrderStatus.IN_TRANSIT, modifier = Modifier.fillMaxWidth())
        LoadingButton(text = "Place Bid", isLoading = false, onClick = {}, modifier = Modifier.fillMaxWidth())
        LoadingButton(text = "Publishing…", isLoading = true, onClick = {}, modifier = Modifier.fillMaxWidth())
        EmptyState(
            icon = Lucide.Wheat,
            title = "No listings yet",
            subtitle = "Publish your first harvest to get started."
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoleSelectCard(role = UserRole.FARMER, selected = true, onClick = {}, modifier = Modifier.weight(1f))
            RoleSelectCard(role = UserRole.SUPPLIER, selected = false, onClick = {}, modifier = Modifier.weight(1f))
        }
        Text(text = "Sample caption", style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun WidgetPreviewLight() {
    AgroRouteTheme(darkTheme = false) { WidgetGalleryPreview() }
}

@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WidgetPreviewDark() {
    AgroRouteTheme(darkTheme = true) { WidgetGalleryPreview() }
}
