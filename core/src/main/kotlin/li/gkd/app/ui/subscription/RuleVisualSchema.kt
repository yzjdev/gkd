package li.gkd.app.ui.subscription

import li.gkd.app.R

import li.gkd.app.resources.*
import androidx.annotation.StringRes

/**
 * 可视化表单的字段表，与 [li.gkd.app.subscription.RawSubscription] 的 data class 一一对应。
 *
 * 每个属性只在此声明一次（[groupFieldDefs] / [ruleFieldDefs]），
 * 各 data class 的字段顺序由下面的名字列表决定，因此不会出现重复定义。
 * 控件由属性类型决定：
 *
 * ```
 * Boolean?        -> Bool        开关
 * Int?            -> Int         整数输入
 * Long?           -> Long        毫秒输入
 * String?         -> Text        文本输入
 * List<String>?   -> TextList    每行一项
 * List<Int>?      -> IntList     逗号或换行分隔
 * action          -> Action      动作下拉
 * resetMatch      -> ResetMatch  重置时机下拉
 * Position?       -> Position    left/top/right/bottom/x/y
 * SwipeArg?       -> SwipeArg    start/end 各六个子字段 + duration
 * IntegerMatcher? -> VersionCode minimum/maximum/include/exclude
 * StringMatcher?  -> VersionName pattern/include/exclude
 * apps            -> Apps        应用对象列表
 * rules           -> Rules       规则列表，由规则页承载
 * ```
 */
enum class VisualControl {
    Bool,
    Int,
    Long,
    Text,
    TextList,
    IntList,
    Action,
    ResetMatch,
    Position,
    SwipeArg,
    VersionCode,
    VersionName,
    Apps,
    Rules;

    /** 说明页「类型」列的描述。 */
    val typeResource: Int
        get() = when (this) {
            Bool -> R.string.rule_help_type_boolean
            Int -> R.string.rule_help_type_integer
            Long -> R.string.rule_help_type_duration
            Text -> R.string.rule_help_type_string
            TextList -> R.string.rule_help_type_string_or_list
            IntList -> R.string.rule_help_type_integer_list
            Action -> R.string.rule_help_type_action
            ResetMatch -> R.string.rule_help_type_reset_match
            Position -> R.string.rule_help_type_position
            SwipeArg -> R.string.rule_help_type_swipe_arg
            VersionCode -> R.string.rule_help_type_version_code
            VersionName -> R.string.rule_help_type_version_name
            Apps -> R.string.rule_help_type_apps
            Rules -> R.string.rule_help_type_rules
        }
}

/** 规则组属性按用途分区，每个分区在界面上是一页；`Rules` 由规则页单独承载。 */
enum class VisualSection { Identity, Target, Timing, Links, Rules }

/**
 * 页内子分组：同组字段折叠进一个手风琴。null 表示平铺在分区里（基础字段不折叠）。
 */
enum class VisualSubGroup(@StringRes val title: Int) {
    /** 执行控制：冷却、延迟、次数等频率类属性。 */
    ActionTiming(R.string.rule_visual_sub_action_timing),

    /** 应用与活动范围：目标应用、界面、版本等范围属性。 */
    AppScope(R.string.rule_visual_sub_app_scope),

    /** 参考链接：快照与示例地址。 */
    RefLinks(R.string.rule_visual_sub_ref_links),
}

