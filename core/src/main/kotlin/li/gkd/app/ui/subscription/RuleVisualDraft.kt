package li.gkd.app.ui.subscription

import li.gkd.app.R

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject
import li.gkd.app.model.Option
import li.gkd.app.resources.*
import li.gkd.app.subscription.RawSubscription
import li.gkd.app.subscription.SubscriptionJson
import li.gkd.app.subscription.SubscriptionJson.toJson5String
import li.songe.json5.Json5
import androidx.annotation.StringRes

/** 表单里每个字段的原始文本；空串表示「不设置」，该属性不会写进 JSON。 */
@Serializable
data class VisualValues(val entries: Map<String, String> = emptyMap()) {
    operator fun get(key: String): String = entries[key].orEmpty()

    fun with(key: String, value: String): VisualValues =
        copy(entries = entries + (key to value))

    fun text(key: String): String? = get(key).trim().takeIf { it.isNotEmpty() }

    /** 每行一项；选择器与链接可能含逗号，因此只按换行拆分。 */
    fun lines(key: String): List<String> = get(key)
        .split('\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    /** 逗号或换行分隔的整数。 */
    fun tokens(key: String): List<String> = get(key)
        .split('\n', ',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    fun long(key: String): Long? = text(key)?.toLongOrNull()

    fun bool(key: String): Boolean? = when (text(key)) {
        "true" -> true
        "false" -> false
        else -> null
    }
}

@Serializable
data class VisualRuleDraft(
    val index: Int,
    val values: VisualValues = VisualValues(),
)

@Serializable
data class VisualRuleGroupDraft(
    val values: VisualValues = VisualValues(),
    val rules: List<VisualRuleDraft> = listOf(newRule(0)),
) {
    companion object {
        /** 新规则行，预置运行时的默认动作。 */
        fun newRule(index: Int) = VisualRuleDraft(
            index = index,
            values = VisualValues(mapOf("action" to RuleVisualAction.ClickNode.value)),
        )
    }
}

/** 正在可视化编辑的规则组类型。 */
enum class VisualTarget { AppGroup, GlobalGroup, NewApp }

/** 动作取值，与运行时接受的字符串一致。 */
enum class RuleVisualAction(
    override val value: String,
    override val labelResource: Int,
    val descriptionResource: Int,
) : Option<String> {
    ClickNode(
        "clickNode",
        R.string.rule_visual_action_click_node,
        R.string.rule_help_a_click_node,
    ),
    ClickCenter(
        "clickCenter",
        R.string.rule_visual_action_click_center,
        R.string.rule_help_a_click_center,
    ),
    Click("click", R.string.rule_visual_action_click, R.string.rule_help_a_click),
    LongClickNode(
        "longClickNode",
        R.string.rule_visual_action_long_click_node,
        R.string.rule_help_a_long_click_node,
    ),
    LongClickCenter(
        "longClickCenter",
        R.string.rule_visual_action_long_click_center,
        R.string.rule_help_a_long_click_center,
    ),
    LongClick(
        "longClick",
        R.string.rule_visual_action_long_click,
        R.string.rule_help_a_long_click,
    ),
    Back("back", R.string.rule_visual_action_back, R.string.rule_help_a_back),
    None("none", R.string.rule_visual_action_none, R.string.rule_help_a_none),
    Swipe("swipe", R.string.rule_visual_action_swipe, R.string.rule_help_a_swipe);

    override val options get() = entries
}

/** `resetMatch` 取值；空值为「不设置」。 */
enum class RuleVisualResetMatch(
    override val value: String,
    override val labelResource: Int,
) : Option<String> {
    Unset("", R.string.rule_visual_unset),
    Activity("activity", R.string.rule_visual_reset_activity),
    Match("match", R.string.rule_visual_reset_match),
    App("app", R.string.rule_visual_reset_app);

    override val options get() = entries
}

/**
 * 可视化草稿与 JSON5 的互转。字段清单来自 [groupFieldsFor] / [ruleFieldsFor] 返回的 schema，
 * 控件类型决定读写方式，因此新增属性只需在 schema 登记。
 */
object RuleVisualDraft {
    /** 新建应用时顶层的应用 id，不属于任何规则组。 */
    const val AppId = "id"

    // 嵌套控件的表单键。
    fun positionKey(name: String) = "position.$name"
    fun swipeStartKey(name: String) = "swipe.start.$name"
    fun swipeEndKey(name: String) = "swipe.end.$name"
    fun versionCodeKey(name: String) = "versionCode.$name"
    fun versionNameKey(name: String) = "versionName.$name"
    const val SwipeDuration = "swipe.duration"

    /** 生成 JSON5；必填项未填时返回 null。 */
    fun toJson5(draft: VisualRuleGroupDraft, target: VisualTarget): String? {
        val values = draft.values
        if (values.text("name") == null) return null
        if (target == VisualTarget.NewApp && values.text(AppId) == null) return null
        // 只要填过任意字段就纳入预览：新增的规则还没写选择器，
        // 若按 matches 过滤会整条消失，看不到刚添加的内容。
        val rules = draft.rules.filter { rule ->
            rule.values.entries.any { (_, value) -> value.isNotBlank() }
        }
        if (rules.isEmpty()) return null
        val group = groupJson(values, rules, target)
        val root = if (target == VisualTarget.NewApp) {
            buildJsonObject {
                put(AppId, JsonPrimitive(values.text(AppId)!!))
                put("groups", JsonArray(listOf(group)))
            }
        } else {
            group
        }
        return toJson5String(root)
    }

    private fun groupJson(
        values: VisualValues,
        rules: List<VisualRuleDraft>,
        target: VisualTarget,
    ): JsonObject = buildJsonObject {
        // 字段顺序完全跟随 schema，schema 顺序即 data class 的声明顺序。
        groupFieldsFor(target).forEach { putField(values, it) }
        put("rules", JsonArray(rules.map { ruleJson(it.values, target) }))
    }

    private fun ruleJson(values: VisualValues, target: VisualTarget): JsonObject =
        buildJsonObject {
            ruleFieldsFor(target).forEach { putField(values, it) }
        }

    /** 按控件类型把表单值写入 JSON。 */
    private fun JsonObjectBuilder.putField(values: VisualValues, field: VisualField) {
        val name = field.name
        when (field.control) {
            VisualControl.Bool -> values.bool(name)?.let { put(name, JsonPrimitive(it)) }
            VisualControl.Int, VisualControl.Long ->
                values.long(name)?.let { put(name, JsonPrimitive(it)) }

            VisualControl.Text, VisualControl.Action, VisualControl.ResetMatch ->
                values.text(name)?.let { put(name, JsonPrimitive(it)) }

            VisualControl.TextList -> putTexts(values, name)
            VisualControl.IntList -> putInts(values, name)
            VisualControl.Position -> putPosition(values)
            VisualControl.SwipeArg -> putSwipe(values)
            VisualControl.VersionCode -> putVersionCode(values)
            VisualControl.VersionName -> putVersionName(values)
            VisualControl.Apps -> putApps(values, name)
            // 规则列表由 groupJson 单独写入，这里不处理。
            VisualControl.Rules -> Unit
        }
    }

    private fun JsonObjectBuilder.putTexts(values: VisualValues, key: String) {
        val items = values.lines(key)
        if (items.isEmpty()) return
        // 单值保持标量，与订阅里常见的写法一致。
        put(key, if (items.size == 1) JsonPrimitive(items.first())
        else JsonArray(items.map { JsonPrimitive(it) }))
    }

    private fun JsonObjectBuilder.putInts(values: VisualValues, key: String) {
        val items = values.tokens(key).mapNotNull { it.toIntOrNull() }
        if (items.isEmpty()) return
        put(key, JsonArray(items.map { JsonPrimitive(it) }))
    }

    /**
     * 每行一个应用。仅 id 时写成 `{id}`；`id:false` 表示禁用；
     * 行首为 `{` 时按完整 JSON5 对象解析，以保留页面与版本条件。
     */
    private fun JsonObjectBuilder.putApps(values: VisualValues, key: String) {
        val apps = values.lines(key).mapNotNull { line ->
            if (line.startsWith("{")) {
                runCatching { Json5.parseToJsonElement(line) }.getOrNull() as? JsonObject
            } else {
                val id = line.substringBefore(':').trim()
                if (id.isEmpty()) return@mapNotNull null
                val disabled = line.substringAfter(':', "").trim().equals("false", true)
                buildJsonObject {
                    put("id", JsonPrimitive(id))
                    if (disabled) put("enable", JsonPrimitive(false))
                }
            }
        }
        if (apps.isEmpty()) return
        put(key, JsonArray(apps))
    }

    private fun JsonObjectBuilder.putVersionCode(values: VisualValues) {
        val minimum = values.long(versionCodeKey("minimum"))
        val maximum = values.long(versionCodeKey("maximum"))
        val include = values.tokens(versionCodeKey("include")).mapNotNull { it.toIntOrNull() }
        val exclude = values.tokens(versionCodeKey("exclude")).mapNotNull { it.toIntOrNull() }
        if (minimum == null && maximum == null && include.isEmpty() && exclude.isEmpty()) return
        putJsonObject("versionCode") {
            minimum?.let { put("minimum", JsonPrimitive(it)) }
            maximum?.let { put("maximum", JsonPrimitive(it)) }
            if (include.isNotEmpty()) put("include", JsonArray(include.map { JsonPrimitive(it) }))
            if (exclude.isNotEmpty()) put("exclude", JsonArray(exclude.map { JsonPrimitive(it) }))
        }
    }

    private fun JsonObjectBuilder.putVersionName(values: VisualValues) {
        val pattern = values.text(versionNameKey("pattern"))
        val include = values.lines(versionNameKey("include"))
        val exclude = values.lines(versionNameKey("exclude"))
        if (pattern == null && include.isEmpty() && exclude.isEmpty()) return
        putJsonObject("versionName") {
            pattern?.let { put("pattern", JsonPrimitive(it)) }
            if (include.isNotEmpty()) put("include", JsonArray(include.map { JsonPrimitive(it) }))
            if (exclude.isNotEmpty()) put("exclude", JsonArray(exclude.map { JsonPrimitive(it) }))
        }
    }

    private fun JsonObjectBuilder.putPosition(values: VisualValues) {
        val entries = VisualSubFields.position.mapNotNull { name ->
            values.text(positionKey(name))?.let { name to it }
        }
        if (entries.isEmpty()) return
        putJsonObject("position") {
            entries.forEach { (name, value) -> put(name, JsonPrimitive(value)) }
        }
    }

    private fun JsonObjectBuilder.putSwipe(values: VisualValues) {
        val duration = values.long(SwipeDuration)
        val start = subPosition(values, ::swipeStartKey)
        val end = subPosition(values, ::swipeEndKey)
        if (duration == null && start.isEmpty() && end.isEmpty()) return
        putJsonObject("swipeArg") {
            if (start.isNotEmpty()) {
                putJsonObject("start") {
                    start.forEach { (name, value) -> put(name, JsonPrimitive(value)) }
                }
            }
            if (end.isNotEmpty()) {
                putJsonObject("end") {
                    end.forEach { (name, value) -> put(name, JsonPrimitive(value)) }
                }
            }
            duration?.let { put("duration", JsonPrimitive(it)) }
        }
    }

    private fun subPosition(
        values: VisualValues,
        keyOf: (String) -> String,
    ): List<Pair<String, String>> = VisualSubFields.position.mapNotNull { name ->
        values.text(keyOf(name))?.let { name to it }
    }

    /** 用已有规则组回填草稿，便于从可视化界面继续编辑。 */
    fun fromGroup(group: RawSubscription.RawGroupProps): VisualRuleGroupDraft {
        val target = if (group is RawSubscription.RawGlobalGroup) {
            VisualTarget.GlobalGroup
        } else {
            VisualTarget.AppGroup
        }
        return VisualRuleGroupDraft(
            values = VisualValues(groupEntries(group, target)),
            rules = group.rules
                .mapIndexed { index, rule ->
                    VisualRuleDraft(index, VisualValues(ruleEntries(rule, target)))
                }
                .ifEmpty { listOf(VisualRuleGroupDraft.newRule(0)) },
        )
    }

    private fun groupEntries(
        group: RawSubscription.RawGroupProps,
        target: VisualTarget,
    ): Map<String, String> = buildMap {
        groupFieldsFor(target).forEach { field ->
            when (field.control) {
                VisualControl.VersionCode -> (group as? RawSubscription.RawAppGroup)
                    ?.versionCode?.let { matcher ->
                        matcher.minimum?.let { put(versionCodeKey("minimum"), it.toString()) }
                        matcher.maximum?.let { put(versionCodeKey("maximum"), it.toString()) }
                        matcher.include?.ifEmpty { null }
                            ?.let { put(versionCodeKey("include"), it.joinToString("\n")) }
                        matcher.exclude?.ifEmpty { null }
                            ?.let { put(versionCodeKey("exclude"), it.joinToString("\n")) }
                    }

                VisualControl.VersionName -> (group as? RawSubscription.RawAppGroup)
                    ?.versionName?.let { matcher ->
                        matcher.pattern?.let { put(versionNameKey("pattern"), it) }
                        matcher.include?.ifEmpty { null }
                            ?.let { put(versionNameKey("include"), it.joinToString("\n")) }
                        matcher.exclude?.ifEmpty { null }
                            ?.let { put(versionNameKey("exclude"), it.joinToString("\n")) }
                    }

                VisualControl.Apps -> (group as? RawSubscription.RawGlobalGroup)
                    ?.apps?.ifEmpty { null }
                    ?.let { apps ->
                        put(field.name, apps.joinToString("\n") { toAppLine(it) })
                    }

                else -> group.readScalar(field.name)?.let { put(field.name, it) }
            }
        }
    }

    private fun ruleEntries(
        rule: RawSubscription.RawRuleProps,
        target: VisualTarget,
    ): Map<String, String> = buildMap {
        ruleFieldsFor(target).forEach { field ->
            when (field.control) {
                VisualControl.Position -> rule.position?.let { position ->
                    VisualSubFields.position.forEach { name ->
                        position.valueByName(name)
                            ?.let { put(positionKey(name), it) }
                    }
                }

                VisualControl.SwipeArg -> rule.swipeArg?.let { swipe ->
                    VisualSubFields.position.forEach { name ->
                        swipe.start.valueByName(name)?.let { put(swipeStartKey(name), it) }
                    }
                    swipe.end?.let { end ->
                        VisualSubFields.position.forEach { name ->
                            end.valueByName(name)?.let { put(swipeEndKey(name), it) }
                        }
                    }
                    put(SwipeDuration, swipe.duration.toString())
                }

                VisualControl.VersionCode -> (rule as? RawSubscription.RawAppRule)
                    ?.versionCode?.let { matcher ->
                        matcher.minimum?.let { put(versionCodeKey("minimum"), it.toString()) }
                        matcher.maximum?.let { put(versionCodeKey("maximum"), it.toString()) }
                        matcher.include?.ifEmpty { null }
                            ?.let { put(versionCodeKey("include"), it.joinToString("\n")) }
                        matcher.exclude?.ifEmpty { null }
                            ?.let { put(versionCodeKey("exclude"), it.joinToString("\n")) }
                    }

                VisualControl.VersionName -> (rule as? RawSubscription.RawAppRule)
                    ?.versionName?.let { matcher ->
                        matcher.pattern?.let { put(versionNameKey("pattern"), it) }
                        matcher.include?.ifEmpty { null }
                            ?.let { put(versionNameKey("include"), it.joinToString("\n")) }
                        matcher.exclude?.ifEmpty { null }
                            ?.let { put(versionNameKey("exclude"), it.joinToString("\n")) }
                    }

                VisualControl.Apps -> (rule as? RawSubscription.RawGlobalRule)
                    ?.apps?.ifEmpty { null }
                    ?.let { apps ->
                        put(field.name, apps.joinToString("\n") { toAppLine(it) })
                    }

                else -> rule.readScalar(field.name)?.let { put(field.name, it) }
            }
        }
    }

    private fun toAppLine(app: RawSubscription.RawGlobalApp): String =
        SubscriptionJson.json.encodeToString(app)

    private fun toAppLine(app: RawSubscription.RawGlobalRuleApp): String =
        SubscriptionJson.json.encodeToString(app)

    /** 标量字段直接从 data class 读取；嵌套控件在各自分支处理。 */
    private fun RawSubscription.RawGroupProps.readScalar(fieldName: String): String? =
        when (fieldName) {
            "key" -> key.toString()
            "name" -> name
            "desc" -> desc
            "enable" -> enable?.toString()
            "scopeKeys" -> scopeKeys?.joinToString("\n")
            "actionCdKey" -> actionCdKey?.toString()
            "actionMaximumKey" -> actionMaximumKey?.toString()
            "actionCd" -> actionCd?.toString()
            "actionDelay" -> actionDelay?.toString()
            "fastQuery" -> fastQuery?.toString()
            "matchRoot" -> matchRoot?.toString()
            "actionMaximum" -> actionMaximum?.toString()
            "priorityTime" -> priorityTime?.toString()
            "priorityActionMaximum" -> priorityActionMaximum?.toString()
            "order" -> order?.toString()
            "forcedTime" -> forcedTime?.toString()
            "matchDelay" -> matchDelay?.toString()
            "matchTime" -> matchTime?.toString()
            "resetMatch" -> resetMatch
            "snapshotUrls" -> snapshotUrls?.joinToString("\n")
            "excludeSnapshotUrls" -> excludeSnapshotUrls?.joinToString("\n")
            "exampleUrls" -> exampleUrls?.joinToString("\n")
            "activityIds" -> (this as? RawSubscription.RawAppGroup)?.activityIds?.joinToString("\n")
            "excludeActivityIds" ->
                (this as? RawSubscription.RawAppGroup)?.excludeActivityIds?.joinToString("\n")

            "ignoreGlobalGroupMatch" ->
                (this as? RawSubscription.RawAppGroup)?.ignoreGlobalGroupMatch?.toString()

            "matchAnyApp" -> (this as? RawSubscription.RawGlobalGroup)?.matchAnyApp?.toString()
            "matchSystemApp" -> (this as? RawSubscription.RawGlobalGroup)?.matchSystemApp?.toString()
            "matchLauncher" -> (this as? RawSubscription.RawGlobalGroup)?.matchLauncher?.toString()
            "disableIfAppGroupMatch" ->
                (this as? RawSubscription.RawGlobalGroup)?.disableIfAppGroupMatch

            else -> null
        }

    private fun RawSubscription.RawRuleProps.readScalar(fieldName: String): String? =
        when (fieldName) {
            "key" -> key?.toString()
            "name" -> name
            "preKeys" -> preKeys?.joinToString("\n")
            "action" -> action
            "matches" -> matches?.joinToString("\n")
            "excludeMatches" -> excludeMatches?.joinToString("\n")
            "excludeAllMatches" -> excludeAllMatches?.joinToString("\n")
            "anyMatches" -> anyMatches?.joinToString("\n")
            "actionCdKey" -> actionCdKey?.toString()
            "actionMaximumKey" -> actionMaximumKey?.toString()
            "actionCd" -> actionCd?.toString()
            "actionDelay" -> actionDelay?.toString()
            "fastQuery" -> fastQuery?.toString()
            "matchRoot" -> matchRoot?.toString()
            "actionMaximum" -> actionMaximum?.toString()
            "priorityTime" -> priorityTime?.toString()
            "priorityActionMaximum" -> priorityActionMaximum?.toString()
            "order" -> order?.toString()
            "forcedTime" -> forcedTime?.toString()
            "matchDelay" -> matchDelay?.toString()
            "matchTime" -> matchTime?.toString()
            "resetMatch" -> resetMatch
            "snapshotUrls" -> snapshotUrls?.joinToString("\n")
            "excludeSnapshotUrls" -> excludeSnapshotUrls?.joinToString("\n")
            "exampleUrls" -> exampleUrls?.joinToString("\n")
            "activityIds" -> (this as? RawSubscription.RawAppRule)?.activityIds?.joinToString("\n")
            "excludeActivityIds" ->
                (this as? RawSubscription.RawAppRule)?.excludeActivityIds?.joinToString("\n")

            else -> null
        }

    private fun RawSubscription.Position.valueByName(name: String): String? = when (name) {
        "left" -> left
        "top" -> top
        "right" -> right
        "bottom" -> bottom
        "x" -> x
        "y" -> y
        else -> null
    }
}
