package com.echoos

import android.app.Application
import com.echoos.data.EchoDatabase
import com.echoos.data.repo.EchoRepository
import com.echoos.engine.AutomationEngine
import com.echoos.engine.DemoContextSimulator
import com.echoos.engine.TriggerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EchoApp : Application() {

    val database by lazy { EchoDatabase.get(this) }
    val repository by lazy { EchoRepository(database) }
    val engine by lazy { AutomationEngine(this, repository) }
    val scheduler by lazy { TriggerScheduler(this, repository) }
    val simulator by lazy { DemoContextSimulator(engine, repository) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // Seed capability switches OFF on first launch — the user enables
            // sources progressively in the Permission Center (SRS §15).
            val dao = database.permissionDao()
            listOf("location", "notifications", "calendar", "connectivity",
                "dnd", "messaging", "settings", "camera").forEach { cap ->
                if (dao.isEnabled(cap) == null) {
                    dao.upsert(com.echoos.data.entity.PermissionEntity(cap, false))
                }
            }
        }
    }
}
