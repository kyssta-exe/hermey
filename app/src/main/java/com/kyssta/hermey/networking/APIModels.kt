package com.kyssta.hermey.networking

import com.google.gson.annotations.SerializedName

// ─── Auth ─────────────────────────────────────────────────────────────────────
data class HealthResponse(@SerializedName("status") val status: String? = null)
data class AuthStatusResponse(
    @SerializedName("auth_enabled") val authEnabled: Boolean? = null,
    @SerializedName("password_auth_enabled") val passwordAuthEnabled: Boolean? = null,
    @SerializedName("passkey_auth_enabled") val passkeyAuthEnabled: Boolean? = null,
)

/** POST /auth/password-login → {"ok": true, "next": "/"} */
data class LoginResponse(
    @SerializedName("ok") val ok: Boolean? = null,
    @SerializedName("next") val next: String? = null,
    @SerializedName("detail") val detail: String? = null,
)

/** POST /api/auth/ws-ticket → {"ticket": "...", "ttl_seconds": 30} */
data class WsTicketResponse(
    @SerializedName("ticket") val ticket: String? = null,
    @SerializedName("ttl_seconds") val ttlSeconds: Int? = null,
)

data class AuthProvider(
    @SerializedName("name") val name: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("supports_password") val supportsPassword: Boolean? = null,
)
data class AuthProvidersResponse(
    @SerializedName("providers") val providers: List<AuthProvider> = emptyList(),
)
data class AuthMeResponse(
    @SerializedName("authenticated") val authenticated: Boolean? = null,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("provider") val provider: String? = null,
)

// ─── Sessions (GET /api/sessions rows) ────────────────────────────────────────
data class SessionSummary(
    @SerializedName("id") val id: String? = null,
    @SerializedName("resolved_id") val resolvedId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("preview") val preview: String? = null,
    @SerializedName("started_at") val startedAt: Double? = null,
    @SerializedName("last_active") val lastActive: Double? = null,
    @SerializedName("message_count") val messageCount: Int? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("archived") val archived: Boolean? = null,
    @SerializedName("pinned") val pinned: Boolean? = null,
)

data class SessionsResponse(
    @SerializedName("sessions") val sessions: List<SessionSummary> = emptyList(),
    @SerializedName("total") val total: Int? = null,
    @SerializedName("limit") val limit: Int? = null,
    @SerializedName("offset") val offset: Int? = null,
)

/** GET /api/sessions/{id}/messages */
data class MessagesResponse(
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("messages") val messages: List<ChatMessage> = emptyList(),
    @SerializedName("pagination") val pagination: Map<String, Any?>? = null,
)

data class SessionMutationResponse(
    @SerializedName("ok") val ok: Boolean? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("archived") val archived: Boolean? = null,
    @SerializedName("pinned") val pinned: Boolean? = null,
    @SerializedName("detail") val detail: String? = null,
)

// ─── Crons (GET /api/cron/jobs) ───────────────────────────────────────────────
data class CronJob(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("prompt") val prompt: String? = null,
    /** Server sends {"kind": "cron", "expr": "...", "display": "..."} or a string. */
    @SerializedName("schedule") val schedule: Any? = null,
    @SerializedName("schedule_display") val scheduleDisplay: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("enabled") val enabled: Boolean? = null,
    @SerializedName("paused_at") val pausedAt: Double? = null,
    @SerializedName("next_run_at") val nextRunAt: Double? = null,
    @SerializedName("last_run_at") val lastRunAt: Double? = null,
    @SerializedName("last_status") val lastStatus: String? = null,
) {
    fun isPaused(): Boolean = pausedAt != null || enabled == false
    fun scheduleLabel(): String = when (val s = schedule) {
        is Map<*, *> -> (s["display"] ?: s["expr"])?.toString() ?: "manual"
        is String -> s
        else -> scheduleDisplay ?: "manual"
    }
}

// ─── Skills (GET /api/skills — a bare JSON array) ─────────────────────────────
data class SkillInfo(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("enabled") val enabled: Boolean? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("usage") val usage: Int? = null,
)

// ─── Memory (GET /api/memory — provider status, not facts) ────────────────────
data class MemoryProviderStatus(
    @SerializedName("name") val name: String? = null,
    @SerializedName("active") val active: Boolean? = null,
)
data class MemoryStatus(
    @SerializedName("active") val active: String? = null,
    @SerializedName("builtin_files") val builtinFiles: Map<String, Long>? = null,
    @SerializedName("providers") val providers: List<Map<String, Any?>>? = null,
)

// ─── Multi-server account bookkeeping ─────────────────────────────────────────
data class ServerAccount(
    @SerializedName("id") val id: String = "",
    @SerializedName("url_string") val urlString: String = "",
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("initials") val initials: String = "",
    @SerializedName("header_logo_color_hex") val headerLogoColorHex: String = "#1EB2AA",
    @SerializedName("created_at") val createdAt: Double = 0.0,
    @SerializedName("updated_at") val updatedAt: Double = 0.0,
)

// ─── Chat messages (REST transcript + WS stream share these fields) ───────────
data class ChatMessage(
    @SerializedName("role") val role: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("timestamp") val timestamp: Double? = null,
    @SerializedName("id") val messageId: Long? = null,
    @SerializedName("tool_name") val toolName: String? = null,
)

// ─── Custom headers (kept for Settings compatibility) ────────────────────────
// CustomHeader lives in Endpoints.kt
