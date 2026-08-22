package com.kyssta.hermey.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.kyssta.hermey.networking.*

enum class AuthState { Unconfigured, LoggedIn, LoggedOut }

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keychain = KeychainStore(context)
    private val gson = Gson()

    private val _state = MutableStateFlow<AuthState>(AuthState.Unconfigured)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _servers = MutableStateFlow<List<ServerAccount>>(emptyList())
    val servers: StateFlow<List<ServerAccount>> = _servers.asStateFlow()

    private val _activeServerId = MutableStateFlow<String?>(null)
    val activeServerId: StateFlow<String?> = _activeServerId.asStateFlow()

    private var _api: HermexApi? = null
    private var _baseUrl: String? = null

    val baseUrl: String? get() = _baseUrl
    val api: HermexApi get() = _api ?: error("No active server — sign in first")

    fun getOrCreateApi(url: String): HermexApi {
        if (_api == null || _baseUrl != url) {
            _api = createApiClient(url)
            _baseUrl = url
        }
        return _api!!
    }

    init {
        restoreSession()
    }

    fun restoreSession() {
        // ponytail: fire-and-forget scope; lives for the process like the old one.
        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
        scope.launch {
            val savedUrl = keychain.load("server_url").getOrNull()
            if (savedUrl != null) {
                getOrCreateApi(savedUrl)
                val savedServers = decodeJsonList(keychain.load("servers").getOrNull())
                if (savedServers.isNotEmpty()) {
                    _servers.value = savedServers
                    _activeServerId.value = savedServers.find { it.urlString == savedUrl }?.id
                }
                // Cookie may have expired server-side; verify before claiming LoggedIn.
                try {
                    getOrCreateApi(savedUrl).authMe()
                    _state.value = AuthState.LoggedIn
                } catch (_: Exception) {
                    SharedCookieJar.clear()
                    _state.value = AuthState.Unconfigured
                }
            } else {
                _state.value = AuthState.Unconfigured
            }
        }
    }

    suspend fun testConnection(serverUrlString: String): Result<AuthProvidersResponse> = withContext(Dispatchers.IO) {
        try {
            val url = normalizeUrl(serverUrlString) ?: return@withContext Result.failure(APIError.InvalidServerURL)
            Result.success(createApiClient(url).getAuthProviders())
        } catch (e: Exception) {
            Result.failure(Exception(apiErrorMessage(e)))
        }
    }

    suspend fun login(serverUrlString: String, username: String, password: String, provider: String = "basic"): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val url = normalizeUrl(serverUrlString)
                    ?: return@withContext Result.failure(APIError.InvalidServerURL)
                val api = createApiClient(url)

                // POST /auth/password-login → 200 {"ok":true} + Set-Cookie session.
                val resp = api.passwordLogin(JsonObject().apply {
                    addProperty("provider", provider)
                    addProperty("username", username)
                    addProperty("password", password)
                })
                if (resp.ok != true) {
                    return@withContext Result.failure(Exception(resp.detail ?: "Invalid username or password"))
                }

                // Verify the session cookie actually authenticates.
                try {
                    api.authMe()
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception(apiErrorMessage(e)))
                }

                saveServer(url)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception(apiErrorMessage(e)))
            }
        }

    /** Mint a single-use ws ticket for the /api/ws chat socket. */
    suspend fun mintWsTicket(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ticket = api.wsTicket().ticket
            if (ticket.isNullOrBlank()) Result.failure(Exception("Could not get stream ticket"))
            else Result.success(ticket)
        } catch (e: Exception) {
            Result.failure(Exception(apiErrorMessage(e)))
        }
    }

    private fun saveServer(url: String) {
        keychain.save(url, "server_url")
        val currentServers = _servers.value.toMutableList()
        var found = false
        for (i in currentServers.indices) {
            if (currentServers[i].id == url) {
                currentServers[i] = currentServers[i].copy(updatedAt = System.currentTimeMillis() / 1000.0)
                found = true
                break
            }
        }
        if (!found) {
            val hostFallback = url.removePrefix("https://").removePrefix("http://").split(":")[0]
            currentServers.add(ServerAccount(
                id = url, urlString = url,
                displayName = hostFallback, initials = hostFallback.take(2).uppercase(),
                headerLogoColorHex = "#1EB2AA",
                createdAt = System.currentTimeMillis() / 1000.0,
                updatedAt = System.currentTimeMillis() / 1000.0
            ))
        }
        _servers.value = currentServers
        _activeServerId.value = url
        _state.value = AuthState.LoggedIn
        getOrCreateApi(url)
        keychain.save(encodeJsonList(currentServers), "servers")
    }

    fun logout() {
        // Best-effort: drop cookies locally; server session expires on its own.
        _state.value = AuthState.Unconfigured
        _activeServerId.value = null
        _api = null
        _baseUrl = null
        SharedCookieJar.clear()
        keychain.delete("server_url")
    }

    fun setActiveServer(serverId: String) {
        val server = _servers.value.find { it.id == serverId } ?: return
        _activeServerId.value = serverId
        getOrCreateApi(server.urlString)
        keychain.save(server.urlString, "server_url")
    }

    fun removeServer(serverId: String) {
        val newServers = _servers.value.filterNot { it.id == serverId }.toMutableList()
        _servers.value = newServers
        if (_activeServerId.value == serverId) {
            _activeServerId.value = newServers.firstOrNull()?.id
            if (_activeServerId.value != null) {
                val next = newServers.first { it.id == _activeServerId.value }
                getOrCreateApi(next.urlString)
                keychain.save(next.urlString, "server_url")
            } else {
                _state.value = AuthState.Unconfigured
                _api = null
                _baseUrl = null
            }
        }
        keychain.save(encodeJsonList(newServers), "servers")
    }

    private fun normalizeUrl(raw: String): String? {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isEmpty()) return null
        val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
        return try {
            val uri = java.net.URI(withScheme)
            val host = uri.host ?: return null
            val scheme = uri.scheme ?: "https"
            val port = uri.port.takeIf { it > 0 }
            val portSuffix = if (port != null && port != 80 && port != 443) ":$port" else ""
            "$scheme://$host$portSuffix"
        } catch (_: Exception) {
            null
        }
    }
}

fun encodeJsonList(list: List<ServerAccount>): String = Gson().toJson(list)
fun decodeJsonList(json: String?): List<ServerAccount> {
    if (json.isNullOrEmpty()) return emptyList()
    return Gson().fromJson(json, object : TypeToken<List<ServerAccount>>() {}.type)
}
