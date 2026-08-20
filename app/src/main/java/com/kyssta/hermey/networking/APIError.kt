package com.kyssta.hermey.networking

sealed class APIError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object InvalidServerURL : APIError("Invalid server URL")
    data object Unauthorized : APIError("Unauthorized — sign in again")
    data object NoInternet : APIError("No internet connection")
    class HTTP(val code: Int, body: String?) : APIError("HTTP $code: ${body ?: "unknown"}")
    class Network(cause: Throwable) : APIError("Network error: ${cause.message}", cause)
    class Decoding(cause: Throwable) : APIError("Decoding error: ${cause.message}", cause)
}
