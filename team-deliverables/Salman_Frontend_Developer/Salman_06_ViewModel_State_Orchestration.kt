package com.echoos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echoos.EchoApp
import com.echoos.ai.ApiClient
import com.echoos.ai.toSpec
import com.echoos.ai.FallbackParser
import com.echoos.data.entity.CommitmentEntity
import com.echoos.data.entity.PatternEntity
import com.echoos.domain.IntentSpec
import com.echoos.domain.IntentValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Single ViewModel for the MVP (Salman). Exposes flows the screens collect and
 *  orchestrates backend calls with offline fallback. */
class EchoViewModel(app: Application) : AndroidViewModel(app) {

    private val echo = app as EchoApp
    private val repo = echo.repository

    val automations = repo.observeAutomations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val commitments = repo.observeCommitments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val patterns = repo.observePatterns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val executions = repo.observeExecutions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val contextEvents = repo.observeContextEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val capabilities = repo.observeEnabledCapabilities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    /** null = signed out → the app shows the login screen. */
    val user = repo.observeUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ---------- UI state ----------
    sealed class ParseState {
        data object Idle : ParseState()
        data object Loading : ParseState()
        data class Preview(val intent: IntentSpec, val rejected: List<String>,
                           val engine: String) : ParseState()
        data class Error(val message: String) : ParseState()
    }
    val parseState = MutableStateFlow<ParseState>(ParseState.Idle)

    data class PlanUi(val blocks: List<ApiClient.PlanBlockDto>, val notes: List<String>)
    val planState = MutableStateFlow<PlanUi?>(null)
    val planLoading = MutableStateFlow(false)
    val backendOnline = MutableStateFlow<Boolean?>(null)

    fun checkBackend() = viewModelScope.launch {
        backendOnline.value = runCatching { ApiClient.api.health().status == "ok" }
            .getOrDefault(false)
    }

    // ---------- Natural-language automation (FR-01..03) ----------

    fun parseText(text: String) = viewModelScope.launch {
        parseState.value = ParseState.Loading
        val (raw, engine) = try {
            val resp = ApiClient.api.parse(ApiClient.ParseRequest(text))
            resp.intent.toSpec() to resp.engine
        } catch (e: Exception) {
            FallbackParser.parse(text) to "on_device_fallback"
        }
        val result = IntentValidator.validate(raw)
        parseState.value = if (result.valid) {
            ParseState.Preview(result.intent!!, result.rejectedActions, engine)
        } else {
            ParseState.Error(result.reason ?: "Could not understand that automation.")
        }
    }

    fun saveAutomation(name: String, intent: IntentSpec, autonomy: String) =
        viewModelScope.launch {
            val id = repo.saveAutomation(name, intent, autonomy, status = "active")
            repo.log(id, "detected", "Automation '$name' created from natural language. " +
                "Autonomy: $autonomy. ${intent.summary}")
            runCatching { echo.scheduler.sync() }
            parseState.value = ParseState.Idle
        }

    fun setAutomationStatus(id: Long, status: String) = viewModelScope.launch {
        repo.setAutomationStatus(id, status)
    }

    fun confirmPending(id: Long) = viewModelScope.launch { echo.engine.executeConfirmed(id) }
    fun deleteAutomation(id: Long) = viewModelScope.launch { repo.deleteAutomation(id) }

    // ---------- EchoLens (FR-06..08) ----------

    fun runEchoLens(demoItems: List<ApiClient.SourceItem>) = viewModelScope.launch {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val resp = runCatching {
            ApiClient.api.commitments(ApiClient.CommitmentRequest(demoItems, today))
        }.getOrNull() ?: return@launch
        resp.commitments.forEach { c ->
            repo.addCommitment(CommitmentEntity(
                task = c.task, person = c.person, deadline = c.deadline,
                confidence = c.confidence, rawExcerpt = c.raw_excerpt))
        }
        repo.log(null, "detected",
            "EchoLens scanned ${demoItems.size} permitted items and found " +
                "${resp.commitments.size} commitment candidate(s).")
    }

    fun setCommitmentStatus(id: Long, status: String) = viewModelScope.launch {
        repo.setCommitmentStatus(id, status)
    }

    // ---------- Patterns (FR-09) ----------

    fun analyzePatterns() = viewModelScope.launch {
        val events = repo.allContextEvents().map {
            ApiClient.EventDto(it.type, it.value,
                java.time.Instant.ofEpochMilli(it.timestamp).toString().removeSuffix("Z"))
        }
        val resp = runCatching {
            ApiClient.api.patterns(ApiClient.PatternRequest(events))
        }.getOrNull() ?: return@launch
        resp.patterns.forEach { p ->
            repo.addPattern(PatternEntity(
                sequenceJson = JSONArray(p.sequence).toString(),
                frequency = p.frequency, confidence = p.confidence,
                suggestionSummary = p.suggestion_summary,
                suggestedIntentJson = null))
            repo.log(null, "suggested", p.suggestion_summary)
        }
    }

    fun setPatternStatus(id: Long, status: String) = viewModelScope.launch {
        repo.setPatternStatus(id, status)
    }

    // ---------- Planner (FR-10) ----------

    fun planDay(calendar: List<ApiClient.CalendarDto>, tasks: List<ApiClient.TaskDto>) =
        viewModelScope.launch {
            planLoading.value = true
            val commitments = repo.acceptedCommitments().map {
                ApiClient.PlanCommitmentDto(it.task, it.deadline)
            }
            val resp = runCatching {
                ApiClient.api.plan(ApiClient.PlanRequest(
                    date = LocalDate.now().plusDays(1).toString(),
                    calendar = calendar, tasks = tasks, commitments = commitments))
            }.getOrNull()
            planState.value = resp?.let { PlanUi(it.blocks, it.notes) }
            planLoading.value = false
            if (resp != null) repo.log(null, "detected",
                "AI planner generated ${resp.blocks.size} blocks for ${resp.date}.")
        }

    // ---------- Permission center ----------

    fun setCapability(cap: String, enabled: Boolean) = viewModelScope.launch {
        repo.setCapability(cap, enabled)
        repo.log(null, "detected",
            "Data source '$cap' ${if (enabled) "enabled" else "disabled"} by user.")
    }

    // ---------- Account (local profile, SRS §15) ----------

    fun signIn(name: String, email: String, guest: Boolean = false) = viewModelScope.launch {
        repo.signIn(name, email, guest)
        repo.log(null, "detected", "User signed in as $name" +
            (if (guest) " (guest)" else " ($email)") +
            ". Profile stored on device only — nothing is sent anywhere.")
    }

    fun updateProfile(name: String, email: String) = viewModelScope.launch {
        repo.updateProfile(name, email)
        repo.log(null, "detected", "User updated their profile details.")
    }

    fun signOut() = viewModelScope.launch {
        repo.log(null, "detected", "User signed out. Automations and history stay on the device.")
        repo.signOut()
    }

    fun wipeAll() = viewModelScope.launch { repo.wipeAll() }

    // ---------- Privacy deletion controls ----------

    fun deleteAllCommitments() = viewModelScope.launch { repo.deleteAllCommitments() }
    fun deleteAllHistory() = viewModelScope.launch { repo.deleteAllHistory() }

    // ---------- Demo mode (Subh) ----------

    fun demoEnterCollege() = viewModelScope.launch { echo.simulator.enterCollege() }
    fun demoCarConnected() = viewModelScope.launch { echo.simulator.carConnected() }
    fun demoSeedDrivingPattern() = viewModelScope.launch {
        echo.simulator.seedDrivingPattern(); analyzePatterns()
    }
}