/** 按属性名归入子分组；未登记的属性平铺展示。 */
private val fieldSubGroups: Map<String, VisualSubGroup> = mapOf(
    // 执行控制
    "actionCd" to VisualSubGroup.ActionTiming,
    "actionDelay" to VisualSubGroup.ActionTiming,
    "fastQuery" to VisualSubGroup.ActionTiming,
    "matchRoot" to VisualSubGroup.ActionTiming,
    "matchDelay" to VisualSubGroup.ActionTiming,
    "matchTime" to VisualSubGroup.ActionTiming,
    "actionMaximum" to VisualSubGroup.ActionTiming,
    "resetMatch" to VisualSubGroup.ActionTiming,
    "actionCdKey" to VisualSubGroup.ActionTiming,
    "actionMaximumKey" to VisualSubGroup.ActionTiming,
    "priorityTime" to VisualSubGroup.ActionTiming,
    "priorityActionMaximum" to VisualSubGroup.ActionTiming,
    "forcedTime" to VisualSubGroup.ActionTiming,
    // 应用与活动范围
    "activityIds" to VisualSubGroup.AppScope,
    "excludeActivityIds" to VisualSubGroup.AppScope,
    "apps" to VisualSubGroup.AppScope,
    "matchAnyApp" to VisualSubGroup.AppScope,
    "matchSystemApp" to VisualSubGroup.AppScope,
    "matchLauncher" to VisualSubGroup.AppScope,
    "ignoreGlobalGroupMatch" to VisualSubGroup.AppScope,
    "disableIfAppGroupMatch" to VisualSubGroup.AppScope,
    // 参考链接
    "snapshotUrls" to VisualSubGroup.RefLinks,
    "excludeSnapshotUrls" to VisualSubGroup.RefLinks,
    "exampleUrls" to VisualSubGroup.RefLinks,
)

/**
 * 一个属性的表单描述。规则组的字段写在 `draft.values`，每条规则的字段写在
 * 各自的 `VisualRuleDraft.values`，因此这里不需要任何作用域前缀。
 *
 * `overridesGroup` 表示同名字段在规则组层也有一份：规则级填了即覆盖，留空则继承。
 */
data class VisualField(
    val name: String,
    val control: VisualControl,
    val description: Int,
    val required: Boolean = false,
    val section: VisualSection = VisualSection.Identity,
    val overridesGroup: Boolean = false,
) {
    /** 字段归属的手风琴子分组；null 表示平铺在分区里。 */
    val subGroup: VisualSubGroup? get() = fieldSubGroups[name]
}

/** 把字段列表按子分组拆分为「平铺字段 + 各子分组」的渲染顺序。 */
data class VisualFieldGrouping(
    val plain: List<VisualField>,
    val grouped: List<Pair<VisualSubGroup, List<VisualField>>>,
)

fun List<VisualField>.groupBySubGroup(): VisualFieldGrouping {
    val plain = filter { it.subGroup == null }
    val grouped = filter { it.subGroup != null }
        .groupBy { it.subGroup!! }
        // 固定子分组出现顺序，避免依赖字段登记顺序。
        .let { grouped -> VisualSubGroup.entries.mapNotNull { g -> grouped[g]?.let { g to it } } }
        .filter { it.second.isNotEmpty() }
    return VisualFieldGrouping(plain, grouped)
}

