package com.agroroute.features.supplier.orders

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.usecase.GetListingsUseCase
import com.agroroute.core.util.Result
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.map

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OrderListingPresentationEntryPoint {
    fun listings(): GetListingsUseCase
}

/** Read-only existing listing cache for photographs/names, never transaction values. */
@Composable
internal fun orderListingPresentation(): Map<String, HarvestListing> {
    val context = LocalContext.current.applicationContext
    val flow = remember(context) {
        EntryPointAccessors.fromApplication(context, OrderListingPresentationEntryPoint::class.java)
            .listings().getAll().map { result ->
                (result as? Result.Success)?.data.orEmpty().filterNot { it.isDraft }.associateBy { it.listingId }
            }
    }
    val listings by flow.collectAsStateWithLifecycle(initialValue = emptyMap())
    return listings
}
