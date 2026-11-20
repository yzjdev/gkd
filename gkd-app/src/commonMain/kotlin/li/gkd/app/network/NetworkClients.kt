package li.gkd.app.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object NetworkClients {
    private val sharedClient = lazy { create() }
    val client: HttpClient
        get() = sharedClient.value

    /**
     * Streams can pause for minutes while a model thinks, so this client has no request or socket
     * timeout. Cancellation comes from the caller's coroutine.
     */
    private val streamingClient = lazy {
        create {
            install(HttpTimeout) {
                requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                connectTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            }
        }
    }
    val streamClient: HttpClient
        get() = streamingClient.value

    fun close() {
        if (sharedClient.isInitialized()) sharedClient.value.close()
        if (streamingClient.isInitialized()) streamingClient.value.close()
    }

    private fun create(configure: HttpClientConfig<*>.() -> Unit = {}) =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    },
                    ContentType.Any,
                )
            }
            engine { clientCacheSize = 0 }
            configure()
        }
}
