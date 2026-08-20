package com.kyssta.hermey.networking

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

data class CustomHeader(val name: String, val value: String) {
    fun isApplicable() = name.isNotBlank() && value.isNotBlank()
    fun sanitizedName() = name.trim()
}

enum class SessionExportFormat { text, html, json, pdf }

object Endpoints {
    const val HEALTH = "/health"
    const val AUTH_STATUS = "/api/auth/status"
    const val AUTH_LOGIN = "/api/auth/login"
    const val AUTH_LOGOUT = "/api/auth/logout"
    const val SESSIONS = "/api/sessions"
    const val SESSIONS_SEARCH = "/api/sessions/search"
    const val SESSION = "/api/session"
    const val SESSION_STATUS = "/api/session/status"
    const val SESSION_NEW = "/api/session/new"
    const val SESSION_RENAME = "/api/session/rename"
    const val SESSION_DELETE = "/api/session/delete"
    const val SESSION_PIN = "/api/session/pin"
    const val SESSION_ARCHIVE = "/api/session/archive"
    const val SESSION_BRANCH = "/api/session/branch"
    const val SESSION_COMPRESS = "/api/session/compress"
    const val SESSION_UNDO = "/api/session/undo"
    const val SESSION_RETRY = "/api/session/retry"
    const val SESSION_TRUNCATE = "/api/session/truncate"
    const val SESSION_UPDATE = "/api/session/update"
    const val SESSION_MOVE = "/api/session/move"
    const val SESSION_YOLO = "/api/session/yolo"
    const val SESSION_EXPORT = "/api/session/export"
    const val PROJECTS = "/api/projects"
    const val PROJECT_CREATE = "/api/projects/create"
    const val PROJECT_RENAME = "/api/projects/rename"
    const val PROJECT_DELETE = "/api/projects/delete"
    const val CHAT_START = "/api/chat/start"
    const val CHAT_STREAM = "/api/chat/stream"
    const val CHAT_CANCEL = "/api/chat/cancel"
    const val CHAT_STREAM_STATUS = "/api/chat/stream/status"
    const val CHAT_STEER = "/api/chat/steer"
    const val GOAL = "/api/goal"
    const val APPROVAL_PENDING = "/api/approval/pending"
    const val APPROVAL_STREAM = "/api/approval/stream"
    const val APPROVAL_RESPOND = "/api/approval/respond"
    const val CLARIFY_PENDING = "/api/clarify/pending"
    const val CLARIFY_STREAM = "/api/clarify/stream"
    const val CLARIFY_RESPOND = "/api/clarify/respond"
    const val BTW = "/api/btw"
    const val BACKGROUND = "/api/background"
    const val BACKGROUND_STATUS = "/api/background/status"
    const val WORKSPACES = "/api/workspaces"
    const val WORKSPACE_SUGGEST = "/api/workspaces/suggest"
    const val WORKSPACE_ADD = "/api/workspaces/add"
    const val WORKSPACE_REMOVE = "/api/workspaces/remove"
    const val WORKSPACE_RENAME = "/api/workspaces/rename"
    const val WORKSPACE_REORDER = "/api/workspaces/reorder"
    const val DIRECTORY_LIST = "/api/list"
    const val FILE = "/api/file"
    const val FILE_RAW = "/api/file/raw"
    const val MEDIA = "/api/media"
    const val GIT_INFO = "/api/git-info"
    const val GIT_STATUS = "/api/git/status"
    const val GIT_BRANCHES = "/api/git/branches"
    const val GIT_DIFF = "/api/git/diff"
    const val GIT_FETCH = "/api/git/fetch"
    const val GIT_PULL = "/api/git/pull"
    const val GIT_PUSH = "/api/git/push"
    const val GIT_CHECKOUT = "/api/git/checkout"
    const val GIT_STAGE = "/api/git/stage"
    const val GIT_UNSTAGE = "/api/git/unstage"
    const val GIT_DISCARD = "/api/git/discard"
    const val GIT_COMMIT = "/api/git/commit"
    const val GIT_COMMIT_MESSAGE = "/api/git/commit-message"
    const val MODELS = "/api/models"
    const val MODELS_LIVE = "/api/models/live"
    const val COMMANDS = "/api/commands"
    const val DEFAULT_MODEL = "/api/default-model"
    const val REASONING = "/api/reasoning"
    const val PERSONALITIES = "/api/personalities"
    const val PERSONALITY_SET = "/api/personality/set"
    const val PROFILES = "/api/profiles"
    const val PROFILE_SWITCH = "/api/profile/switch"
    const val PROFILE_CREATE = "/api/profile/create"
    const val PROVIDERS = "/api/providers"
    const val SETTINGS = "/api/settings"
    const val UPDATES_CHECK = "/api/updates/check"
    const val UPDATES_APPLY = "/api/updates/apply"
    const val INSIGHTS = "/api/insights"
    const val CRONS = "/api/crons"
    const val CRON_CREATE = "/api/crons/create"
    const val CRON_UPDATE = "/api/crons/update"
    const val CRON_DELETE = "/api/crons/delete"
    const val CRON_RUN = "/api/crons/run"
    const val CRON_PAUSE = "/api/crons/pause"
    const val CRON_RESUME = "/api/crons/resume"
    const val CRON_STATUS = "/api/crons/status"
    const val CRON_OUTPUT = "/api/crons/output"
    const val CRON_DELIVERY_OPTIONS = "/api/crons/delivery-options"
    const val KANBAN_CONFIG = "/api/kanban/config"
    const val KANBAN_BOARDS = "/api/kanban/boards"
    const val KANBAN_DISPATCH = "/api/kanban/dispatch"
    const val KANBAN_EVENTS = "/api/kanban/events"
    const val KANBAN_TASKS = "/api/kanban/tasks"
    const val KANBAN_LINKS = "/api/kanban/links"
    const val MEMORY = "/api/memory"
    const val MEMORY_WRITE = "/api/memory/write"
    const val SKILLS = "/api/skills"
    const val SKILLS_CONTENT = "/api/skills/content"
    const val SKILLS_TOGGLE = "/api/skills/toggle"
    const val UPLOAD = "/api/upload"
    const val TRANSCRIBE = "/api/transcribe"
    const val TTS = "/api/tts"
}

