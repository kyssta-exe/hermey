package com.kyssta.hermey.networking

import com.google.gson.annotations.SerializedName

// ─── Auth responses ───────────────────────────────────────────────────────────
data class HealthResponse(@SerializedName("status") val status: String? = null)
data class AuthStatusResponse(
    @SerializedName("auth_enabled") val authEnabled: Boolean? = null,
    @SerializedName("password_auth_enabled") val passwordAuthEnabled: Boolean? = null,
    @SerializedName("passkey_auth_enabled") val passkeyAuthEnabled: Boolean? = null,
)
data class LoginResponse(@SerializedName("ok") val ok: Boolean? = null)

// ─── Session models ───────────────────────────────────────────────────────────
data class SessionSummary(
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("created_at") val createdAt: Double? = null,
    @SerializedName("updated_at") val updatedAt: Double? = null,
    @SerializedName("pinned") val pinned: Boolean? = null,
    @SerializedName("archived") val archived: Boolean? = null,
    @SerializedName("profile") val profile: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("message_count") val messageCount: Int? = null,
    @SerializedName("workspace") val workspace: String? = null,
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("parent_session_id") val parentSessionId: String? = null,
)

data class SessionsResponse(
    @SerializedName("sessions") val sessions: List<SessionSummary>? = null,
    @SerializedName("cli_count") val cliCount: Int? = null,
    @SerializedName("archived_count") val archivedCount: Int? = null,
    @SerializedName("server_time") val serverTime: Double? = null,
    @SerializedName("server_tz") val serverTz: String? = null,
)

data class SessionDetail(
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("created_at") val createdAt: Double? = null,
    @SerializedName("updated_at") val updatedAt: Double? = null,
    @SerializedName("pinned") val pinned: Boolean? = null,
    @SerializedName("archived") val archived: Boolean? = null,
    @SerializedName("profile") val profile: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("model_provider") val modelProvider: String? = null,
    @SerializedName("reasoning_effort") val reasoningEffort: String? = null,
    @SerializedName("workspace") val workspace: String? = null,
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("messages") val messages: List<ChatMessage>? = null,
    @SerializedName("message_count") val messageCount: Int? = null,
    @SerializedName("parent_session_id") val parentSessionId: String? = null,
    @SerializedName("streaming") val streaming: Boolean? = null,
    @SerializedName("background") val background: Boolean? = null,
) {
    fun summary() = SessionSummary(
        sessionId = sessionId,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pinned = pinned,
        archived = archived,
        profile = profile,
        model = model,
        messageCount = messages?.size ?: messageCount,
        workspace = workspace,
        projectId = projectId,
        parentSessionId = parentSessionId,
    )
}

data class SessionMutationResponse(
    @SerializedName("ok") val ok: Boolean? = null,
    @SerializedName("session") val session: SessionSummary? = null,
    @SerializedName("error") val error: String? = null,
)

data class SessionBranchResponse(
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("parent_session_id") val parentSessionId: String? = null,
    @SerializedName("error") val error: String? = null,
)

data class SessionCompressionSummary(
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("focus_topic") val focusTopic: String? = null,
    @SerializedName("messages_compressed") val messagesCompressed: Int? = null,
)
data class SessionCompressResponse(
    @SerializedName("ok") val ok: Boolean? = null,
    @SerializedName("session") val session: SessionDetail? = null,
    @SerializedName("summary") val summary: SessionCompressionSummary? = null,
    @SerializedName("focus_topic") val focusTopic: String? = null,
    @SerializedName("error") val error: String? = null,
)

// ─── Chat / Streaming ─────────────────────────────────────────────────────────
data class ChatStartRequest(
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("message") val message: String,
    @SerializedName("model") val model: String? = null,
    @SerializedName("model_provider") val modelProvider: String? = null,
    @SerializedName("reasoning_effort") val reasoningEffort: String? = null,
    @SerializedName("workspace") val workspace: String? = null,
    @SerializedName("profile") val profile: String? = null,
    @SerializedName("attachments") val attachments: List<ChatAttachment>? = null,
)

