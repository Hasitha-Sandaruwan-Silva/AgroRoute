package com.agroroute.core.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Presentation only: reserve the action before measuring the flexible map/editor region. */
@Composable
internal fun LocationPickerLayout(
    modifier: Modifier = Modifier,
    insets: WindowInsets = WindowInsets.safeDrawing.union(WindowInsets.ime),
    search: @Composable () -> Unit,
    results: (@Composable () -> Unit)? = null,
    map: @Composable () -> Unit,
    address: @Composable () -> Unit,
    confirm: @Composable () -> Unit
) {
    val currentMap by rememberUpdatedState(map)
    // Moving between stacked and side-by-side layouts must keep the native map/camera alive.
    val mapContent = remember { movableContentOf { currentMap() } }
    // This dialog has its own window, so it does not inherit a caller Scaffold's padding.
    // Union applies the larger IME/system inset once, including landscape cutouts.
    Column(modifier.fillMaxSize().windowInsetsPadding(insets)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) { search() }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)) {
            val resultHeight = (maxHeight * 0.2f).coerceAtMost(120.dp)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                results?.let {
                    Column(Modifier.fillMaxWidth().heightIn(max = resultHeight)
                        .verticalScroll(rememberScrollState())) { it() }
                }
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    if (maxHeight < 180.dp || (maxWidth >= 480.dp && maxHeight < 360.dp)) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(22.dp))) { mapContent() }
                            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { address() }
                        }
                    } else {
                        val editorHeight = (maxHeight * 0.42f).coerceAtMost(200.dp)
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp))) { mapContent() }
                            Column(Modifier.fillMaxWidth().heightIn(max = editorHeight)
                                .verticalScroll(rememberScrollState())) { address() }
                        }
                    }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) { confirm() }
        }
    }
}
