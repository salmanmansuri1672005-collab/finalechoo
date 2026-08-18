package com.echoos

import com.echoos.domain.ActionSpec
import com.echoos.domain.AutonomyEngine
import com.echoos.domain.AutonomyLevel
import com.echoos.domain.IntentSpec
import com.echoos.domain.TriggerSpec
import org.junit.Assert.assertTrue
import org.junit.Test

/** Required cases 3 & 10: autonomy gating + disabled data source blocks execution. */
class AutonomyEngineTest {

    private val focusAtCollege = IntentSpec(
        trigger = TriggerSpec("location_enter", place = "college"),
        actions = listOf(ActionSpec("focus_mode")),
        requiredPermissions = listOf("dnd", "location"),
        requiresConfirmation = false, confidence = 0.9, summary = "test")

    @Test fun `disabled source blocks execution`() {
        val d = AutonomyEngine.decide(focusAtCollege, AutonomyLevel.AUTOMATIC,
            enabledCapabilities = setOf("dnd")) // location disabled
        assertTrue(d is AutonomyEngine.Decision.Blocked)
        assertTrue(d.explanation.contains("location"))
    }

    @Test fun `automatic executes when all sources enabled`() {
        val d = AutonomyEngine.decide(focusAtCollege, AutonomyLevel.AUTOMATIC,
            setOf("dnd", "location"))
        assertTrue(d is AutonomyEngine.Decision.Execute)
    }

    @Test fun `confirm level asks first`() {
        val d = AutonomyEngine.decide(focusAtCollege, AutonomyLevel.CONFIRM,
            setOf("dnd", "location"))
        assertTrue(d is AutonomyEngine.Decision.AskConfirmation)
    }

    @Test fun `suggest level never executes`() {
        val d = AutonomyEngine.decide(focusAtCollege, AutonomyLevel.SUGGEST,
            setOf("dnd", "location"))
        assertTrue(d is AutonomyEngine.Decision.SuggestOnly)
    }

    @Test fun `sensitive action always confirmed even on automatic`() {
        val sensitive = focusAtCollege.copy(
            actions = listOf(ActionSpec("send_message", mapOf("to" to "Rohit"))),
            requiredPermissions = listOf("messaging", "location"))
        val d = AutonomyEngine.decide(sensitive, AutonomyLevel.AUTOMATIC,
            setOf("messaging", "location"))
        assertTrue(d is AutonomyEngine.Decision.AskConfirmation)
    }
}
