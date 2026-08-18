package com.echoos.engine

import com.echoos.data.repo.EchoRepository

/**
 * Demo mode / test-event controls (SRS §5, §23, §26). Owner: Subh (+ Salman UI).
 * Fires clearly-labeled SIMULATED context events so the three-minute demo works
 * reliably regardless of Android background restrictions.
 */
class DemoContextSimulator(
    private val engine: AutomationEngine,
    private val repo: EchoRepository,
) {
    suspend fun enterCollege() =
        engine.onContextEvent("location_enter", "college", "simulated", simulated = true)

    suspend fun leaveCollege() =
        engine.onContextEvent("location_exit", "college", "simulated", simulated = true)

    suspend fun carConnected() =
        engine.onContextEvent("bluetooth_connect", "Car Stereo", "simulated", simulated = true)

    suspend fun fireTime(hhmm: String) =
        engine.onContextEvent("time_schedule", hhmm, "simulated", simulated = true)

    /** Seeds the repeated driving routine so pattern detection has data. */
    suspend fun seedDrivingPattern(times: Int = 4) {
        repeat(times) {
            repo.recordContextEvent("bluetooth_connect", "car", "simulated", true)
            repo.recordContextEvent("open_app", "maps", "simulated", true)
            repo.recordContextEvent("play_music", "drive mix", "simulated", true)
        }
    }
}
