package li.gkd.app.ai

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import li.gkd.app.model.ComplexSnapshot
import li.gkd.app.subscription.RawSubscription
import li.gkd.app.subscription.RawSubscriptionParser
import li.gkd.app.subscription.RuleGroupCompilation
import li.gkd.app.subscription.SubscriptionJson
import li.songe.json5.Json5

sealed interface AiRuleResult {
    data class Success(val group: RawSubscription.RawAppGroup, val text: String) : AiRuleResult

    /** The model returned text that is not a valid rule group. */
    data class Invalid(val detail: String) : AiRuleResult
}

/** A model often wraps JSON5 in a Markdown fence or adds prose around it. */
private fun String.stripToJson(): String {
    val trimmed = trim()
    val fenced =
        if (trimmed.startsWith("```")) {
            trimmed.substringAfter('\n', "").substringBeforeLast("```", "").trim()
        } else {
            trimmed
        }
    // Keep only the outermost object so trailing explanations cannot break parsing.
    val start = fenced.indexOf('{')
    val end = fenced.lastIndexOf('}')
    return if (start >= 0 && end > start) fenced.substring(start, end + 1) else fenced
}

/** The model may return the group itself or wrap it in an app or subscription object. */
private fun JsonObject.unwrapGroup(): JsonObject {
    val firstApp = (this["apps"] as? JsonArray)?.firstOrNull() as? JsonObject
    val holder = firstApp ?: this
    val groups = holder["groups"] as? JsonArray
    return (groups?.firstOrNull() as? JsonObject) ?: holder
}

/**
 * The group key and name are required by the parser but not always emitted by a model, so they are
 * filled in before validation instead of rejecting an otherwise usable rule.
 */
private fun JsonObject.normalizedGroup(): JsonObject {
    val result = toMutableMap()
    result["key"] = JsonPrimitive(result["key"].asIntOrNull() ?: 0)
    result["name"] =
        JsonPrimitive(result["name"].asTextOrNull()?.takeIf { it.isNotBlank() } ?: "AI")
    val rules = result["rules"]
    result["rules"] =
        when (rules) {
            null,
            is JsonNull -> JsonArray(emptyList())
            is JsonArray ->
                JsonArray(
                    rules.mapIndexed { index, element ->
                        (element as? JsonObject)?.normalizedRule(index) ?: element
                    }
                )

            is JsonObject -> JsonArray(listOf(rules.normalizedRule(0)))
            else -> rules
        }
    return JsonObject(result)
}

/** Rule keys only need to be unique inside the group; a stable index keeps them so. */
private fun JsonObject.normalizedRule(index: Int): JsonObject {
    val result = toMutableMap()
    if (result["key"].asIntOrNull() == null) result["key"] = JsonPrimitive(index)
    return JsonObject(result)
}

private fun kotlinx.serialization.json.JsonElement?.asIntOrNull(): Int? =
    (this as? JsonPrimitive)?.contentOrNull?.toIntOrNull()

private fun kotlinx.serialization.json.JsonElement?.asTextOrNull(): String? =
    (this as? JsonPrimitive)?.contentOrNull

/**
 * Asks the model for one rule group for this snapshot and validates it with the same parser and
 * selector compiler the app uses, so the caller only ever sees executable rules.
 *
 * [onDelta] receives the raw text as it streams in so the caller can show progress.
 */
suspend fun generateRuleGroup(
    connection: AiConnection,
    snapshot: ComplexSnapshot,
    goal: String,
    onDelta: (String) -> Unit = {},
): AiRuleResult {
    val raw =
        AiClient.streamComplete(
            connection = connection,
            prompt = snapshot.toRulePrompt(goal),
            systemPrompt = AiRuleSystemPrompt,
            onDelta = onDelta,
        )
    return raw.toRuleResult()
}

/** Parses and compiles model output; separated so the streaming path stays transport-only. */
suspend fun String.toRuleResult(): AiRuleResult {
    val text = stripToJson()
    val group =
        try {
            val element = Json5.parseToJsonElement(text) as? JsonObject
                ?: return AiRuleResult.Invalid("Not a JSON object")
            val normalized = element.unwrapGroup().normalizedGroup()
            if ((normalized["rules"] as? JsonArray)?.isEmpty() != false) {
                return AiRuleResult.Invalid("The rule group has no rules")
            }
            RawSubscriptionParser.parseAppGroup(normalized)
        } catch (e: Exception) {
            return AiRuleResult.Invalid(e.message ?: e.javaClass.simpleName)
        }
    val error = RuleGroupCompilation.compile(group).error
    if (error != null) return AiRuleResult.Invalid(error.message.resolve())
    return AiRuleResult.Success(group, SubscriptionJson.toJson5String(group))
}
