package com.agroroute.core.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val profileName: String? = null,
    val profileImageUrl: String? = null
)


@Composable
fun AnimatedBottomBar(
    items: List<BottomNavItem>,
    selectedRoute: String,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {

    val selectedIndex = items
        .indexOfFirst {
            selectedRoute.startsWith(
                it.route.substringBefore("/")
            )
        }
        .coerceAtLeast(0)


    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {

        Surface(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),

            // Automatically changes with Light / Dark theme
            color = MaterialTheme.colorScheme.surface,

            contentColor = MaterialTheme.colorScheme.onSurface,

            tonalElevation = 3.dp,
            shadowElevation = 6.dp
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(
                        horizontal = 6.dp,
                        vertical = 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                items.forEachIndexed { index, item ->

                    val selected =
                        index == selectedIndex ||
                                selectedRoute.startsWith(
                                    item.route.substringBefore("{")
                                )

                    BottomNavigationItem(
                        item = item,
                        selected = selected,
                        onClick = {
                            onItemSelected(item)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}


@Composable
private fun BottomNavigationItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val interactionSource = remember {
        MutableInteractionSource()
    }


    val scale by animateFloatAsState(
        targetValue = if (selected) {
            1.05f
        } else {
            1f
        },
        animationSpec = tween(
            durationMillis = 160
        ),
        label = "bottomNavScale"
    )


    val contentColor = if (selected) {

        // Selected icon + text
        MaterialTheme.colorScheme.primary

    } else {

        // Unselected icon + text
        MaterialTheme.colorScheme.onSurfaceVariant
    }


    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(
                RoundedCornerShape(20.dp)
            )
            .semantics {
                this.selected = selected
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab
            ) {
                onClick()
            }
            .padding(
                vertical = 7.dp
            ),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // =====================================================
        // ICON
        // =====================================================

        Icon(
            imageVector = if (selected) {
                item.selectedIcon
            } else {
                item.icon
            },
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
        )


        Spacer(
            modifier = Modifier.height(4.dp)
        )


        // =====================================================
        // LABEL
        // =====================================================

        Text(
            text = item.label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}