/** 组级属性的唯一定义，`RawCommonProps` + `RawGroupProps` 的全部属性。 */
private val groupFieldDefs: Map<String, VisualField> = listOf(
    VisualField("key", VisualControl.Int, R.string.rule_help_p_group_key),
    VisualField("name", VisualControl.Text, R.string.rule_help_p_group_name, required = true),
    VisualField("desc", VisualControl.Text, R.string.rule_help_p_desc),
    VisualField("enable", VisualControl.Bool, R.string.rule_help_p_enable),
    VisualField("scopeKeys", VisualControl.IntList, R.string.rule_help_p_scope_keys),
    VisualField(
        "actionCd",
        VisualControl.Long,
        R.string.rule_help_p_action_cd,
        section = VisualSection.Timing,
    ),
    VisualField(
        "actionDelay",
        VisualControl.Long,
        R.string.rule_help_p_action_delay,
        section = VisualSection.Timing,
    ),
    VisualField(
        "fastQuery",
        VisualControl.Bool,
        R.string.rule_help_p_fast_query,
        section = VisualSection.Timing,
    ),
    VisualField(
        "matchRoot",
        VisualControl.Bool,
        R.string.rule_help_p_match_root,
        section = VisualSection.Timing,
    ),
    VisualField(
        "matchDelay",
        VisualControl.Long,
        R.string.rule_help_p_match_delay,
        section = VisualSection.Timing,
    ),
    VisualField(
        "matchTime",
        VisualControl.Long,
        R.string.rule_help_p_match_time,
        section = VisualSection.Timing,
    ),
    VisualField(
        "actionMaximum",
        VisualControl.Int,
        R.string.rule_help_p_action_maximum,
        section = VisualSection.Timing,
    ),
    VisualField(
        "resetMatch",
        VisualControl.ResetMatch,
        R.string.rule_help_p_reset_match,
        section = VisualSection.Timing,
    ),
    VisualField(
        "actionCdKey",
        VisualControl.Int,
        R.string.rule_help_p_action_cd_key,
        section = VisualSection.Timing,
    ),
    VisualField(
        "actionMaximumKey",
        VisualControl.Int,
        R.string.rule_help_p_action_maximum_key,
        section = VisualSection.Timing,
    ),
    VisualField("order", VisualControl.Int, R.string.rule_help_p_order),
    VisualField(
        "forcedTime",
        VisualControl.Long,
        R.string.rule_help_p_forced_time,
        section = VisualSection.Timing,
    ),
    VisualField(
        "snapshotUrls",
        VisualControl.TextList,
        R.string.rule_help_p_snapshot_urls,
        section = VisualSection.Links,
    ),
    VisualField(
        "excludeSnapshotUrls",
        VisualControl.TextList,
        R.string.rule_help_p_exclude_snapshot_urls,
        section = VisualSection.Links,
    ),
    VisualField(
        "exampleUrls",
        VisualControl.TextList,
        R.string.rule_help_p_example_urls,
        section = VisualSection.Links,
    ),
    VisualField(
        "priorityTime",
        VisualControl.Long,
        R.string.rule_help_p_priority_time,
        section = VisualSection.Timing,
    ),
    VisualField(
        "priorityActionMaximum",
        VisualControl.Int,
        R.string.rule_help_p_priority_action_maximum,
        section = VisualSection.Timing,
    ),
    VisualField(
        "activityIds",
        VisualControl.TextList,
        R.string.rule_help_p_activity_ids,
        section = VisualSection.Target,
    ),
    VisualField(
        "excludeActivityIds",
        VisualControl.TextList,
        R.string.rule_help_p_exclude_activity_ids,
        section = VisualSection.Target,
    ),
    VisualField(
        "versionCode",
        VisualControl.VersionCode,
        R.string.rule_help_p_version_code,
        section = VisualSection.Target,
    ),
    VisualField(
        "versionName",
        VisualControl.VersionName,
        R.string.rule_help_p_version_name,
        section = VisualSection.Target,
    ),
    VisualField(
        "ignoreGlobalGroupMatch",
        VisualControl.Bool,
        R.string.rule_help_p_ignore_global_group_match,
        section = VisualSection.Target,
    ),
    VisualField(
        "matchAnyApp",
        VisualControl.Bool,
        R.string.rule_help_p_match_any_app,
        section = VisualSection.Target,
    ),
    VisualField(
        "matchSystemApp",
        VisualControl.Bool,
        R.string.rule_help_p_match_system_app,
        section = VisualSection.Target,
    ),
    VisualField(
        "matchLauncher",
        VisualControl.Bool,
        R.string.rule_help_p_match_launcher,
        section = VisualSection.Target,
    ),
    VisualField(
        "disableIfAppGroupMatch",
        VisualControl.Text,
        R.string.rule_help_p_disable_if_app_group_match,
        section = VisualSection.Target,
    ),
    VisualField(
        "apps",
        VisualControl.Apps,
        R.string.rule_visual_apps_hint,
        section = VisualSection.Target,
    ),
    VisualField("rules", VisualControl.Rules, R.string.rule_help_p_rules, section = VisualSection.Rules),
).associateBy { it.name }

