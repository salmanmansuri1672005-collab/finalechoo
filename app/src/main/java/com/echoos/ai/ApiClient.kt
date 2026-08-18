package com.echoos.ai

import com.echoos.BuildConfig
import com.echoos.domain.ActionSpec
import com.echoos.domain.IntentSpec
import com.echoos.domain.TriggerSpec
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Backend client (UI↔Backend contract, docs/API_CONTRACTS.md).
 * Owners: Swati (contract) + Rajersh (payloads) + Salman (consumption).
 * Every AI failure degrades gracefully to the on-device FallbackParser —
 * the backend being down must never corrupt local state (SRS §13.1).
 */
object ApiClient {

    // ---------- DTOs (kept in one place; mirrored from backend schemas) ----------
    data class TriggerDto(
        val type: String, val place: String? = null, val time: String? = null,
        val days: List<String>? = null, val match: String? = null,
        val device: String? = null,
    )
    data class ActionDto(
        val type: String, val value: Any? = null, val enabled: Boolean? = null,
        val app_name: String? = null, val playlist: String? = null,
        val destination: String? = null, val to: String? = null,
        val text: String? = null, val title: String? = null,
        val time: String? = null, val level: Int? = null,
    )
    data class IntentDto(
        val trigger: TriggerDto, val actions: List<ActionDto>,
        val required_permissions: List<String> = emptyList(),
        val requires_confirmation: Boolean = true,
        val confidence: Double = 0.5, val summary: String = "",
    )
    data class ParseRequest(val text: String, val user_context: Map<String, Any> = emptyMap())
    data class ParseResponse(val intent: IntentDto, val valid: Boolean,
                             val rejected_actions: List<String>, val engine: String)

    data class SourceItem(val id: String, val source: String,
                          val sender: String?, val text: String)
    data class CommitmentRequest(val items: List<SourceItem>, val today: String?)
    data class CommitmentDto(val item_id: String, val task: String, val person: String?,
                             val deadline: String?, val confidence: Double,
                             val raw_excerpt: String)
    data class ClassifiedDto(val item_id: String, val priority: String)
    data class CommitmentResponse(val commitments: List<CommitmentDto>,
                                  val classified: List<ClassifiedDto>)

    data class EventDto(val type: String, val value: String?, val ts: String)
    data class PatternRequest(val events: List<EventDto>, val min_frequency: Int = 3)
    data class PatternDto(val sequence: List<String>, val frequency: Int,
                          val confidence: Double, val suggestion_summary: String,
                          val suggested_intent: IntentDto?)
    data class PatternResponse(val patterns: List<PatternDto>)

    data class CalendarDto(val title: String, val start: String, val end: String)
    data class TaskDto(val title: String, val est_minutes: Int, val priority: Int)
    data class PlanCommitmentDto(val task: String, val deadline: String?)
    data class PlanRequest(val date: String, val calendar: List<CalendarDto>,
                           val tasks: List<TaskDto>,
                           val commitments: List<PlanCommitmentDto>,
                           val day_start: String = "08:00", val day_end: String = "22:00")
    data class PlanBlockDto(val start: String, val end: String, val title: String,
                            val kind: String, val source: String, val editable: Boolean)
    data class PlanResponse(val date: String, val blocks: List<PlanBlockDto>,
                            val notes: List<String>)
    data class HealthResponse(val status: String, val version: String,
                              val llm_mode: String)

    interface EchoApi {
        @GET("health") suspend fun health(): HealthResponse
        @POST("ai/parse") suspend fun parse(@Body req: ParseRequest): ParseResponse
        @POST("ai/commitment") suspend fun commitments(@Body req: CommitmentRequest): CommitmentResponse
        @POST("ai/pattern") suspend fun patterns(@Body req: PatternRequest): PatternResponse
        @POST("ai/plan") suspend fun plan(@Body req: PlanRequest): PlanResponse
    }

    val api: EchoApi by lazy {
        val http = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL.trimEnd('/') + "/")
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EchoApi::class.java)
    }
}

/** Top-level extension (a member extension inside an object cannot be imported). */
fun ApiClient.IntentDto.toSpec(): IntentSpec = IntentSpec(
    trigger = TriggerSpec(trigger.type, trigger.place, trigger.time,
        trigger.device, trigger.match),
    actions = actions.map { a ->
        val params = buildMap {
            a.value?.let { put("value", it.toString()) }
            a.enabled?.let { put("enabled", it.toString()) }
            a.app_name?.let { put("app_name", it) }
            a.playlist?.let { put("playlist", it) }
            a.destination?.let { put("destination", it) }
            a.to?.let { put("to", it) }
            a.text?.let { put("text", it) }
            a.title?.let { put("title", it) }
            a.time?.let { put("time", it) }
            a.level?.let { put("level", it.toString()) }
        }
        ActionSpec(a.type, params)
    },
    requiredPermissions = required_permissions,
    requiresConfirmation = requires_confirmation,
    confidence = confidence,
    summary = summary,
)