data class ChatAttachment(
    @SerializedName("name") val name: String? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("mime") val mime: String? = null,
    @SerializedName("size") val size: Long? = null,
)

data class ChatSteerRequest(
    @SerializedName("stream_id") val streamId: String,
    @SerializedName("message") val message: String,
)

data class StartResponse(
    @SerializedName("stream_id") val streamId: String? = null,
    @SerializedName("session") val session: SessionDetail? = null,
    @SerializedName("error") val error: String? = null,
)

// ─── Session list query ───────────────────────────────────────────────────────
data class SessionsQuery(
    @SerializedName("include_archived") val includeArchived: Boolean = false,
    @SerializedName("archived_limit") val archivedLimit: Int? = null,
)

// ─── Projects ─────────────────────────────────────────────────────────────────
data class ProjectSummary(
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("created_at") val createdAt: Double? = null,
)
data class ProjectsResponse(@SerializedName("projects") val projects: List<ProjectSummary>? = null)
data class ProjectMutationResponse(
    @SerializedName("ok") val ok: Boolean? = null,
    @SerializedName("project") val project: ProjectSummary? = null,
    @SerializedName("error") val error: String? = null,
)

// ─── Models & Providers ───────────────────────────────────────────────────────
data class ModelInfo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("provider") val provider: String? = null,
    @SerializedName("context_window") val contextWindow: Int? = null,
    @SerializedName("is_favorited") val isFavorited: Boolean? = null,
)
data class ModelsResponse(@SerializedName("models") val models: List<ModelInfo>? = null)

data class ProviderInfo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("models") val models: List<ModelInfo>? = null,
)
data class ProvidersResponse(@SerializedName("providers") val providers: List<ProviderInfo>? = null)

data class DefaultModelResponse(
    @SerializedName("model") val model: String? = null,
    @SerializedName("provider") val provider: String? = null,
    @SerializedName("reasoning_effort") val reasoningEffort: String? = null,
)

// ─── Profiles ─────────────────────────────────────────────────────────────────
data class ProfileInfo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("is_default") val isDefault: Boolean? = null,
)
data class ProfilesResponse(@SerializedName("profiles") val profiles: List<ProfileInfo>? = null)

// ─── Workspaces ───────────────────────────────────────────────────────────────
data class WorkspaceInfo(
    @SerializedName("name") val name: String? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("description") val description: String? = null,
)
data class WorkspacesResponse(@SerializedName("workspaces") val workspaces: List<WorkspaceInfo>? = null)

// ─── File / Media ─────────────────────────────────────────────────────────────
data class FileEntry(
    @SerializedName("name") val name: String? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("type") val type: String? = null, // "dir" | "file"
    @SerializedName("size") val size: Long? = null,
    @SerializedName("modified") val modified: Double? = null,
)
data class DirectoryResponse(@SerializedName("entries") val entries: List<FileEntry>? = null)

// ─── Git ──────────────────────────────────────────────────────────────────────
data class GitInfo(
    @SerializedName("branch") val branch: String? = null,
    @SerializedName("repo") val repo: String? = null,
    @SerializedName("remote") val remote: String? = null,
    @SerializedName("has_changes") val hasChanges: Boolean? = null,
)
data class GitStatusResponse(@SerializedName("info") val info: GitInfo? = null)
data class GitBranchResponse(@SerializedName("branches") val branches: List<String>? = null)

// ─── Crons (Tasks) ────────────────────────────────────────────────────────────
data class CronJob(
    @SerializedName("job_id") val jobId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("prompt") val prompt: String? = null,
    @SerializedName("schedule") val schedule: String? = null,
    @SerializedName("next_run") val nextRun: String? = null,
    @SerializedName("last_run") val lastRun: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("enabled") val enabled: Boolean? = null,
    @SerializedName("output") val output: String? = null,
    @SerializedName("deliver_to") val deliverTo: String? = null,
)
data class CronsResponse(@SerializedName("jobs") val jobs: List<CronJob>? = null)

// ─── Skills ───────────────────────────────────────────────────────────────────
data class SkillInfo(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("enabled") val enabled: Boolean? = null,
    @SerializedName("category") val category: String? = null,
)
data class SkillsResponse(@SerializedName("skills") val skills: List<SkillInfo>? = null)

