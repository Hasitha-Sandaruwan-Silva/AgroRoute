package com.agroroute.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.UserRole
import com.agroroute.core.notification.HarvestReminderScheduler
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.HarvestListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val listingRepository: HarvestListingRepository,
    private val harvestReminderScheduler: HarvestReminderScheduler
) : ViewModel() {

    fun rescheduleHarvestReminders() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId ?: return@launch
            val profile = authRepository.getCurrentUserProfile().first().getOrNull() ?: return@launch
            if (profile.role != UserRole.FARMER || profile.userId != userId) return@launch
            listingRepository.syncListings()
            listingRepository.getListingsByFarmer(userId).first().getOrNull()
                ?.filter { !it.isDraft && it.harvestStatus == HarvestStatus.EXPECTED }
                ?.forEach(harvestReminderScheduler::schedule)
        }
    }
}
