package com.kyssta.hermey.networking

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * JSON-RPC over /api/ws — the real gateway chat transport.
 *
 * Frames:
 *   client → {"jsonrpc":"2.0","id":N,"method":"session.create"|"prompt.submit"|...,"params":{...}}
 *   server → {"jsonrpc":"2.0","id":N,"result":{...}}                    (RPC replies)
 *   server → {"jsonrpc":"2.0","method":"event","params":{"type":"message.delta","payload":{"text":"..."}}}
 *
 * Auth: single-use 30s ticket minted at POST /api/auth/ws-ticket (cookie-authed).
 */
class GatewayWsClient(
    baseUrl: String,
    ticket: String,
) {
    private val url = buildUrl(
        baseUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://"),
        Endpoints.CHAT_WS,
        mapOf("ticket" to ticket)
    )

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // websockets never time out on read
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val frames = LinkedBlockingQueue<Map<String, Any?>>(256)
    @Volatile private var failed: String? = null
    private var ws: WebSocket? = null
    private val gson = Gson()

    fun connect(onOpen: () -> Unit) {
        ws = client.newWebSocket(
            Request.Builder().url(url).header("Origin", url.substringBefore("/api")).build(),
            object : okhttp3.WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) { onOpen() }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        frames.put(gson.fromJson(text, Map::class.java) as Map<String, Any?>)
                    } catch (_: Exception) { /* skip undecodable frame */ }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    failed = response?.let { "HTTP ${it.code}" } ?: (t.message ?: "connection failed")
                }
            }
        )
    }

    /** Next server frame, blocking up to [timeoutSec]. Null on timeout/close/failure. */
    fun nextFrame(timeoutSec: Long = 180): Map<String, Any?>? {
        if (failed != null && frames.isEmpty()) return null
        return frames.poll(timeoutSec, TimeUnit.SECONDS)
    }

    fun lastError(): String? = failed

    fun rpc(method: String, params: Map<String, Any?>, id: Int): Boolean =
        ws?.send(gson.toJson(mapOf("jsonrpc" to "2.0", "id" to id, "method" to method, "params" to params))) ?: false

    fun close() {
        ws?.close(1000, "bye")
    }
}

/** Extract (type, payload) from a server event frame, or null for RPC replies. */
fun frameEvent(frame: Map<String, Any?>): Pair<String, Map<String, Any?>>? {
    if (frame["method"] != "event") return null
    @Suppress("UNCHECKED_CAST")
    val params = frame["params"] as? Map<String, Any?> ?: return null
    val type = params["type"]?.toString() ?: return null
    @Suppress("UNCHECKED_CAST")
    val payload = params["payload"] as? Map<String, Any?> ?: emptyMap()
    return type to payload
}

/** True when the frame is the RPC reply carrying the given request id. */
fun frameReply(frame: Map<String, Any?>, id: Int): Boolean =
    (frame["id"] as? Double)?.toInt() == id

/** Error message from an RPC error reply, if present. */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.errMessage(): String? =
    (this["error"] as? Map<String, Any?>)?.get("message")?.toString()
