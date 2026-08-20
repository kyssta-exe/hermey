package com.kyssta.hermey.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
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

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _customHeaders = MutableStateFlow<List<com.kyssta.hermey.networking.CustomHeader>>(emptyList())
    val customHeaders: StateFlow<List<com.kyssta.hermey.networking.CustomHeader>> = _customHeaders.asStateFlow()

    private var _api: HermexApi? = null
    private var _baseUrl: String? = null

    val baseUrl: String? get() = _baseUrl
    val api: HermexApi get() = _api ?: error("No active server — call restoreSession first")

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
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
        scope.launch {
            val savedUrl = keychain.load("server_url").getOrNull()
            if (savedUrl != null) {
                getOrCreateApi(savedUrl)
                val savedServers = decodeJsonList(keychain.load("servers").getOrNull())
                if (savedServers.isNotEmpty()) {
                    _servers.value = savedServers
                    _activeServerId.value = savedServers.find { it.urlString == savedUrl }?.id
                }
                _state.value = AuthState.LoggedIn
            } else {
                _state.value = AuthState.Unconfigured
            }
        }
    }

    suspend fun testConnection(serverUrlString: String): Result<AuthStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val url = normalizeUrl(serverUrlString) ?: return@withContext Result.failure(APIError.InvalidServerURL)
            val api = createApiClient(url)
            // Skip /health — it redirects to /login (HTML) on authenticated gateways.
            // /api/auth/status returns JSON regardless and tells us the auth contract.
            val authStatus = api.authStatus()
            Result.success(authStatus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(serverUrlString: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = normalizeUrl(serverUrlString) ?: return@withContext Result.failure(APIError.InvalidServerURL)
            val api = createApiClient(url)
            val authStatus = api.authStatus()

            if (authStatus.authEnabled == true && authStatus.passwordAuthEnabled == false) {
                return@withContext Result.failure(Exception("This server uses passkey auth only — not supported yet"))
            }

            if (authStatus.authEnabled == true) {
                val loginJsonObj = com.google.gson.JsonObject().apply {
                    addProperty("password", password)
                }
                val loginResp = api.login(loginJsonObj)
                if (loginResp.ok != true) return@withContext Result.failure(APIError.Unauthorized)
            }

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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        _state.value = AuthState.Unconfigured
        _activeServerId.value = null
        _api = null
        _baseUrl = null
        keychain.delete("server_url")
    }

    fun setCustomHeaders(headers: List<CustomHeader>) {
        _customHeaders.value = headers
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

    fun handleUnauthorized() {
        _state.value = AuthState.LoggedOut
        _lastError.value = "Session expired — please sign in again"
    }

    private fun normalizeUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
        return try {
            val uri = java.net.URI(withScheme)
            val host = uri.host ?: return null
            val scheme = uri.scheme ?: "https"
            val port = uri.port.takeIf { it > 0 }
            val portSuffix = if (port != null && port != 80 && port != 443) ":$port" else ""
            "$scheme://$host$portSuffix"
        } catch (e: Exception) {
            null
        }
    }
}

fun encodeJsonList(list: List<ServerAccount>): String = Gson().toJson(list)
fun decodeJsonList(json: String?): List<ServerAccount> {
    if (json.isNullOrEmpty()) return emptyList()
    return Gson().fromJson(json, object : TypeToken<List<ServerAccount>>() {}.type)
}
