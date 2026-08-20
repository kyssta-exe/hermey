package com.kyssta.hermey.networking

// ─── SSE Event Types ──────────────────────────────────────────────────────────
sealed class SSEEvent {
    data class Token(val text: String) : SSEEvent()
    data class InterimAssistant(val text: String, val alreadyStreamed: Boolean? = null) : SSEEvent()
    data class Reasoning(val text: String) : SSEEvent()
    data class ToolStarted(
        val name: String?,
        val preview: String?,
        val args: Map<String, Any>?,
        val duration: Double?,
        val isError: Boolean?,
        val stableID: String?
    ) : SSEEvent()
    data class ToolCompleted(
        val name: String?,
        val preview: String?,
        val args: Map<String, Any>?,
        val duration: Double?,
        val isError: Boolean?,
        val stableID: String?
    ) : SSEEvent()
    data class Title(val sessionId: String?, val title: String?) : SSEEvent()
    data class Metering(
        val tps: Double?,
        val tpsAvailable: Boolean?,
        val estimated: Boolean?,
        val sessionId: String?
    ) : SSEEvent()
    data class Done(val usage: ContextWindowSnapshot?, val session: SessionDetail?) : SSEEvent()
    data class ApprovalPending(val response: ApprovalPendingResponse) : SSEEvent()
    data class ClarificationPending(val response: ClarificationPendingResponse) : SSEEvent()
    data class PendingSteerLeftover(val text: String) : SSEEvent()
    data object StreamEnd : SSEEvent()
    data object Cancelled : SSEEvent()
    data class Error(val message: String) : SSEEvent()
    data class TransportError(val message: String) : SSEEvent()
    data object Heartbeat : SSEEvent()
    data object Ignored : SSEEvent()
}

data class ContextWindowSnapshot(
    @Suppress("unused") val tokensUsed: Int? = null,
    @Suppress("unused") val tokensTotal: Int? = null,
    @Suppress("unused") val tokensRemaining: Int? = null,
    @Suppress("unused") val usagePercent: Double? = null
)

// ─── SSE Event Decoder ─────────────────────────────────────────────────────────
object SSEEventDecoder {
    fun decode(eventType: String, data: String): SSEEvent {
        return try {
            when (eventType) {
                "token" -> SSEEvent.Token(data.trim().removePrefix("\"").removeSuffix("\""))
                "interim_assistant" -> {
                    val obj = data.parseJsonObject()
                    SSEEvent.InterimAssistant(
                        text = obj["text"]?.toString() ?: "",
                        alreadyStreamed = obj["already_streamed"]?.toString()?.toBooleanStrictOrNull()
                    )
                }
                "reasoning" -> SSEEvent.Reasoning(data.trim())
                "tool" -> {
                    val obj = data.parseJsonObject()
                    SSEEvent.ToolStarted(
                        name = obj["name"]?.toString(),
                        preview = obj["preview"]?.toString(),
                        args = obj["args"]?.toString()?.parseJsonObject(),
                        duration = obj["duration"]?.toString()?.toDoubleOrNull(),
                        isError = obj["is_error"]?.toString()?.toBooleanStrictOrNull(),
                        stableID = obj["tid"]?.toString() ?: obj["id"]?.toString()
                    )
                }
                "tool_complete" -> {
                    val obj = data.parseJsonObject()
                    SSEEvent.ToolCompleted(
                        name = obj["name"]?.toString(),
                        preview = obj["preview"]?.toString(),
                        args = obj["args"]?.toString()?.parseJsonObject(),
                        duration = obj["duration"]?.toString()?.toDoubleOrNull(),
                        isError = obj["is_error"]?.toString()?.toBooleanStrictOrNull(),
                        stableID = obj["tid"]?.toString() ?: obj["id"]?.toString()
                    )
                }
                "title" -> {
                    val obj = data.parseJsonObject()
                    SSEEvent.Title(obj["session_id"]?.toString(), obj["title"]?.toString())
                }
                "metering" -> {
                    val obj = data.parseJsonObject()
                    SSEEvent.Metering(
                        tps = obj["tps"]?.toString()?.toDoubleOrNull(),
                        tpsAvailable = obj["tps_available"]?.toString()?.toBooleanStrictOrNull(),
                        estimated = obj["estimated"]?.toString()?.toBooleanStrictOrNull(),
                        sessionId = obj["session_id"]?.toString()
                    )
                }
                "done" -> {
                    val obj = data.parseJsonObject()
                    val usage = obj["usage"]?.toString()?.parseJsonObject()?.let { u ->
                        ContextWindowSnapshot(
                            tokensUsed = u["tokens_used"]?.toString()?.toIntOrNull(),
                            tokensTotal = u["tokens_total"]?.toString()?.toIntOrNull(),
                            tokensRemaining = u["tokens_remaining"]?.toString()?.toIntOrNull(),
                            usagePercent = u["usage_percent"]?.toString()?.toDoubleOrNull()
                        )
                    }
                    SSEEvent.Done(usage = usage, session = null)
                }
                "initial" -> {
                    val obj = data.parseJsonObject()
                    if (obj.containsKey("clarification_id")) {
                        SSEEvent.ClarificationPending(
                            ClarificationPendingResponse(
                                clarificationId = obj["clarification_id"]?.toString(),
                                question = obj["question"]?.toString(),
                                sessionId = obj["session_id"]?.toString()
                            )
                        )
                    } else {
                        SSEEvent.ApprovalPending(
                            ApprovalPendingResponse(
                                approvalId = obj["approval_id"]?.toString(),
                                toolName = obj["tool_name"]?.toString(),
                                toolPreview = obj["tool_preview"]?.toString(),
                                message = obj["message"]?.toString(),
                                sessionId = obj["session_id"]?.toString()
                            )
                        )
                    }
                }
                "approval" -> {
                    val obj = data.parseJsonObject()
                    SSEEvent.ApprovalPending(
                        ApprovalPendingResponse(
                            approvalId = obj["approval_id"]?.toString(),
                            toolName = obj["tool_name"]?.toString(),
                            toolPreview = obj["tool_preview"]?.toString(),
                            message = obj["message"]?.toString(),
                            sessionId = obj["session_id"]?.toString()
                        )
                    )
                }
                "clarify" -> {
                    val obj = data.parseJsonObject()
                    SSEEvent.ClarificationPending(
                        ClarificationPendingResponse(
                            clarificationId = obj["clarification_id"]?.toString(),
                            question = obj["question"]?.toString(),
                            sessionId = obj["session_id"]?.toString()
                        )
                    )
                }
                "stream_end" -> SSEEvent.StreamEnd
                "cancel" -> SSEEvent.Cancelled
                "error", "apperror" -> {
                    val obj = data.parseJsonObject()
                    val msg = obj["message"]?.toString() ?: obj["error"]?.toString() ?: "Stream error"
                    SSEEvent.Error(msg)
                }
                "pending_steer_leftover" -> SSEEvent.PendingSteerLeftover(data.trim())
                "heartbeat", "comment" -> SSEEvent.Heartbeat
                else -> SSEEvent.Ignored
            }
        } catch (e: Exception) {
            SSEEvent.Error("Failed to decode SSE event '$eventType': ${e.message}")
        }
    }

    private fun String.parseJsonObject(): Map<String, Any> {
        return try {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(this.trim(), type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
