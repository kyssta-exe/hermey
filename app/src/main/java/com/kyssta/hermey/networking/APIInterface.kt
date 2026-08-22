package com.kyssta.hermey.networking

import com.google.gson.JsonElement
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kyssta.hermey.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ─── API Interface (real hermes-webui gateway surface) ──────────────────────
interface HermexApi {
    // Auth
    @GET("/api/auth/providers")
    suspend fun getAuthProviders(): AuthProvidersResponse

    @POST("/auth/password-login")
    suspend fun passwordLogin(@Body body: JsonElement): LoginResponse

    @POST("/api/auth/ws-ticket")
    suspend fun wsTicket(): WsTicketResponse

    @GET("/api/auth/me")
    suspend fun authMe(): AuthMeResponse

    // Sessions
    @GET("/api/sessions")
    suspend fun getSessions(@QueryMap query: Map<String, String> = emptyMap()): SessionsResponse

    @GET("/api/sessions/{session_id}/messages")
    suspend fun getSessionMessages(
        @Path("session_id") sessionId: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int = 0,
    ): MessagesResponse

    @PATCH("/api/sessions/{session_id}")
    suspend fun updateSession(
        @Path("session_id") sessionId: String,
        @Body body: JsonElement,
    ): SessionMutationResponse

    @DELETE("/api/sessions/{session_id}")
    suspend fun deleteSession(@Path("session_id") sessionId: String): JsonElement

    // Crons
    @GET("/api/cron/jobs")
    suspend fun getCrons(): List<CronJob>

    @POST("/api/cron/jobs/{job_id}/pause")
    suspend fun pauseCron(@Path("job_id") jobId: String): JsonElement

    @POST("/api/cron/jobs/{job_id}/resume")
    suspend fun resumeCron(@Path("job_id") jobId: String): JsonElement

    // Skills
    @GET("/api/skills")
    suspend fun getSkills(): List<SkillInfo>

    @PUT("/api/skills/toggle")
    suspend fun toggleSkill(@Body body: JsonElement): JsonElement

    // Memory
    @GET("/api/memory")
    suspend fun getMemory(): MemoryStatus
}

// ─── JSON ─────────────────────────────────────────────────────────────────────
object GsonSingleton {
    val instance: Gson by lazy { GsonBuilder().setLenient().create() }
}

// ─── Cookie jar — persists the gateway session cookie across requests ────────
object SharedCookieJar : okhttp3.CookieJar {
    private val cookies = java.util.concurrent.ConcurrentHashMap<String, MutableList<okhttp3.Cookie>>()

    override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
        val list = this.cookies.getOrPut(url.host) { mutableListOf() }
        synchronized(list) {
            list.removeAll { existing -> cookies.any { it.name == existing.name } }
            list.addAll(cookies)
        }
    }

    override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
        val list = cookies[url.host] ?: return emptyList()
        synchronized(list) {
            list.removeAll { it.expiresAt < System.currentTimeMillis() && it.expiresAt != 0L }
            return list.toList()
        }
    }

    fun clear() { cookies.clear() }
}

fun sharedOkHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .cookieJar(SharedCookieJar)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        })
        .build()

fun createApiClient(baseUrl: String): HermexApi =
    Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(sharedOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create(GsonSingleton.instance))
        .build()
        .create(HermexApi::class.java)

/** Human-readable message from any throwable thrown by the Retrofit stack. */
fun apiErrorMessage(e: Throwable): String = when (e) {
    is HttpException -> {
        val detail = try {
            val body = e.response()?.errorBody()?.string()
            val map = GsonSingleton.instance.fromJson(body, Map::class.java)
            map?.get("detail")?.toString()
        } catch (_: Exception) { null }
        when (e.code()) {
            401 -> detail ?: "Invalid username or password"
            in 500..599 -> "Server error (${e.code()})"
            else -> detail ?: "HTTP ${e.code()}"
        }
    }
    is java.io.IOException -> "Cannot reach server — check the URL and your connection"
    else -> e.message ?: "Unexpected error"
}
