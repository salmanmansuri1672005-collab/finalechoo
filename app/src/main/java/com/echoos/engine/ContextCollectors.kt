package com.echoos.engine

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.echoos.EchoApp
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Context collectors (SRS §11.1): event-driven Android components — no polling
 * (battery NFR, SRS §16). Owners: Rajersh (routing) + Tushar (permission gates).
 * Every collector checks the Permission Center switch before recording anything.
 */

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val type = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "location_enter"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "location_exit"
            else -> return
        }
        val place = event.triggeringGeofences?.firstOrNull()?.requestId ?: return
        val app = context.applicationContext as EchoApp
        CoroutineScope(Dispatchers.IO).launch {
            if ("location" in app.repository.enabledCapabilities()) {
                app.engine.onContextEvent(type, place, "geofence")
            }
        }
    }
}

class BluetoothReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission") // name read is wrapped in runCatching; absence is tolerated
    override fun onReceive(context: Context, intent: Intent) {
        val type = when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> "bluetooth_connect"
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> "bluetooth_disconnect"
            else -> return
        }
        val device = runCatching {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.name
        }.getOrNull() ?: "device"
        val app = context.applicationContext as EchoApp
        CoroutineScope(Dispatchers.IO).launch {
            if ("connectivity" in app.repository.enabledCapabilities()) {
                app.engine.onContextEvent(type, device, "bluetooth")
            }
        }
    }
}

/** Fires scheduled (time_schedule) automations. Enqueued by TriggerScheduler. */
class TimeTriggerWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val time = inputData.getString("time") ?: return Result.failure()
        val app = applicationContext as EchoApp
        app.engine.onContextEvent("time_schedule", time, "time")
        return Result.success()
    }
}

/**
 * Notification intelligence source (FR-06). Reads ONLY when the user has both
 * granted listener access AND enabled the 'notifications' source in the
 * Permission Center. Never deletes or alters originals (SRS §9.4).
 */
class EchoNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        if (text.isBlank()) return
        val app = applicationContext as EchoApp
        CoroutineScope(Dispatchers.IO).launch {
            if ("notifications" in app.repository.enabledCapabilities()) {
                app.repository.recordContextEvent(
                    "notification", "$title: $text".take(200), sbn.packageName)
            }
        }
    }
}
