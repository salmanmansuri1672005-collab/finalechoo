package com.echoos.ai

import com.echoos.domain.ActionSpec
import com.echoos.domain.IntentSpec
import com.echoos.domain.TriggerSpec

/**
 * On-device offline parser (subset of the backend rule engine). Owner: Rajersh.
 * Used when the backend is unreachable so core automation creation keeps
 * working offline (SRS §13.1). Output still passes IntentValidator.
 */
object FallbackParser {

    private val places = listOf("college", "work", "home", "gym")

    fun parse(text: String): IntentSpec {
        val low = text.lowercase()
        val place = places.firstOrNull { low.contains(it) }

        val trigger = when {
            place != null && Regex("\\b(leave|leaving|exit)\\b").containsMatchIn(low) ->
                TriggerSpec("location_exit", place = place)
            place != null && Regex("\\b(reach|arrive|get to|enter|at)\\b").containsMatchIn(low) ->
                TriggerSpec("location_enter", place = place)
            Regex("\\b(car|driving|drive)\\b").containsMatchIn(low) ->
                TriggerSpec("bluetooth_connect", device = "car")
            Regex("\\b(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)\\b").containsMatchIn(low) -> {
                val m = Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b").find(low)!!
                var h = m.groupValues[1].toInt() % 12
                if (m.groupValues[3] == "pm") h += 12
                TriggerSpec("time_schedule",
                    time = "%02d:%02d".format(h, m.groupValues[2].ifEmpty { "0" }.toInt()))
            }
            place != null -> TriggerSpec("location_enter", place = place)
            else -> TriggerSpec("manual")
        }

        val actions = buildList {
            if (Regex("focus|study|routine").containsMatchIn(low))
                add(ActionSpec("focus_mode", mapOf("value" to (place ?: "focus"))))
            if (Regex("silent|mute").containsMatchIn(low))
                add(ActionSpec("silent_mode", mapOf("enabled" to "true")))
            if (Regex("do not disturb|dnd").containsMatchIn(low)) add(ActionSpec("dnd_on"))
            if (Regex("music|playlist|song").containsMatchIn(low)) add(ActionSpec("play_music"))
            if (Regex("navigat|directions|maps").containsMatchIn(low))
                add(ActionSpec("navigation_start", mapOf("destination" to (place ?: "home"))))
            if (Regex("remind").containsMatchIn(low))
                add(ActionSpec("create_reminder", mapOf("title" to text.take(60))))
            if (Regex("calendar|schedule|agenda").containsMatchIn(low))
                add(ActionSpec("calendar_summary"))
        }

        val summary = "Offline draft: ${trigger.type.replace('_', ' ')}" +
            (trigger.place?.let { " at $it" } ?: "") +
            " → " + (actions.joinToString { it.type.replace('_', ' ') }
                .ifEmpty { "no actions recognized" })

        return IntentSpec(
            trigger = trigger, actions = actions,
            requiredPermissions = emptyList(),          // recomputed by validator
            requiresConfirmation = true,
            confidence = if (actions.isNotEmpty()) 0.6 else 0.2,
            summary = summary,
        )
    }
}
