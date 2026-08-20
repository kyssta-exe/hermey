package com.kyssta.hermey.networking

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.kyssta.hermey.BuildConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.InputStream
import java.util.concurrent.TimeUnit

// ─── API Interface ────────────────────────────────────────────────────────────
interface HermexApi {
    // Auth
    @GET("/health")
    suspend fun health(): HealthResponse

    @GET("/api/auth/providers")
    suspend fun getAuthProviders(): AuthProvidersResponse

    @POST("/auth/password-login")
    suspend fun passwordLogin(@Body body: JsonElement): LoginResponse

    @POST("/auth/logout")
    suspend fun logout(): LoginResponse

    @GET("/api/auth/me")
    suspend fun authMe(): AuthMeResponse

    // Sessions
    @GET(Endpoints.SESSIONS)
    suspend fun getSessions(@QueryMap query: Map<String, String> = emptyMap()): SessionsResponse

    @GET(Endpoints.SESSIONS_SEARCH)
    suspend fun searchSessions(
        @Query("q") q: String,
        @Query("content") content: Boolean,
        @Query("depth") depth: Int,
    ): SessionsResponse

    @GET(Endpoints.SESSION)
    suspend fun getSession(
        @Query("session_id") sessionId: String,
        @Query("messages") messages: Boolean = true,
        @Query("msg_limit") msgLimit: Int? = null,
        @Query("msg_before") msgBefore: String? = null,
        @Query("expand_renderable") expandRenderable: Boolean = false,
    ): SessionDetail

    @GET(Endpoints.SESSION_STATUS)
    suspend fun sessionStatus(@Query("session_id") sessionId: String): JsonElement

    @POST(Endpoints.SESSION_NEW)
    suspend fun newSession(@Body body: JsonElement): SessionDetail

    @POST(Endpoints.SESSION_RENAME)
    suspend fun renameSession(@Body body: JsonElement): SessionMutationResponse

    @POST(Endpoints.SESSION_DELETE)
    suspend fun deleteSession(@Body body: JsonElement): SessionMutationResponse

    @POST(Endpoints.SESSION_PIN)
    suspend fun pinSession(@Body body: JsonElement): SessionMutationResponse

    @POST(Endpoints.SESSION_ARCHIVE)
    suspend fun archiveSession(@Body body: JsonElement): SessionMutationResponse

    @POST(Endpoints.SESSION_BRANCH)
    suspend fun branchSession(@Body body: JsonElement): SessionBranchResponse

    @POST(Endpoints.SESSION_COMPRESS)
    suspend fun compressSession(@Body body: JsonElement): SessionCompressResponse

    // Chat
    @POST(Endpoints.CHAT_START)
    suspend fun startChat(@Body body: JsonElement): StartResponse

    @POST(Endpoints.CHAT_CANCEL)
    suspend fun cancelChat(@Query("stream_id") streamId: String): JsonElement

    @POST(Endpoints.CHAT_STEER)
    suspend fun steerChat(@Body body: JsonElement): JsonElement

    // Models & Providers
    @GET(Endpoints.MODELS)
    suspend fun getModels(): ModelsResponse

    @GET(Endpoints.MODELS_LIVE)
    suspend fun getModelsLive(): ModelsResponse

    @GET(Endpoints.DEFAULT_MODEL)
    suspend fun getDefaultModel(): DefaultModelResponse

    @GET(Endpoints.PROVIDERS)
    suspend fun getProviders(): ProvidersResponse

    @GET(Endpoints.PROFILES)
    suspend fun getProfiles(): ProfilesResponse

    // Workspaces
    @GET(Endpoints.WORKSPACES)
    suspend fun getWorkspaces(): WorkspacesResponse

    // Crons
    @GET(Endpoints.CRONS)
    suspend fun getCrons(): CronsResponse

    @POST(Endpoints.CRON_PAUSE)
    suspend fun pauseCron(@Query("job_id") jobId: String): JsonElement

    @POST(Endpoints.CRON_RESUME)
    suspend fun resumeCron(@Query("job_id") jobId: String): JsonElement

    // Skills
    @GET(Endpoints.SKILLS)
    suspend fun getSkills(): SkillsResponse