fun buildUrl(baseUrl: String, path: String, query: Map<String, String?>): String {
    val base = baseUrl.removeSuffix("/")
    val cleanPath = path.removePrefix("/")
    val params = query.filter { (_, v) -> v != null }
        .map { (k, v) -> "$k=${java.net.URLEncoder.encode(v!!, "UTF-8")}" }
        .joinToString("&")
    return if (params.isNotBlank()) "$base/$cleanPath?$params" else "$base/$cleanPath"
}

fun sessionQuery(sessionId: String, includeMessages: Boolean, msgLimit: Int? = null, msgBefore: String? = null, expandRenderable: Boolean = false): Map<String, String?> {
    return buildMap {
        put("session_id", sessionId)
        put("messages", if (includeMessages) "1" else "0")
        msgLimit?.let { put("msg_limit", it.toString()) }
        msgBefore?.let { put("msg_before", it) }
        if (expandRenderable) put("expand_renderable", "1")
    }
}

fun streamQuery(streamId: String) = mapOf("stream_id" to streamId)
fun backgroundStatusQuery(sessionId: String) = mapOf("session_id" to sessionId)
fun directoryQuery(sessionId: String, path: String? = null) = buildMap {
    put("session_id", sessionId)
    path?.let { put("path", it) }
}
fun fileQuery(sessionId: String, path: String) = mapOf("session_id" to sessionId, "path" to path)
fun workspaceSuggestQuery(prefix: String) = mapOf("prefix" to prefix)
fun cronStatusQuery(jobId: String?) = jobId?.let { mapOf("job_id" to it) } ?: emptyMap()
fun cronOutputQuery(jobId: String, limit: Int? = null) = buildMap {
    put("job_id", jobId)
    limit?.let { put("limit", it.toString()) }
}
fun insightsQuery(days: Int) = mapOf("days" to days.toString())
fun reasoningQuery(model: String? = null, provider: String? = null) = buildMap {
    model?.takeIf { it.isNotBlank() }?.let { put("model", it) }
    provider?.takeIf { it.isNotBlank() }?.let { put("provider", it) }
}
fun skillContentQuery(name: String, file: String? = null) = buildMap {
    put("name", name)
    file?.let { put("file", it) }
}
fun exportQuery(sessionId: String, format: SessionExportFormat) = mapOf(
    "session_id" to sessionId,
    "format" to format.name.lowercase()
)

fun toMultipart(parts: Map<String, Pair<String?, RequestBody>>) = parts.map { (name, value) ->
    MultipartBody.Part.createFormData(
        name,
        value.first ?: "",
        value.second
    )
}

fun String.toMultipartBodyPart(filename: String? = null, mimeType: String? = null): RequestBody =
    toByteArray().toRequestBody(mimeType?.toMediaType() ?: "application/octet-stream".toMediaType())

fun InputStream.toMultipartBodyPart(filename: String, mimeType: String? = null): RequestBody {
    val bytes = readBytes()
    return bytes.toRequestBody(mimeType?.toMediaType() ?: "application/octet-stream".toMediaType(), 0, bytes.size)
}
