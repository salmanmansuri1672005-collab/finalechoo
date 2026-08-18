package com.echoos.engine

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.echoos.data.repo.EchoRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Registers geofences and schedules time triggers for active automations.
 * Owner: Rajersh (+ Swati for WorkManager policy). Event-driven only.
 */
class TriggerScheduler(private val context: Context, private val repo: EchoRepository) {

    /** Demo geofence coordinates; in Settings the user pins real places. */
    private val demoPlaces = mapOf(
        "college" to Pair(28.6139, 77.2090),
        "work" to Pair(28.4595, 77.0266),
        "home" to Pair(28.7041, 77.1025),
        "gym" to Pair(28.5355, 77.3910),
    )

    @SuppressLint("MissingPermission") // caller verifies runtime permission first
    suspend fun sync() {
        val active = repo.activeAutomations()

        // --- Geofences ---
        val geoPlaces = active.mapNotNull { it.second.trigger.place }.distinct()
        if (geoPlaces.isNotEmpty() && "location" in repo.enabledCapabilities()) {
            val client = LocationServices.getGeofencingClient(context)
            val fences = geoPlaces.mapNotNull { place ->
                demoPlaces[place]?.let { (lat, lng) ->
                    Geofence.Builder()
                        .setRequestId(place)
                        .setCircularRegion(lat, lng, 150f)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or
                            Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build()
                }
            }
            if (fences.isNotEmpty()) {
                val mutableFlag =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        PendingIntent.FLAG_MUTABLE else 0
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag
                val pi = PendingIntent.getBroadcast(
                    context, 0, Intent(context, GeofenceReceiver::class.java), flags)
                runCatching {
                    client.addGeofences(
                        GeofencingRequest.Builder()
                            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                            .addGeofences(fences).build(), pi)
                }
            }
        }

        // --- Time triggers ---
        active.mapNotNull { it.second.trigger.time }.distinct().forEach { time ->
            val delay = millisUntil(time)
            val work = OneTimeWorkRequestBuilder<TimeTriggerWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putString("time", time).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "time_$time", ExistingWorkPolicy.REPLACE, work)
        }
    }

    private fun millisUntil(hhmm: String): Long {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }
}
