package com.kyssta.hermey.networking

/**
 * Sealed class kept for compatibility with older screens. The real gateway
 * chat transport is JSON-RPC over /api/ws (see GatewayWsClient).
 */
sealed class SSEEvent {
    data object Ignored : SSEEvent()
}
