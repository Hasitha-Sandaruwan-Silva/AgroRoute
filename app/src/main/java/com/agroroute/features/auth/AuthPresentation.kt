package com.agroroute.features.auth

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.agroroute.R
import com.agroroute.core.model.UserRole
import com.github.thelacspace.icons.Lucide

/** Presentation shared exclusively by Login and Register. */
@Composable
internal fun AuthLayout(
    title: String,
    subtitle: String,
    snackbar: SnackbarHostState,
    @DrawableRes heroImage: Int,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var snackbarHeight by remember { mutableIntStateOf(0) }
    val snackbarInset = with(LocalDensity.current) { snackbarHeight.toDp() }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing.union(WindowInsets.ime),
        snackbarHost = { AuthMessageHost(snackbar, Modifier.onSizeChanged { snackbarHeight = it.height }) }) { padding ->
        AgroAuthBackground(Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).padding(bottom = snackbarInset),
            contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp)) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Lucide.ArrowLeft, "Back", Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }
                AuthReveal {
                    AgroAuthPhotoHeader(heroImage)
                }
                Spacer(Modifier.height(20.dp))
                AuthReveal(delayMillis = 70) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                        Text(subtitle, style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(24.dp))
                AuthReveal(delayMillis = 110) {
                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
                        shadowElevation = 1.dp) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), content = content)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
internal fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    last: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val iconColor by animateColorAsState(
        if (isError) MaterialTheme.colorScheme.error else if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant, tween(180), label = "authFieldFocus")
    OutlinedTextField(value, onValueChange, label = { Text(label) }, singleLine = true,
        leadingIcon = { Icon(icon, null, Modifier.size(20.dp), tint = iconColor) },
        trailingIcon = if (password) ({
            IconButton(onClick = { visible = !visible }) {
                Crossfade(visible, animationSpec = tween(150), label = "passwordVisibility") { shown ->
                    Icon(if (shown) Lucide.EyeOff else Lucide.Eye,
                        "${if (shown) "Hide" else "Show"} ${label.lowercase()}", Modifier.size(20.dp))
                }
            }
        }) else null,
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType,
            imeAction = if (last) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }, onDone = { focus.clearFocus() }),
        isError = isError, supportingText = supportingText,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.hasFocus })
}

@Composable
internal fun AuthRoleOption(role: UserRole, selected: Boolean, onClick: () -> Unit) {
    val farmer = role == UserRole.FARMER
    val container by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface, tween(180), label = "roleContainer")
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant, tween(180), label = "roleBorder")
    val stroke by animateDpAsState(if (selected) 1.5.dp else 1.dp, tween(180), label = "roleStroke")
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, tween(180), label = "roleIcon")
    Surface(shape = RoundedCornerShape(18.dp),
        color = container,
        border = BorderStroke(stroke, border),
        modifier = Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (farmer) Lucide.Sprout else Lucide.Store, null, Modifier.size(24.dp).scale(scale),
                tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(if (farmer) "Farmer" else "Supplier", style = MaterialTheme.typography.titleSmall)
                Text(if (farmer) "Sell and manage your harvest" else "Discover and source produce", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected, onClick = null)
        }
    }
}