// ─── Memory ───────────────────────────────────────────────────────────────────
data class MemoryEntry(
    @SerializedName("fact_id") val factId: Int? = null,
    @SerializedName("entity") val entity: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("tags") val tags: String? = null,
    @SerializedName("trust") val trust: Double? = null,
    @SerializedName("created_at") val createdAt: Double? = null,
)
data class MemoryResponse(@SerializedName("facts") val facts: List<MemoryEntry>? = null)

// ─── Insights ─────────────────────────────────────────────────────────────────
data class InsightMetric(
    @SerializedName("label") val label: String? = null,
    @SerializedName("value") val value: Any? = null,
    @SerializedName("trend") val trend: String? = null,
)
data class InsightsResponse(
    @SerializedName("days") val days: Int? = null,
    @SerializedName("total_sessions") val totalSessions: Int? = null,
    @SerializedName("total_messages") val totalMessages: Int? = null,
    @SerializedName("total_turns") val totalTurns: Int? = null,
    @SerializedName("avg_tokens_per_turn") val avgTokensPerTurn: Double? = null,
    @SerializedName("metrics") val metrics: List<InsightMetric>? = null,
)

// ─── Kanban ───────────────────────────────────────────────────────────────────
data class KanbanBoard(
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("active") val active: Boolean? = null,
    @SerializedName("columns") val columns: List<String>? = null,
    @SerializedName("created_at") val createdAt: Double? = null,
)
data class KanbanCard(
    @SerializedName("card_id") val cardId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("column") val column: String? = null,
    @SerializedName("priority") val priority: Int? = null,
    @SerializedName("assignee") val assignee: String? = null,
    @SerializedName("created_at") val createdAt: Double? = null,
    @SerializedName("updated_at") val updatedAt: Double? = null,
    @SerializedName("blocked") val blocked: Boolean? = null,
    @SerializedName("dependencies") val dependencies: List<String>? = null,
    @SerializedName("comments") val comments: List<KanbanComment>? = null,
)
data class KanbanComment(
    @SerializedName("id") val id: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("created_at") val createdAt: Double? = null,
)
data class KanbanConfig(
    @SerializedName("boards") val boards: List<KanbanBoard>? = null,
    @SerializedName("active_board") val activeBoard: String? = null,
)

// ─── Approval / Clarification ─────────────────────────────────────────────────
data class ApprovalPendingResponse(
    @SerializedName("approval_id") val approvalId: String? = null,
    @SerializedName("tool_name") val toolName: String? = null,
    @SerializedName("tool_preview") val toolPreview: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("session_id") val sessionId: String? = null,
)
data class ClarificationPendingResponse(
    @SerializedName("clarification_id") val clarificationId: String? = null,
    @SerializedName("question") val question: String? = null,
    @SerializedName("session_id") val sessionId: String? = null,
)

// ─── Server Account (multi-server) ────────────────────────────────────────────
data class ServerAccount(
    @SerializedName("id") val id: String = "",
    @SerializedName("url_string") val urlString: String = "",
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("initials") val initials: String = "",
    @SerializedName("header_logo_color_hex") val headerLogoColorHex: String = "#1EB2AA",
    @SerializedName("custom_headers_ref") val customHeadersRef: String? = null,
    @SerializedName("created_at") val createdAt: Double = 0.0,
    @SerializedName("updated_at") val updatedAt: Double = 0.0,
)

// ─── Chat message ─────────────────────────────────────────────────────────────
data class ChatMessage(
    @SerializedName("role") val role: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("timestamp") val timestamp: Double? = null,
    @SerializedName("message_id") val messageId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null,
    @SerializedName("tool_use_id") val toolUseId: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerializedName("reasoning") val reasoning: String? = null,
    @SerializedName("turn_tps") val turnTps: Double? = null,
)

data class ToolCall(
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("function") val function: ToolFunction? = null,
)

data class ToolFunction(
    @SerializedName("name") val name: String? = null,
    @SerializedName("arguments") val arguments: String? = null,
)
