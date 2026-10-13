package com.agroroute.core.runtime

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/** Optional scoped UI dependencies; the default path retains the existing Hilt graph. */
val LocalScreenFactory = staticCompositionLocalOf<ViewModelProvider.Factory?> { null }
val LocalScreenOwner = staticCompositionLocalOf<ViewModelStoreOwner?> { null }
val LocalActionUnavailableMessage = staticCompositionLocalOf<String?> { null }
val LocalMutationAllowed = staticCompositionLocalOf { true }
val LocalListingSource = staticCompositionLocalOf<com.agroroute.core.repository.HarvestListingRepository?> { null }
val LocalContactLookup = staticCompositionLocalOf<(suspend (String) -> Pair<String?, String?>)?> { null }

@Composable inline fun <reified T : ViewModel> appViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
    key: String? = null
): T {
    val factory = LocalScreenFactory.current
    return if (factory == null) hiltViewModel(viewModelStoreOwner, key)
        else viewModel(viewModelStoreOwner = LocalScreenOwner.current ?: viewModelStoreOwner,
            key = key, factory = factory)
}
