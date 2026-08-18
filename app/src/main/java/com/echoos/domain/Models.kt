package com.echoos.domain

/** Shared domain models + catalogs. Owners: Rajersh (schema) + Tushar (security). */

enum class AutonomyLevel(val key: String) {
    SUGGEST("suggest"),       // only suggest, never execute
    CONFIRM("confirm"),       // execute after explicit user confirmation
    AUTOMATIC("automatic");   // execute automatically (user pre-approved)

    companion object {
        fun from(key: String) = entries.firstOrNull { it.key == key } ?: CONFIRM
    }
}

enum class AutomationStatus(val key: String) {
    DRAFT("draft"), PENDING_CONFIRMATION("pending_confirmation"), ACTIVE("active"),
    RUNNING("running"), SUCCEEDED("succeeded"), FAILED("failed"), DISABLED("disabled");
}

object ActionCatalog {
    /** action type -> (sensitive, requiredCapability?, simulatedOnDevice) */
    val supported: Map<String, Triple<Boolean, String?, Boolean>> = mapOf(
        "focus_mode" to Triple(false, "dnd", false),
        "dnd_on" to Triple(false, "dnd", false),
        "dnd_off" to Triple(false, "dnd", false),
        "silent_mode" to Triple(false, "dnd", false),
        "wifi_toggle" to Triple(false, "connectivity", true),
        "bluetooth_toggle" to Triple(false, "connectivity", true),
        "open_app" to Triple(false, null, false),
        "play_music" to Triple(false, null, false),
        "navigation_start" to Triple(false, "location", false),
        "send_message" to Triple(true, "messaging", false),
        "create_reminder" to Triple(false, null, false),
        "calendar_summary" to Triple(false, "calendar", false),
        "notify_user" to Triple(false, "notifications", false),
        "set_alarm" to Triple(false, null, false),
        "brightness" to Triple(false, "settings", true),
    )

    val supportedTriggers = setOf(
        "location_enter", "location_exit", "time_schedule", "calendar_event",
        "bluetooth_connect", "bluetooth_disconnect", "pattern_detected", "manual",
    )

    fun isSensitive(type: String) = supported[type]?.first == true
    fun capabilityFor(type: String) = supported[type]?.second
    fun isSimulated(type: String) = supported[type]?.third == true
}

data class TriggerSpec(
    val type: String,
    val place: String? = null,
    val time: String? = null,
    val device: String? = null,
    val match: String? = null,
)

data class ActionSpec(
    val type: String,
    val params: Map<String, String> = emptyMap(),
)

data class IntentSpec(
    val trigger: TriggerSpec,
    val actions: List<ActionSpec>,
    val requiredPermissions: List<String>,
    val requiresConfirmation: Boolean,
    val confidence: Double,
    val summary: String,
)
