package com.echoos.domain

/**
 * On-device validation of AI-produced intent — second line of defense after the
 * backend validator (SRS §10, FR-03). Owner: Rajersh, security review: Tushar.
 */
object IntentValidator {

    data class Result(
        val intent: IntentSpec?,
        val rejectedActions: List<String>,
        val reason: String? = null,
    ) {
        val valid get() = intent != null
    }

    private const val MAX_ACTIONS = 5

    fun validate(raw: IntentSpec): Result {
        if (raw.trigger.type !in ActionCatalog.supportedTriggers) {
            return Result(null, emptyList(), "Unsupported trigger '${raw.trigger.type}'")
        }
        val rejected = raw.actions.filter { it.type !in ActionCatalog.supported }
            .map { it.type }
        val kept = raw.actions.filter { it.type in ActionCatalog.supported }
            .take(MAX_ACTIONS)
        if (kept.isEmpty()) {
            return Result(null, rejected, "No supported actions remain")
        }

        val mustConfirm = raw.requiresConfirmation ||
            kept.any { ActionCatalog.isSensitive(it.type) } ||
            raw.confidence < 0.4

        // Recompute permissions from the catalog — never trust the model's list.
        val perms = buildSet {
            when (raw.trigger.type) {
                "location_enter", "location_exit" -> add("location")
                "calendar_event" -> add("calendar")
                "bluetooth_connect", "bluetooth_disconnect" -> add("connectivity")
            }
            kept.forEach { a -> ActionCatalog.capabilityFor(a.type)?.let { add(it) } }
        }.sorted()

        return Result(
            intent = raw.copy(
                actions = kept,
                requiresConfirmation = mustConfirm,
                requiredPermissions = perms,
                confidence = raw.confidence.coerceIn(0.0, 1.0),
            ),
            rejectedActions = rejected,
        )
    }
}
