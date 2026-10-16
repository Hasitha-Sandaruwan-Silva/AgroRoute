package com.agroroute.core.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.theme.*
import com.github.thelacspace.icons.Lucide

/**
 * A single reusable feature surface for Home, profile and compact highlights.
 */
@Composable
fun PremiumHero(
    modifier: Modifier = Modifier,
    brush: Brush = AgroGradients.forest,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(brush)
    ) {
        Canvas(Modifier.matchParentSize()) {
            // Quiet contour lines echo cultivated land without competing with text.
            repeat(4) { index ->
                drawCircle(
                    CreamInk.copy(alpha = 0.045f),
                    radius = size.width * (0.35f + index * 0.11f),
                    center = Offset(
                        size.width * 1.05f,
                        size.height * 0.12f
                    ),
                    style = Stroke(1.dp.toPx())
                )
            }
        }

        androidx.compose.runtime.CompositionLocalProvider(
            LocalContentColor provides if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                MaterialTheme.colorScheme.onSurface
            } else CreamInk
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
fun DashboardAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    earth: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalAgroExtendedColors.current

    AppCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AgroIconBox(
                icon,
                containerColor = if (earth) {
                    colors.accentIconContainer
                } else {
                    colors.iconContainer
                },
                contentColor = if (earth) {
                    colors.accentIconOnContainer
                } else {
                    colors.iconOnContainer
                }
            )

            Spacer(Modifier.width(12.dp))

            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Lucide.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Only the saved address is prominent; coordinates stay in the map picker.
 */
fun profileLocationLabel(value: String?): String {
    if (value.isNullOrBlank()) {
        return "Add your location"
    }

    val selected = SelectedLocation.fromStoredText(value)

    return selected?.address
        ?.takeIf(String::isNotBlank)
        ?: if (selected != null) {
            "Saved map location"
        } else {
            value
        }
}

@Composable
fun ProfileLocationCard(
    value: String?,
    onClick: () -> Unit
) {
    DashboardAction(
        title = "Your location",
        subtitle = profileLocationLabel(value),
        icon = Lucide.MapPin,
        onClick = onClick
    )
}