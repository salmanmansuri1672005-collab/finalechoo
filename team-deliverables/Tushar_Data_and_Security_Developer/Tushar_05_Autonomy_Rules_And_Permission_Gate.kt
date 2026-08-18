package com.echoos.domain

/**
 * Autonomy + permission gate (SRS §9.7, FR-12, FR-13). Owner: Tushar.
 * Decides what may happen when an automation's trigger fires.
 */
object AutonomyEngine {

    sealed class Decision(val explanation: String) {
        class Execute(why: String) : Decision(why)
        class AskConfirmation(why: String) : Decision(why)
        class SuggestOnly(why: String) : Decision(why)
        class Blocked(why: String) : Decision(why)
    }

    /**
     * @param enabledCapabilities capabilities the user has switched ON in the
     *        Permission Center (independent of Android runtime permissions).
     */
    fun decide(
        intent: IntentSpec,
        autonomy: AutonomyLevel,
        enabledCapabilities: Set<String>,
    ): Decision {
        val missing = intent.requiredPermissions.filterNot { it in enabledCapabilities }
        if (missing.isNotEmpty()) {
            return Decision.Blocked(
                "Blocked: required data source(s) disabled: ${missing.joinToString()}. " +
                    "Enable them in the Permission Center."
            )
        }
        val sensitive = intent.actions.any { ActionCatalog.isSensitive(it.type) }
        return when {
            autonomy == AutonomyLevel.SUGGEST ->
                Decision.SuggestOnly("Autonomy is Suggest — EchoOS only recommends.")
            sensitive ->
                Decision.AskConfirmation("Sensitive action requires your confirmation.")
            autonomy == AutonomyLevel.CONFIRM || intent.requiresConfirmation ->
                Decision.AskConfirmation("Autonomy is Confirm — approve to run.")
            else ->
                Decision.Execute("Pre-approved (Automatic) — permissions verified.")
        }
    }
}
