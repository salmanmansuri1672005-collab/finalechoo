package com.echoos.data.repo

import com.echoos.data.EchoDatabase
import com.echoos.data.entity.AutomationEntity
import com.echoos.data.entity.CommitmentEntity
import com.echoos.data.entity.ContextEventEntity
import com.echoos.data.entity.ExecutionEntity
import com.echoos.data.entity.PatternEntity
import com.echoos.data.entity.PermissionEntity
import com.echoos.data.entity.UserEntity
import com.echoos.domain.ActionSpec
import com.echoos.domain.IntentSpec
import com.echoos.domain.TriggerSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Repository layer over Room. Owner: Tushar. JSON (de)serialization kept here
 *  so entities stay plain and the domain stays typed. */
class EchoRepository(private val db: EchoDatabase) {

    // ---------- Automations ----------

    fun observeAutomations(): Flow<List<AutomationEntity>> =
        db.automationDao().observeAll()

    suspend fun saveAutomation(name: String, intent: IntentSpec,
                               autonomy: String, status: String): Long {
        val entity = AutomationEntity(
            name = name,
            triggerJson = triggerToJson(intent.trigger).toString(),
            actionsJson = actionsToJson(intent.actions).toString(),
            requiredPermissionsJson = JSONArray(intent.requiredPermissions).toString(),
            autonomy = autonomy,
            status = status,
            summary = intent.summary,
            confidence = intent.confidence,
        )
        return db.automationDao().insert(entity)
    }

    suspend fun activeAutomations(): List<Pair<AutomationEntity, IntentSpec>> =
        db.automationDao().active().map { it to entityToIntent(it) }

    suspend fun setAutomationStatus(id: Long, status: String) =
        db.automationDao().setStatus(id, status)

    suspend fun deleteAutomation(id: Long) = db.automationDao().delete(id)

    fun entityToIntent(e: AutomationEntity): IntentSpec {
        val t = JSONObject(e.triggerJson)
        val trigger = TriggerSpec(
            type = t.optString("type", "manual"),
            place = t.optString("place").ifEmpty { null },
            time = t.optString("time").ifEmpty { null },
            device = t.optString("device").ifEmpty { null },
            match = t.optString("match").ifEmpty { null },
        )
        val actions = mutableListOf<ActionSpec>()
        val arr = JSONArray(e.actionsJson)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val params = mutableMapOf<String, String>()
            o.keys().forEach { k -> if (k != "type" && !o.isNull(k)) params[k] = o.get(k).toString() }
            actions.add(ActionSpec(type = o.getString("type"), params = params))
        }
        val perms = mutableListOf<String>()
        val pArr = JSONArray(e.requiredPermissionsJson)
        for (i in 0 until pArr.length()) perms.add(pArr.getString(i))
        return IntentSpec(trigger, actions, perms,
            requiresConfirmation = e.autonomy != "automatic",
            confidence = e.confidence, summary = e.summary)
    }

    private fun triggerToJson(t: TriggerSpec) = JSONObject().apply {
        put("type", t.type)
        t.place?.let { put("place", it) }
        t.time?.let { put("time", it) }
        t.device?.let { put("device", it) }
        t.match?.let { put("match", it) }
    }

    private fun actionsToJson(actions: List<ActionSpec>) = JSONArray().apply {
        actions.forEach { a ->
            put(JSONObject().apply {
                put("type", a.type)
                a.params.forEach { (k, v) -> put(k, v) }
            })
        }
    }

    // ---------- Context events ----------

    fun observeContextEvents(): Flow<List<ContextEventEntity>> =
        db.contextEventDao().observeRecent(100)

    suspend fun recordContextEvent(type: String, value: String?, source: String,
                                   simulated: Boolean = false) =
        db.contextEventDao().insert(ContextEventEntity(
            timestamp = System.currentTimeMillis(), type = type, value = value,
            source = source, simulated = simulated))

    suspend fun allContextEvents(): List<ContextEventEntity> = db.contextEventDao().all()

    // ---------- Commitments ----------

    fun observeCommitments(): Flow<List<CommitmentEntity>> =
        db.commitmentDao().observeAll()

    suspend fun addCommitment(c: CommitmentEntity) = db.commitmentDao().insert(c)
    suspend fun setCommitmentStatus(id: Long, status: String) =
        db.commitmentDao().setStatus(id, status)
    suspend fun acceptedCommitments() = db.commitmentDao().accepted()

    // ---------- Patterns ----------

    fun observePatterns(): Flow<List<PatternEntity>> = db.patternDao().observeAll()
    suspend fun addPattern(p: PatternEntity) = db.patternDao().insert(p)
    suspend fun setPatternStatus(id: Long, status: String) =
        db.patternDao().setStatus(id, status)

    // ---------- Executions (activity history, FR-11) ----------

    fun observeExecutions(): Flow<List<ExecutionEntity>> =
        db.executionDao().observeRecent(200)

    suspend fun log(automationId: Long?, outcome: String, explanation: String,
                    simulated: Boolean = false) =
        db.executionDao().insert(ExecutionEntity(
            automationId = automationId, outcome = outcome,
            explanation = explanation, simulated = simulated))

    // ---------- Permission center ----------

    fun observePermissions(): Flow<List<PermissionEntity>> =
        db.permissionDao().observeAll()

    suspend fun setCapability(capability: String, enabled: Boolean) =
        db.permissionDao().upsert(PermissionEntity(capability, enabled))

    suspend fun enabledCapabilities(): Set<String> =
        db.permissionDao().observeAll().first()
            .filter { it.enabled }.map { it.capability }.toSet()

    fun observeEnabledCapabilities(): Flow<Set<String>> =
        db.permissionDao().observeAll()
            .map { list -> list.filter { it.enabled }.map { it.capability }.toSet() }

    // ---------- User profile (local only) ----------

    fun observeUser(): Flow<UserEntity?> = db.userDao().observe()

    suspend fun signIn(name: String, email: String, guest: Boolean) {
        db.userDao().upsert(UserEntity(name = name, email = email, isGuest = guest))
    }

    suspend fun updateProfile(name: String, email: String) {
        val current = db.userDao().get() ?: UserEntity()
        db.userDao().upsert(current.copy(name = name, email = email,
            isGuest = if (email.isNotBlank()) false else current.isGuest))
    }

    suspend fun setDefaultAutonomy(level: String) {
        val current = db.userDao().get() ?: UserEntity()
        db.userDao().upsert(current.copy(defaultAutonomy = level))
    }

    /** Signs out. Automations and history stay on the device by design. */
    suspend fun signOut() = db.userDao().clear()

    // ---------- Privacy: deletion controls (SRS §12.1, §15) ----------

    suspend fun deleteAllCommitments() = db.commitmentDao().clear()
    suspend fun deleteAllPatterns() = db.patternDao().clear()
    suspend fun deleteAllHistory() {
        db.executionDao().clear()
        db.contextEventDao().clear()
    }

    /** Deletes every trace of the user from this device. */
    suspend fun wipeAll() {
        db.commitmentDao().clear()
        db.patternDao().clear()
        db.executionDao().clear()
        db.contextEventDao().clear()
        db.userDao().clear()
    }
}
