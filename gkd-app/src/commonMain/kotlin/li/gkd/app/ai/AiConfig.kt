package li.gkd.app.ai

/** Used when the field is left empty; any OpenAI-compatible endpoint works. */
const val AiDefaultBaseUrl = "https://api.openai.com/v1"

data class AiConnection(
    val apiKey: String,
    val baseUrl: String = "",
    val model: String = "",
) {
    /** Blank input keeps the default endpoint instead of sending to an empty host. */
    val endpoint: String
        get() = baseUrl.trim().ifEmpty { AiDefaultBaseUrl }
}

fun AiConnection.isConfigured(): Boolean =
    endpoint.isNotEmpty() && apiKey.isNotEmpty() && model.isNotEmpty()
