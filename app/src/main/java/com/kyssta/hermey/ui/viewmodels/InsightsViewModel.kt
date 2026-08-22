package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.networking.GatewayWsClient
import com.kyssta.hermey.networking.frameEvent
import com.kyssta.hermey.networking.frameReply
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsData(
    val sessions: Int = 0,
    val messages: Int = 0,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _insights = MutableStateFlow<InsightsData?>(null)
    val insights: StateFlow<InsightsData?> = _insights.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadInsights(days: Int = 30) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            try {
                // insights.get is a WS RPC, not REST — same ticketed /api/ws channel.
                val ticket = authManager.mintWsTicket().getOrNull()
                val baseUrl = authManager.baseUrl
                if (ticket == null || baseUrl == null) { _loading.value = false; return@launch }

                val client = GatewayWsClient(baseUrl, ticket)
                var opened = false
                client.connect { opened = true }
                try {
                    val openDeadline = System.currentTimeMillis() + 20_000
                    while (!opened && System.currentTimeMillis() < openDeadline) {
                        val f = client.nextFrame(1) ?: break
                        if (frameEvent(f)?.first == "gateway.ready") { opened = true; break }
                    }
                    if (!opened) return@launch

                    client.rpc("insights.get", mapOf("days" to days), 1)
                    val deadline = System.currentTimeMillis() + 30_000
                    while (System.currentTimeMillis() < deadline) {
                        val frame = client.nextFrame(5) ?: break
                        if (frameReply(frame, 1)) {
                            @Suppress("UNCHECKED_CAST")
                            val result = frame["result"] as? Map<String, Any?>
                            _insights.value = InsightsData(
                                sessions = (result?.get("sessions") as? Double)?.toInt() ?: 0,
                                messages = (result?.get("messages") as? Double)?.toInt() ?: 0,
                            )
                            break
                        }
                    }
                } finally {
                    client.close()
                }
            } finally {
                _loading.value = false
            }
        }
    }
}