    @POST(Endpoints.SKILLS_TOGGLE)
    suspend fun toggleSkill(@Body body: JsonElement): JsonElement

    // Memory
    @GET(Endpoints.MEMORY)
    suspend fun getMemory(): MemoryResponse

    // Insights
    @GET(Endpoints.INSIGHTS)
    suspend fun getInsights(@Query("days") days: Int): InsightsResponse

    // Kanban
    @GET(Endpoints.KANBAN_CONFIG)
    suspend fun getKanbanConfig(): KanbanConfig

    // Workspace
    @GET(Endpoints.DIRECTORY_LIST)
    suspend fun listDirectory(
        @Query("session_id") sessionId: String,
        @Query("path") path: String? = null,
    ): DirectoryResponse

    @GET(Endpoints.FILE)
    suspend fun readFile(
        @Query("session_id") sessionId: String,
        @Query("path") path: String,
    ): String

    // Upload / transcribe / tts
    @Multipart
    @POST(Endpoints.UPLOAD)
    suspend fun uploadFile(@Part parts: List<MultipartBody.Part>): JsonElement

    @Multipart
    @POST(Endpoints.TRANSCRIBE)
    suspend fun transcribeAudio(@Part parts: List<MultipartBody.Part>): JsonElement

    @POST(Endpoints.TTS)
    suspend fun textToSpeech(@Body body: JsonElement): JsonElement
}

// ─── Client Factory ───────────────────────────────────────────────────────────

// Shared cookie jar — persists session cookies across all requests (login → API calls)
object SharedCookieJar : okhttp3.CookieJar {
    private val cookies = java.util.concurrent.ConcurrentHashMap<String, List<okhttp3.Cookie>>()

    override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
        this.cookies[url.host] = cookies
    }

    override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
        return cookies[url.host] ?: emptyList()
    }

    fun clear() { cookies.clear() }

    fun saveFromResponseString(url: okhttp3.HttpUrl, setCookieHeaders: List<String>) {
        val parsed = mutableListOf<okhttp3.Cookie>()
        for (header in setCookieHeaders) {
            parsed.add(okhttp3.Cookie.parse(url, header))
        }
        if (parsed.isNotEmpty()) {
            cookies[url.host] = (cookies[url.host] ?: emptyList()) + parsed.filter { it.expiresAt >= System.currentTimeMillis() }
        }
    }
}

fun createApiClient(baseUrl: String): HermexApi {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .cookieJar(SharedCookieJar)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(GsonSingleton.instance))
        .build()
        .create(HermexApi::class.java)
}

// ─── SSE Client ───────────────────────────────────────────────────────────────
class SSEClient(
    private val baseUrl: String,
    private val customHeaders: () -> List<CustomHeader> = { emptyList() }
) {
    fun stream(
        path: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        onEvent: (SSEEvent) -> Unit,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ): kotlinx.coroutines.Job? {
        return kotlinx.coroutines.GlobalScope.launch {
            try {
                val url = buildUrl(baseUrl, path, query)
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .cookieJar(SharedCookieJar)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("Accept", "text/event-stream")
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    .also { reqBuilder ->
                        headers.forEach { (k, v) -> reqBuilder.header(k, v) }
                        customHeaders().forEach { reqBuilder.header(it.name, it.value) }
                    }
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        onError(Exception("HTTP ${response.code}"))
                        return@launch
                    }
                    val body = response.body ?: throw Exception("No response body")
                    val source = body.source()
                    var line: String?
                    var buffer = ""
                    var eventType = ""

                    while (!source.exhausted() && !Thread.currentThread().isInterrupted) {
                        line = source.readUtf8Line() ?: break
                        when {
                            line.isEmpty() -> {
                                if (buffer.isNotEmpty()) {
                                    val event = SSEEventDecoder.decode(eventType, buffer)
                                    onEvent(event)
                                }
                                buffer = ""
                                eventType = ""
                            }
                            line.startsWith("event:") -> eventType = line.removePrefix("event:").trim()
                            line.startsWith("data:") -> buffer += if (buffer.isNotEmpty()) "\n${line.removePrefix("data:")}" else line.removePrefix("data:")
                        }
                    }
                    onComplete()
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    onError(e)
                }
            }
        }
    }
}

// ─── Utilities ────────────────────────────────────────────────────────────────
