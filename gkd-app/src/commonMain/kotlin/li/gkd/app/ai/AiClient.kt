package li.gkd.app.ai

import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readLine
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import li.gkd.app.network.NetworkClients
import li.gkd.app.network.logNetworkFailure

/** Stream chunks are decoded one by one, so unknown fields must never fail a chunk. */
private val streamJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
)

@Serializable private data class ChatMessage(val role: String, val content: String)

/** One turn of a chat; [role] is "system", "user" or "assistant". */
data class AiMessage(val role: String, val content: String)

@Serializable
private data class ChatResponse(@SerialName("choices") val choices: List<ChatChoice> = emptyList())

@Serializable private data class ChatChoice(val message: ChatMessage? = null)

@Serializable
private data class StreamResponse(@SerialName("choices") val choices: List<StreamChoice> = emptyList())

@Serializable private data class StreamChoice(val delta: StreamDelta? = null)

@Serializable private data class StreamDelta(val content: String? = null)

@Serializable
private data class ModelsResponse(@SerialName("data") val data: List<ModelEntry> = emptyList())

@Serializable private data class ModelEntry(val id: String = "")

class AiRequestException(override val message: String) : Exception(message)

/**
 * Only the transport lives here; callers own prompts, results and user feedback.
 */
object AiClient {
    private fun endpoint(baseUrl: String, path: String) = baseUrl.trimEnd('/') + path

    /** Models the key may use, so the page never asks the user to type an id by hand. */
    suspend fun models(connection: AiConnection): List<String> {
        val response =
            try {
                NetworkClients.client.get(endpoint(connection.endpoint, "/models")) {
                    expectSuccess = true
                    bearerAuth(connection.apiKey)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logNetworkFailure("AI models", e)
                throw AiRequestException(e.message ?: e.javaClass.simpleName)
            }
        return response
            .body<ModelsResponse>()
            .data
            .map { it.id }
            .filter { it.isNotBlank() }
            .sorted()
    }

    suspend fun complete(
        connection: AiConnection,
        prompt: String,
        systemPrompt: String? = null,
    ): String {
        val response =
            try {
                NetworkClients.client.post(endpoint(connection.endpoint, "/chat/completions")) {
                    expectSuccess = true
                    bearerAuth(connection.apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(
                        ChatRequest(
                            model = connection.model,
                            messages =
                                chatMessages(prompt, systemPrompt).map {
                                    ChatMessage(it.role, it.content)
                                },
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logNetworkFailure("AI request", e)
                throw AiRequestException(e.message ?: e.javaClass.simpleName)
            }
        return response
            .body<ChatResponse>()
            .choices
            .firstOrNull()
            ?.message
            ?.content
            ?.takeIf { it.isNotBlank() }
            ?: throw AiRequestException("Empty response")
    }

    /**
     * Streams the answer as it is generated. Generation can take minutes for long rules, so the
     * request has no timeout; cancellation comes from the caller's coroutine.
     */
    suspend fun streamComplete(
        connection: AiConnection,
        prompt: String,
        systemPrompt: String? = null,
        onDelta: (String) -> Unit,
    ): String = streamMessages(connection, chatMessages(prompt, systemPrompt), onDelta)

    /** Multi-turn variant: [messages] is the full history, oldest first. */
    suspend fun streamMessages(
        connection: AiConnection,
        messages: List<AiMessage>,
        onDelta: (String) -> Unit,
    ): String {
        val text = StringBuilder()
        try {
            NetworkClients.streamClient
                .preparePost(endpoint(connection.endpoint, "/chat/completions")) {
                    expectSuccess = true
                    bearerAuth(connection.apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(
                        ChatRequest(
                            model = connection.model,
                            messages = messages.map { ChatMessage(it.role, it.content) },
                            stream = true,
                        )
                    )
                }
                .execute { response ->
                    val channel = response.bodyAsChannel()
                    while (true) {
                        val line = channel.readLine() ?: break
                        if (!line.startsWith("data:")) continue
                        val payload = line.removePrefix("data:").trim()
                        if (payload.isEmpty()) continue
                        if (payload == "[DONE]") break
                        val delta =
                            parseStreamChunk(payload)
                                .choices
                                .firstOrNull()
                                ?.delta
                                ?.content
                        if (!delta.isNullOrEmpty()) {
                            text.append(delta)
                            onDelta(delta)
                        }
                    }
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logNetworkFailure("AI stream", e)
            throw AiRequestException(e.message ?: e.javaClass.simpleName)
        }
        return text.toString()
    }

    private fun chatMessages(prompt: String, systemPrompt: String?): List<AiMessage> = buildList {
        if (!systemPrompt.isNullOrEmpty()) add(AiMessage("system", systemPrompt))
        add(AiMessage("user", prompt))
    }

    /** A malformed chunk ends the stream instead of failing the whole answer. */
    private fun parseStreamChunk(payload: String): StreamResponse =
        try {
            streamJson.decodeFromString<StreamResponse>(payload)
        } catch (_: Exception) {
            StreamResponse()
        }
}
