package com.echoos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entities for the seven core data-model tables (SRS §12). Owner: Tushar.
 * Structured intent fields are stored as JSON strings (validated on read/write
 * by the domain layer) to keep the schema simple for the MVP.
 */

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "local",
    /** Local profile — never leaves the device (SRS §15). */
    val name: String = "",
    val email: String = "",
    val isGuest: Boolean = false,
    val preferencesJson: String = "{}",
    val defaultAutonomy: String = "confirm", // suggest | confirm | automatic
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val triggerJson: String,
    val conditionsJson: String = "[]",
    val actionsJson: String,
    val requiredPermissionsJson: String = "[]",
    val autonomy: String = "confirm",
    /** draft | pending_confirmation | active | running | succeeded | failed | disabled */
    val status: String = "draft",
    val summary: String = "",
    val confidence: Double = 0.5,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "context_events")
data class ContextEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,        // location_enter, bluetooth_connect, open_app, ...
    val value: String? = null,
    val source: String,      // geofence | time | bluetooth | notification | simulated
    val simulated: Boolean = false,
)

@Entity(tableName = "commitments")
data class CommitmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val task: String,
    val person: String? = null,
    val deadline: String? = null,     // ISO-8601
    val confidence: Double,
    val source: String = "notification",
    val rawExcerpt: String = "",
    /** candidate | accepted | dismissed | done */
    val status: String = "candidate",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "patterns")
data class PatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sequenceJson: String,
    val frequency: Int,
    val confidence: Double,
    val suggestionSummary: String,
    val suggestedIntentJson: String? = null,
    /** suggested | accepted | dismissed */
    val status: String = "suggested",
)

@Entity(tableName = "executions")
data class ExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val automationId: Long?,
    val timestamp: Long = System.currentTimeMillis(),
    /** detected | suggested | confirmed | executed_success | executed_failure | blocked */
    val outcome: String,
    /** Explainability (SRS §11.4): why did this happen? */
    val explanation: String,
    val simulated: Boolean = false,
)

@Entity(tableName = "permissions")
data class PermissionEntity(
    /** capability: location | notifications | calendar | connectivity | dnd | messaging | settings | camera */
    @PrimaryKey val capability: String,
    val enabled: Boolean = false,
)
