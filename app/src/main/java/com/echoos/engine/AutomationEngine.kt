package com.echoos.engine

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import com.echoos.data.repo.EchoRepository
import com.echoos.domain.ActionCatalog
import com.echoos.domain.ActionSpec
import com.echoos.domain.AutonomyEngine
import com.echoos.domain.AutonomyLevel
import com.echoos.domain.IntentSpec

/**
 * The controlled execution loop (SRS §1, §10):
 * sense → understand → decide → ask/verify → execute → log → learn.
 *
 * TriggerManager (Rajersh) routes context events to matching automations.
 * ActionExecutor (Rajersh + Tushar) performs ONLY catalog actions, after the
 * AutonomyEngine gate. Simulated actions are labeled, never claimed as real.
 */
class AutomationEngine(
    private val context: Context,
    private val repo: EchoRepository,
) {

    /** Called by receivers/workers/simulator whenever a context event occurs. */
    suspend fun onContextEvent(type: String, value: String?, source: String,
                               simulated: Boolean = false) {
        repo.recordContextEvent(type, value, source, simulated)
        val enabled = repo.enabledCapabilities()

        for ((entity, intent) in repo.activeAutomations()) {
            if (!matches(intent, type, value)) continue
            val decision = AutonomyEngine.decide(
                intent, AutonomyLevel.from(entity.autonomy), enabled)
            val why = "Trigger '$type${value?.let { ":$it" } ?: ""}' matched " +
                "'${entity.name}'. ${decision.explanation}"
            when (decision) {
                is AutonomyEngine.Decision.Execute -> {
                    repo.setAutomationStatus(entity.id, "running")
                    val ok = execute(intent, entity.id, why)
                    repo.setAutomationStatus(entity.id, "active")
                    if (!ok) repo.log(entity.id, "executed_failure",
                        "$why One or more actions failed — NOT reported as success.")
                }
                is AutonomyEngine.Decision.AskConfirmation -> {
                    repo.setAutomationStatus(entity.id, "pending_confirmation")
                    repo.log(entity.id, "suggested", why)
                    notifyUser("EchoOS — confirm automation",
                        "${entity.name}: tap to review and approve.")
                }
                is AutonomyEngine.Decision.SuggestOnly ->
                    repo.log(entity.id, "suggested", why)
                is AutonomyEngine.Decision.Blocked ->
                    repo.log(entity.id, "blocked", why)
            }
        }
    }

    /** User approved a pending automation from the UI. */
    suspend fun executeConfirmed(automationId: Long) {
        val pair = repo.activeAutomations().firstOrNull { it.first.id == automationId }
        val (entity, intent) = pair ?: run {
            val e = com.echoos.data.EchoDatabase.get(context).automationDao().byId(automationId)
                ?: return
            e to repo.entityToIntent(e)
        }
        repo.setAutomationStatus(entity.id, "running")
        val ok = execute(intent, entity.id, "User confirmed '${entity.name}'.")
        repo.setAutomationStatus(entity.id, "active")
        if (!ok) repo.log(entity.id, "executed_failure",
            "User confirmed '${entity.name}' but an action failed.")
    }

    private fun matches(intent: IntentSpec, type: String, value: String?): Boolean {
        val t = intent.trigger
        if (t.type != type) return false
        return when (type) {
            "location_enter", "location_exit" ->
                t.place.equals(value, ignoreCase = true) || t.place == null
            "bluetooth_connect", "bluetooth_disconnect" ->
                t.device == null || value?.contains(t.device, true) == true
            "time_schedule" -> t.time == value
            else -> true
        }
    }

    /** Executes catalog actions only. Returns false if ANY action failed (FR: a
     *  failed automation must never be reported as successful, SRS §16). */
    private suspend fun execute(intent: IntentSpec, automationId: Long?,
                                why: String): Boolean {
        var allOk = true
        for (action in intent.actions) {
            val simulated = ActionCatalog.isSimulated(action.type)
            val ok = try {
                perform(action, simulated)
            } catch (e: Exception) {
                false
            }
            allOk = allOk && ok
            repo.log(
                automationId,
                if (ok) "executed_success" else "executed_failure",
                "$why Action '${action.type}' " +
                    (if (simulated) "[SIMULATED] " else "") +
                    (if (ok) "completed." else "FAILED."),
                simulated = simulated,
            )
        }
        return allOk
    }

    private fun perform(action: ActionSpec, simulated: Boolean): Boolean {
        if (simulated) return true // clearly-labeled demo simulation (SRS §5, §15)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when (action.type) {
            "dnd_on", "focus_mode" -> {
                if (nm.isNotificationPolicyAccessGranted) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    true
                } else { notifyUser("EchoOS", "Grant Do-Not-Disturb access to enable focus mode."); false }
            }
            "dnd_off" -> {
                if (nm.isNotificationPolicyAccessGranted) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL); true
                } else false
            }
            "silent_mode" -> {
                am.ringerMode = if (action.params["enabled"] != "false")
                    AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
                true
            }
            "open_app" -> {
                val name = action.params["app_name"] ?: action.params["package"] ?: return false
                val pm = context.packageManager
                val launch = pm.getLaunchIntentForPackage(name)
                    ?: pm.getInstalledApplications(0)
                        .firstOrNull { pm.getApplicationLabel(it).toString().equals(name, true) }
                        ?.let { pm.getLaunchIntentForPackage(it.packageName) }
                launch?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(it); true
                } ?: false
            }
            "play_music" -> {
                val i = Intent("android.intent.action.MUSIC_PLAYER")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(i) }.isSuccess
            }
            "navigation_start" -> {
                val dest = action.params["destination"] ?: "home"
                val i = Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("google.navigation:q=$dest"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(i) }.isSuccess
            }
            "create_reminder", "notify_user", "calendar_summary", "send_message" -> {
                // send_message only reaches here AFTER explicit confirmation; MVP
                // surfaces a compose-ready notification instead of silent sending.
                notifyUser("EchoOS — ${action.type.replace('_', ' ')}",
                    action.params["title"] ?: action.params["text"] ?: "Done"); true
            }
            "set_alarm" -> {
                val t = (action.params["time"] ?: "07:00").split(":")
                val i = Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
                    .putExtra(android.provider.AlarmClock.EXTRA_HOUR, t[0].toInt())
                    .putExtra(android.provider.AlarmClock.EXTRA_MINUTES, t.getOrElse(1) { "0" }.toInt())
                    .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(i) }.isSuccess
            }
            else -> false // not in catalog — must never execute
        }
    }

    @SuppressLint("MissingPermission") // POST_NOTIFICATIONS is requested progressively by the UI
    private fun notifyUser(title: String, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "echoos"
        nm.createNotificationChannel(
            NotificationChannel(channelId, "EchoOS", NotificationManager.IMPORTANCE_DEFAULT))
        nm.notify(System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(text).build())
    }
}
