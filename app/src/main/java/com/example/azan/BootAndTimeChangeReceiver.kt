package com.example.azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.database.PriyoAzanDatabase
import com.example.database.PriyoAzanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Restores all native prayer alarms automatically after device reboot,
 * package update, manual clock change, time zone change, or date change.
 */
class BootAndTimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val relevant = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED
        )
        if (action !in relevant) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = PriyoAzanRepository(PriyoAzanDatabase.getInstance(context))
                repo.ensureInitialized()
                if (action == Intent.ACTION_TIMEZONE_CHANGED) {
                    val loc = repo.getCurrentLocationSnapshot()
                    if (loc != null) {
                        val systemTz = ZoneId.systemDefault().id
                        if (loc.source != "CITY_PRESET" && loc.timeZoneId != systemTz) {
                            repo.saveCurrentLocation(
                                name = loc.name,
                                country = loc.country,
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                timeZoneId = systemTz,
                                source = loc.source
                            )
                        }
                    }
                }
                val engine = AzanEngine(context, repo)
                engine.rescheduleAllAlarms()
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