/** 规则级属性的唯一定义，`RawCommonProps` + `RawRuleProps` + `LocationProps` 的全部属性。 */
private val ruleFieldDefs: Map<String, VisualField> = listOf(
    VisualField("key", VisualControl.Int, R.string.rule_help_p_key),
    VisualField("name", VisualControl.Text, R.string.rule_help_p_name),
    VisualField("preKeys", VisualControl.IntList, R.string.rule_help_p_pre_keys),
    VisualField("action", VisualControl.Action, R.string.rule_help_p_action),
    VisualField("position", VisualControl.Position, R.string.rule_help_p_position),
    VisualField("swipeArg", VisualControl.SwipeArg, R.string.rule_help_p_swipe_arg),
    VisualField("matches", VisualControl.TextList, R.string.rule_help_p_matches, required = true),
    VisualField("excludeMatches", VisualControl.TextList, R.string.rule_help_p_exclude_matches),
    VisualField(
        "excludeAllMatches",
        VisualControl.TextList,
        R.string.rule_help_p_exclude_all_matches,
    ),
    VisualField("anyMatches", VisualControl.TextList, R.string.rule_help_p_any_matches),
    VisualField("actionCd", VisualControl.Long, R.string.rule_help_p_action_cd, overridesGroup = true),
    VisualField(
        "actionDelay",
        VisualControl.Long,
        R.string.rule_help_p_action_delay,
        overridesGroup = true,
    ),
    VisualField("fastQuery", VisualControl.Bool, R.string.rule_help_p_fast_query, overridesGroup = true),
    VisualField("matchRoot", VisualControl.Bool, R.string.rule_help_p_match_root, overridesGroup = true),
    VisualField(
        "matchDelay",
        VisualControl.Long,
        R.string.rule_help_p_match_delay,
        overridesGroup = true,
    ),
    VisualField("matchTime", VisualControl.Long, R.string.rule_help_p_match_time, overridesGroup = true),
    VisualField(
        "actionMaximum",
        VisualControl.Int,
        R.string.rule_help_p_action_maximum,
        overridesGroup = true,
    ),
    VisualField(
        "resetMatch",
        VisualControl.ResetMatch,
        R.string.rule_help_p_reset_match,
        overridesGroup = true,
    ),
    VisualField(
        "actionCdKey",
        VisualControl.Int,
        R.string.rule_help_p_action_cd_key,
        overridesGroup = true,
    ),
    VisualField(
        "actionMaximumKey",
        VisualControl.Int,
        R.string.rule_help_p_action_maximum_key,
        overridesGroup = true,
    ),
    VisualField("priorityTime", VisualControl.Long, R.string.rule_help_p_priority_time, overridesGroup = true),
    VisualField(
        "priorityActionMaximum",
        VisualControl.Int,
        R.string.rule_help_p_priority_action_maximum,
        overridesGroup = true,
    ),
    VisualField("order", VisualControl.Int, R.string.rule_help_p_order, overridesGroup = true),
    VisualField("forcedTime", VisualControl.Long, R.string.rule_help_p_forced_time, overridesGroup = true),
    VisualField(
        "snapshotUrls",
        VisualControl.TextList,
        R.string.rule_help_p_snapshot_urls,
        overridesGroup = true,
    ),
    VisualField(
        "excludeSnapshotUrls",
        VisualControl.TextList,
        R.string.rule_help_p_exclude_snapshot_urls,
        overridesGroup = true,
    ),
    VisualField(
        "exampleUrls",
        VisualControl.TextList,
        R.string.rule_help_p_example_urls,
        overridesGroup = true,
    ),
    VisualField(
        "activityIds",
        VisualControl.TextList,
        R.string.rule_help_p_activity_ids,
        overridesGroup = true,
    ),
    VisualField(
        "excludeActivityIds",
        VisualControl.TextList,
        R.string.rule_help_p_exclude_activity_ids,
        overridesGroup = true,
    ),
    VisualField(
        "versionCode",
        VisualControl.VersionCode,
        R.string.rule_help_p_version_code,
        overridesGroup = true,
    ),
    VisualField(
        "versionName",
        VisualControl.VersionName,
        R.string.rule_help_p_version_name,
        overridesGroup = true,
    ),
    VisualField("apps", VisualControl.Apps, R.string.rule_visual_apps_hint, overridesGroup = true),
).associateBy { it.name }

private fun Map<String, VisualField>.ordered(names: List<String>): List<VisualField> =
    names.map { getValue(it) }

