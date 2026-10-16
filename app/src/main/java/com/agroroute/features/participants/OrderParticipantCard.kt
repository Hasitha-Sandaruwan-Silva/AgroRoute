package com.agroroute.features.participants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.ProfilePhoneContact
import com.agroroute.core.components.UserProfileAvatar
import com.agroroute.core.runtime.appViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParticipantCardState(val identity: ParticipantIdentity? = null, val loading: Boolean = true)
@HiltViewModel
class OrderParticipantViewModel @Inject constructor(private val people: ParticipantRepository) : ViewModel() {
    private val state = MutableStateFlow(ParticipantCardState())
    val uiState = state.asStateFlow()
    private var job: Job? = null
    fun load(id: String) {
        job?.cancel(); state.value = ParticipantCardState()
        job = viewModelScope.launch {
            try { state.value = ParticipantCardState(people.identity(id), false) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { state.value = ParticipantCardState(loading = false) }
        }
    }
}

/** Counterpart identity only; the order and existing contact component retain all behavior. */
@Composable
fun OrderParticipantCard(id: String, roleLabel: String, navController: NavController,
    onNameLoaded: (String?) -> Unit = {},
    viewModel: OrderParticipantViewModel = appViewModel(key = "order-participant-$id"), orderId: String = "") {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(id) { viewModel.load(id) }
    LaunchedEffect(state.identity?.name) { onNameLoaded(state.identity?.name) }
    ParticipantIdentityCard(state, roleLabel,
        onProfile = { if (id.isNotBlank()) navController.navigate(ParticipantRoutes.profile(id)) },
        onMessage = { if (id.isNotBlank()) navController.navigate(ParticipantRoutes.chat(id, orderId)) },
        onRetry = { viewModel.load(id) },
        contact = { ProfilePhoneContact(userId = id, personLabel = roleLabel) })
}

@Composable
internal fun ParticipantIdentityCard(state: ParticipantCardState, roleLabel: String,
    onProfile: () -> Unit, onMessage: () -> Unit, onRetry: () -> Unit = {},
    contact: @Composable () -> Unit = {}) {
    AppCard {
        Text("Your $roleLabel", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ParticipantIdentityRow(
            displayName = if (state.loading) "Loading participant..." else state.identity?.name ?: "Participant unavailable",
            roleLabel = roleLabel, imageUrl = state.identity?.imageUrl, points = state.identity?.points,
            onProfile = onProfile, nameMaxLines = 2, modifier = Modifier.testTag("order-participant-identity"))
        if (!state.loading && state.identity == null) TextButton(onClick = onRetry) { Text("Retry participant") }
        OutlinedButton(onClick = onMessage, enabled = state.identity != null, modifier = Modifier.fillMaxWidth()) { Text("Message") }
        contact()
    }
}
