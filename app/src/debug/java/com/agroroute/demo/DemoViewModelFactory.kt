package com.agroroute.demo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.agroroute.core.repository.*
import com.agroroute.core.remote.*
import com.agroroute.core.theme.*
import com.agroroute.core.notification.HarvestReminderScheduler
import com.agroroute.core.notification.HarvestReminderStore
import javax.inject.Inject

/** Constructor injection restricted to local interfaces, use cases and ViewModels. Never falls back to Hilt. */
class DemoViewModelFactory(context: Context, val repositories: DemoRepositories) : ViewModelProvider.Factory {
    private val values = mutableMapOf<Class<*>,Any>(
        Context::class.java to context.applicationContext,
        NetworkConnectivityMonitor::class.java to NetworkConnectivityMonitor(context.applicationContext),
        ProfilePhotoUploader::class.java to object : ProfilePhotoUploader {
            override suspend fun upload(uri: android.net.Uri,userId: String): String = error(DemoRepositories.UNAVAILABLE)
        },
        ListingPhotoStorage::class.java to object : ListingPhotoStorage {
            override suspend fun uploadListingPhoto(localUri: String,farmerId: String,listingId: String): String = error(DemoRepositories.UNAVAILABLE)
        },
        ThemeStore::class.java to object : ThemeStore {
            private var value: String? = null
            override fun readMode() = value
            override fun writeMode(value: String) { this.value = value }
        }
    )
    init {
        listOf(AuthRepository::class.java,HarvestListingRepository::class.java,BidRepository::class.java,
            OrderRequestRepository::class.java,OrderRepository::class.java,ReviewRepository::class.java,
            LeaderboardRepository::class.java).forEach { values[it] = repositories.repository(it) }
    }
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = resolve(modelClass) as T
    private fun resolve(type: Class<*>): Any {
        values[type]?.let { return it }
        check(type.name.startsWith("com.agroroute.features.") || type.name.startsWith("com.agroroute.core.usecase.") ||
            type in setOf(ThemePreference::class.java,ThemeViewModel::class.java,HarvestReminderScheduler::class.java,HarvestReminderStore::class.java)) {
            "Dependency is not permitted in local browsing: ${type.name}"
        }
        val constructor = type.constructors.single { it.isAnnotationPresent(Inject::class.java) }
        return constructor.newInstance(*constructor.parameterTypes.map(::resolve).toTypedArray()).also { values[type] = it }
    }
}