/** `RawAppGroup` 的字段，顺序与 data class 的声明一致。 */
val appGroupFields: List<VisualField> = groupFieldDefs.ordered(
    listOf(
        "key", "name", "desc", "enable", "scopeKeys",
        "actionCdKey", "actionMaximumKey", "actionCd", "actionDelay",
        "fastQuery", "matchRoot", "actionMaximum", "priorityTime", "priorityActionMaximum",
        "order", "forcedTime", "matchDelay", "matchTime", "resetMatch",
        "snapshotUrls", "excludeSnapshotUrls", "exampleUrls",
        "activityIds", "excludeActivityIds", "rules",
        "versionCode", "versionName", "ignoreGlobalGroupMatch",
    )
)

/** `RawGlobalGroup` 的字段，顺序与 data class 的声明一致。 */
val globalGroupFields: List<VisualField> = groupFieldDefs.ordered(
    listOf(
        "key", "name", "desc", "enable", "scopeKeys",
        "actionCd", "actionDelay", "fastQuery", "matchRoot", "matchDelay", "matchTime",
        "actionMaximum", "resetMatch", "actionCdKey", "actionMaximumKey",
        "priorityTime", "priorityActionMaximum", "order", "forcedTime",
        "snapshotUrls", "excludeSnapshotUrls", "exampleUrls",
        "matchAnyApp", "matchSystemApp", "matchLauncher", "disableIfAppGroupMatch",
        "rules", "apps",
    )
)

/**
 * `RawAppRule` 的字段。展示顺序按语义分组：先选什么（选择器），再做什么（动作），
 * 然后计时与链接，最后是页面与版本条件；与 data class 的声明顺序有意不同。
 */
val appRuleFields: List<VisualField> = ruleFieldDefs.ordered(
    listOf(
        "key", "name", "preKeys",
        "matches", "excludeMatches", "excludeAllMatches", "anyMatches",
        "action", "position", "swipeArg",
        "actionCdKey", "actionMaximumKey", "actionCd", "actionDelay",
        "fastQuery", "matchRoot", "actionMaximum", "priorityTime", "priorityActionMaximum",
        "order", "forcedTime", "matchDelay", "matchTime", "resetMatch",
        "snapshotUrls", "excludeSnapshotUrls", "exampleUrls",
        "activityIds", "excludeActivityIds", "versionCode", "versionName",
    )
)

/** `RawGlobalRule` 的字段，展示顺序与 [appRuleFields] 保持一致。 */
val globalRuleFields: List<VisualField> = ruleFieldDefs.ordered(
    listOf(
        "key", "name", "preKeys",
        "matches", "excludeMatches", "excludeAllMatches", "anyMatches",
        "action", "position", "swipeArg",
        "actionCdKey", "actionMaximumKey", "actionCd", "actionDelay",
        "fastQuery", "matchRoot", "actionMaximum", "priorityTime", "priorityActionMaximum",
        "order", "forcedTime", "matchDelay", "matchTime", "resetMatch",
        "snapshotUrls", "excludeSnapshotUrls", "exampleUrls",
        "apps",
    )
)

/** 某个目标下规则组的全部字段。 */
fun groupFieldsFor(target: VisualTarget): List<VisualField> =
    if (target == VisualTarget.GlobalGroup) globalGroupFields else appGroupFields

/** 某个目标下规则的全部字段。 */
fun ruleFieldsFor(target: VisualTarget): List<VisualField> =
    if (target == VisualTarget.GlobalGroup) globalRuleFields else appRuleFields

/** 嵌套 data class 的子字段名，顺序与声明一致。 */
object VisualSubFields {
    /** `Position`：点击位置与滑动起终点共用。 */
    val position = listOf("left", "top", "right", "bottom", "x", "y")

    /** `IntegerMatcher`。 */
    val versionCode = listOf("minimum", "maximum", "include", "exclude")

    /** `StringMatcher`。 */
    val versionName = listOf("pattern", "include", "exclude")

    /** `SwipeArg.duration` 的 JSON 名。 */
    const val duration = "duration"
}
