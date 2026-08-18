package com.echoos

import com.echoos.domain.ActionSpec
import com.echoos.domain.IntentSpec
import com.echoos.domain.IntentValidator
import com.echoos.domain.TriggerSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Required cases 2 & 3 on-device (Subh). */
class IntentValidatorTest {

    private fun intent(actions: List<ActionSpec>, trigger: TriggerSpec = TriggerSpec("manual"),
                       confirm: Boolean = false, confidence: Double = 0.9) =
        IntentSpec(trigger, actions, emptyList(), confirm, confidence, "test")

    @Test fun `unsupported action stripped, valid kept`() {
        val r = IntentValidator.validate(intent(listOf(
            ActionSpec("focus_mode"), ActionSpec("transfer_money"))))
        assertTrue(r.valid)
        assertEquals(listOf("transfer_money"), r.rejectedActions)
        assertEquals(listOf("focus_mode"), r.intent!!.actions.map { it.type })
    }

    @Test fun `all unsupported rejected entirely`() {
        val r = IntentValidator.validate(intent(listOf(ActionSpec("disable_lock_screen"))))
        assertFalse(r.valid)
    }

    @Test fun `unsupported trigger rejected`() {
        val r = IntentValidator.validate(intent(listOf(ActionSpec("dnd_on")),
            trigger = TriggerSpec("root_shell")))
        assertFalse(r.valid)
    }

    @Test fun `sensitive action forces confirmation`() {
        val r = IntentValidator.validate(intent(listOf(
            ActionSpec("send_message", mapOf("to" to "Rohit")))))
        assertTrue(r.intent!!.requiresConfirmation)
    }

    @Test fun `low confidence forces confirmation`() {
        val r = IntentValidator.validate(intent(listOf(ActionSpec("dnd_on")),
            confidence = 0.2))
        assertTrue(r.intent!!.requiresConfirmation)
    }

    @Test fun `permissions recomputed from catalog`() {
        val r = IntentValidator.validate(intent(listOf(ActionSpec("calendar_summary")),
            trigger = TriggerSpec("location_enter", place = "college")))
        assertEquals(listOf("calendar", "location"), r.intent!!.requiredPermissions)
    }

    @Test fun `max five actions`() {
        val r = IntentValidator.validate(intent(List(9) { ActionSpec("dnd_on") }))
        assertEquals(5, r.intent!!.actions.size)
    }
}
