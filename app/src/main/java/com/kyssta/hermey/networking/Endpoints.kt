package com.kyssta.hermey.networking

object Endpoints {
    const val CHAT_WS = "/api/ws"
    const val SESSION_MESSAGES = "/api/sessions/{session_id}/messages"
}

fun buildUrl(baseUrl: String, path: String, query: Map<String, String?>): String {
    val base = baseUrl.removeSuffix("/")
    val cleanPath = path.removePrefix("/")
    val params = query.filter { (_, v) -> v != null }
        .map { (k, v) -> "$k=${java.net.URLEncoder.encode(v!!, "UTF-8")}" }
        .joinToString("&")
    return if (params.isNotBlank()) "$base/$cleanPath?$params" else "$base/$cleanPath"
}

data class CustomHeader(val name: String, val value: String)
