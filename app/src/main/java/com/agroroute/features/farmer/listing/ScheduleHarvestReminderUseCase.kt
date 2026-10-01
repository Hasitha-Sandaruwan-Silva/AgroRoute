package com.agroroute.features.farmer.listing

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.notification.HarvestReminderScheduler
import javax.inject.Inject

class ScheduleHarvestReminderUseCase @Inject constructor(
    private val scheduler: HarvestReminderScheduler
) {
    operator fun invoke(listing: HarvestListing) = scheduler.schedule(listing)
}
