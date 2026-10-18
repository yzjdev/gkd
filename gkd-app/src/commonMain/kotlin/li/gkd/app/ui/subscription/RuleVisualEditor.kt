package li.gkd.app.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import li.gkd.app.resources.*
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkOutlinedTextField
import li.gkd.app.ui.component.GkPageBottomSpace
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** 步进页：规则组属性按 schema 分区，规则各占一页，最后是生成结果。 */
private sealed interface VisualStep {
    data class Group(val section: VisualSection) : VisualStep
    data class Rule(val position: Int) : VisualStep
    data object Preview : VisualStep
}

/**
 * 可视化规则编辑器。字段集来自 [groupFieldsFor] / [ruleFieldsFor] 返回的 schema，
 * 控件由属性类型决定，因此与 data class 保持一致。
 */
@Composable
fun RuleVisualEditor(
    draft: VisualRuleGroupDraft,
    target: VisualTarget,
    onDraftChange: (VisualRuleGroupDraft) -> Unit,
    modifier: Modifier = Modifier,
    generated: String? = null,
) {
    val steps = remember(draft.rules.size, target) { buildSteps(draft.rules.size, target) }
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    var pendingPage by remember { mutableStateOf<Int?>(null) }
    val values = draft.values
    val setValue: (String, String) -> Unit = { key, value ->
        onDraftChange(draft.copy(values = values.with(key, value)))
    }
    LaunchedEffect(steps.size, pendingPage) {
        val next = pendingPage
        if (next != null && next in steps.indices) {
            pendingPage = null
            pagerState.animateScrollToPage(next)
        } else if (pagerState.currentPage > steps.lastIndex) {
            pagerState.scrollToPage(steps.lastIndex)
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage.coerceIn(steps.indices),
            edgePadding = 8.dp,
        ) {
            steps.forEachIndexed { index, step ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(stepTitle(step, target), maxLines = 1) },
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            val step = steps[index.coerceIn(steps.indices)]
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.rule_visual_required_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when (step) {
                    is VisualStep.Group -> GroupPage(
                        draft = draft,
                        target = target,
                        section = step.section,
                        onValueChange = setValue,
                    )

                    is VisualStep.Rule -> RulePage(
                        draft = draft,
                        position = step.position,
                        target = target,
                        onDraftChange = onDraftChange,
                        onAddRule = {
                            val nextIndex = (draft.rules.maxOfOrNull { it.index } ?: -1) + 1
                            onDraftChange(
                                draft.copy(
                                    rules = draft.rules + VisualRuleGroupDraft.newRule(nextIndex),
                                )
                            )
                            val nextSteps = buildSteps(draft.rules.size + 1, target)
                            val lastRule = nextSteps.indexOfLast { it is VisualStep.Rule }
                            pendingPage = lastRule
                        },
                        onRemoveRule = {
                            val remaining = draft.rules.filterIndexed { i, _ -> i != step.position }
                            onDraftChange(draft.copy(rules = remaining))
                            val nextSteps = buildSteps(remaining.size, target)
                            val adjacent = step.position.coerceAtMost(remaining.lastIndex)
                            pendingPage = nextSteps.indexOf(VisualStep.Rule(adjacent))
                        },
                    )

                    VisualStep.Preview -> GeneratedPage(generated)
                }
                GkPageBottomSpace()
            }
        }
    }
}

/** 分区顺序固定为「身份 → 作用范围 → 计时 → 链接」，规则紧随其后。 */
private fun buildSteps(ruleCount: Int, target: VisualTarget): List<VisualStep> = buildList {
    val present = groupFieldsFor(target).mapTo(mutableSetOf()) { it.section }
    listOf(VisualSection.Identity, VisualSection.Target, VisualSection.Timing, VisualSection.Links)
        .filter { it in present }
        .forEach { add(VisualStep.Group(it)) }
    repeat(ruleCount) { add(VisualStep.Rule(it)) }
    add(VisualStep.Preview)
}

@Composable
private fun stepTitle(step: VisualStep, target: VisualTarget): String = when (step) {
    is VisualStep.Group -> stringResource(sectionTitle(step.section, target))
    is VisualStep.Rule -> stringResource(Res.string.rule_visual_rule_index, step.position + 1)
    VisualStep.Preview -> stringResource(Res.string.rule_visual_generated)
}

private fun sectionTitle(section: VisualSection, target: VisualTarget): StringResource =
    when (section) {
        VisualSection.Identity -> Res.string.rule_visual_section_group
        VisualSection.Target ->
            if (target == VisualTarget.GlobalGroup) Res.string.rule_help_global_props
            else Res.string.rule_help_app_props

        VisualSection.Timing -> Res.string.rule_visual_section_timing
        VisualSection.Links -> Res.string.rule_visual_section_urls
        VisualSection.Rules -> Res.string.rule_visual_section_rule
    }

/** 规则组某一分区：字段与顺序完全来自 schema。 */
@Composable
private fun GroupPage(
    draft: VisualRuleGroupDraft,
    target: VisualTarget,
    section: VisualSection,
    onValueChange: (String, String) -> Unit,
) {
    val fields = groupFieldsFor(target).filter { it.section == section }
    VisualSection(sectionTitle(section, target)) {
        if (section == VisualSection.Identity && target == VisualTarget.NewApp) {
            // 新建应用时先填应用 id，它属于顶层的 RawApp 而不是规则组。
            VisualText(
                values = draft.values,
                key = RuleVisualDraft.AppId,
                label = RuleVisualDraft.AppId,
                description = Res.string.rule_help_p_apps,
                required = true,
                placeholder = Res.string.rule_visual_app_id_hint,
                onValueChange = { onValueChange(RuleVisualDraft.AppId, it) },
            )
        }
        fields.forEach { field ->
            FieldEditor(draft.values, field, onValueChange)
        }
    }
}

/** 一条规则一页，字段与顺序同样来自 schema。 */
@Composable
private fun RulePage(
    draft: VisualRuleGroupDraft,
    position: Int,
    target: VisualTarget,
    onDraftChange: (VisualRuleGroupDraft) -> Unit,
    onAddRule: () -> Unit,
    onRemoveRule: () -> Unit,
) {
    val rule = draft.rules.getOrNull(position) ?: return
    val setValue: (String, String) -> Unit = { key, value ->
        onDraftChange(
            draft.copy(
                rules = draft.rules.map { item ->
                    if (item.index == rule.index) {
                        item.copy(values = item.values.with(key, value))
                    } else {
                        item
                    }
                },
            )
        )
    }
    VisualSection(Res.string.rule_visual_section_rule) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.rule_visual_rule_index, position + 1),
                style = MaterialTheme.typography.titleSmall,
            )
            if (draft.rules.size > 1) {
                GkIconButton(
                    imageVector = GkIcons.Delete,
                    contentDescription = stringResource(Res.string.rule_visual_remove_rule),
                    onClick = onRemoveRule,
                )
            }
        }
        var overrideHintShown = false
        ruleFieldsFor(target).forEach { field ->
            if (field.overridesGroup && !overrideHintShown) {
                overrideHintShown = true
                Text(
                    text = stringResource(Res.string.rule_visual_inherit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FieldEditor(rule.values, field, setValue)
        }
        TextButton(onClick = onAddRule) {
            Text(stringResource(Res.string.rule_visual_add_rule))
        }
    }
}

/** 按字段的控件类型渲染；类型来自 data class 的声明。 */
@Composable
private fun FieldEditor(
    values: VisualValues,
    field: VisualField,
    onValueChange: (String, String) -> Unit,
) {
    val set: (String) -> Unit = { onValueChange(field.name, it) }
    when (field.control) {
        VisualControl.Bool -> VisualSwitch(values, field.name, field.description, set)
        VisualControl.Int, VisualControl.Long ->
            VisualNumber(values, field.name, field.description, set)

        VisualControl.Text -> VisualText(values, field.name, field.description, set)
        VisualControl.Action ->
            VisualChoice(values, field.name, field.description, actionOptions, set)

        VisualControl.ResetMatch ->
            VisualChoice(values, field.name, field.description, resetMatchOptions, set)

        VisualControl.TextList, VisualControl.IntList, VisualControl.Apps ->
            VisualLines(
                values = values,
                key = field.name,
                description = field.description,
                onValueChange = set,
                required = field.required,
            )

        VisualControl.Position -> PositionEditor(values, field.description, onValueChange)
        VisualControl.SwipeArg -> SwipeEditor(values, field.description, onValueChange)
        VisualControl.VersionCode -> VersionCodeEditor(values, field.description, onValueChange)
        VisualControl.VersionName -> VersionNameEditor(values, field.description, onValueChange)
        // 规则列表由规则页承载，不走字段编辑器。
        VisualControl.Rules -> Unit
    }
}

/** `Position`：六个子字段，与 data class 一致。 */
@Composable
private fun PositionEditor(
    values: VisualValues,
    description: StringResource,
    onValueChange: (String, String) -> Unit,
) {
    VisualSubSection(Res.string.rule_visual_section_position) {
        VisualSubFields.position.forEach { name ->
            val key = RuleVisualDraft.positionKey(name)
            VisualText(values, key, description, onValueChange = { onValueChange(key, it) }, label = name)
        }
    }
}

/** `SwipeArg`：start/end 各六个子字段，加 duration。 */
@Composable
private fun SwipeEditor(
    values: VisualValues,
    description: StringResource,
    onValueChange: (String, String) -> Unit,
) {
    VisualSubSection(Res.string.rule_visual_section_swipe) {
        VisualSubFields.position.forEach { name ->
            val key = RuleVisualDraft.swipeStartKey(name)
            VisualText(
                values = values,
                key = key,
                description = description,
                label = "start.$name",
                onValueChange = { onValueChange(key, it) },
            )
        }
        VisualSubFields.position.forEach { name ->
            val key = RuleVisualDraft.swipeEndKey(name)
            VisualText(
                values = values,
                key = key,
                description = description,
                label = "end.$name",
                onValueChange = { onValueChange(key, it) },
            )
        }
        val durationKey = RuleVisualDraft.SwipeDuration
        VisualNumber(
            values = values,
            key = durationKey,
            description = Res.string.rule_help_p_swipe_arg,
            onValueChange = { onValueChange(durationKey, it) },
            label = VisualSubFields.duration,
        )
    }
}

/** `IntegerMatcher`：minimum / maximum / include / exclude。 */
@Composable
private fun VersionCodeEditor(
    values: VisualValues,
    description: StringResource,
    onValueChange: (String, String) -> Unit,
) {
    VisualSubSection(Res.string.rule_help_p_version_code) {
        VisualSubFields.versionCode.forEach { name ->
            val key = RuleVisualDraft.versionCodeKey(name)
            if (name == "include" || name == "exclude") {
                VisualLines(
                    values = values,
                    key = key,
                    description = description,
                    onValueChange = { onValueChange(key, it) },
                    label = name,
                )
            } else {
                VisualNumber(
                    values = values,
                    key = key,
                    description = description,
                    onValueChange = { onValueChange(key, it) },
                    label = name,
                )
            }
        }
    }
}

/** `StringMatcher`：pattern / include / exclude。 */
@Composable
private fun VersionNameEditor(
    values: VisualValues,
    description: StringResource,
    onValueChange: (String, String) -> Unit,
) {
    VisualSubSection(Res.string.rule_help_p_version_name) {
        VisualSubFields.versionName.forEach { name ->
            val key = RuleVisualDraft.versionNameKey(name)
            if (name == "pattern") {
                VisualText(
                    values = values,
                    key = key,
                    description = description,
                    onValueChange = { onValueChange(key, it) },
                    label = name,
                )
            } else {
                VisualLines(
                    values = values,
                    key = key,
                    description = description,
                    onValueChange = { onValueChange(key, it) },
                    label = name,
                )
            }
        }
    }
}

/** 最后一页：当前草稿生成的 JSON5，未填完时说明原因。 */
@Composable
private fun GeneratedPage(generated: String?) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = generated ?: stringResource(Res.string.rule_visual_incomplete),
                style = if (generated != null) {
                    MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 带标题的卡片；字段与顺序由调用方从 schema 传入。 */
@Composable
private fun VisualSection(
    title: StringResource,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

/** 嵌套 data class 的子字段区，用更轻的描边区分层级。 */
@Composable
private fun VisualSubSection(
    title: StringResource,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

/** 字段名按 JSON 属性名显示；规则级的 "rule." 前缀只用于表单区分，不显示。 */
private fun fieldLabel(key: String) = key.removePrefix("rule.")

@Composable
private fun VisualText(
    values: VisualValues,
    key: String,
    description: StringResource?,
    onValueChange: (String) -> Unit,
    label: String = fieldLabel(key),
    required: Boolean = false,
    placeholder: StringResource? = null,
) {
    GkOutlinedTextField(
        value = values[key],
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { VisualLabel(label, required) },
        placeholder = placeholder?.let { { Text(stringResource(it)) } },
        supportingText = description?.let { { Text(stringResource(it)) } },
    )
}

@Composable
private fun VisualLines(
    values: VisualValues,
    key: String,
    description: StringResource?,
    onValueChange: (String) -> Unit,
    label: String = fieldLabel(key),
    required: Boolean = false,
) {
    GkOutlinedTextField(
        value = values[key],
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        label = { VisualLabel(label, required) },
        supportingText = description?.let { { Text(stringResource(it)) } },
    )
}

@Composable
private fun VisualNumber(
    values: VisualValues,
    key: String,
    description: StringResource?,
    onValueChange: (String) -> Unit,
    label: String = fieldLabel(key),
) {
    GkOutlinedTextField(
        value = values[key],
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == '-' }) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { VisualLabel(label, false) },
        supportingText = description?.let { { Text(stringResource(it)) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

/**
 * 布尔字段用三态复选框：不确定 = 未设置（不写入该字段，跟随默认值），
 * 选中 = `true`，未选中 = `false`。点击按 未设置 → true → false → 未设置 循环。
 */
@Composable
private fun VisualSwitch(
    values: VisualValues,
    key: String,
    description: StringResource?,
    onValueChange: (String) -> Unit,
    label: String = fieldLabel(key),
) {
    val raw = values[key]
    val state = when (raw) {
        "true" -> ToggleableState.On
        "false" -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }
    val stateText = when (raw) {
        "true" -> stringResource(Res.string.turned_on)
        "false" -> stringResource(Res.string.turned_off)
        else -> stringResource(Res.string.rule_visual_unset)
    }
    val next = when (raw) {
        "" -> "true"
        "true" -> "false"
        else -> ""
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            VisualLabel(label, false)
            Text(
                text = description?.let { stringResource(it) + " · " + stateText } ?: stateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TriStateCheckbox(
            state = state,
            onClick = { onValueChange(next) },
            modifier = Modifier.semantics { contentDescription = stateText },
        )
    }
}

/** 枚举字段用下拉；空值显示为「不设置」。 */
@Composable
private fun VisualChoice(
    values: VisualValues,
    key: String,
    description: StringResource?,
    options: List<Pair<String, StringResource>>,
    onValueChange: (String) -> Unit,
    label: String = fieldLabel(key),
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == values[key] }?.second
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            VisualLabel(label, false)
            if (description != null) {
                Text(
                    text = stringResource(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(
                    text = selectedLabel?.let { stringResource(it) }
                        ?: stringResource(Res.string.rule_visual_unset),
                    style = MaterialTheme.typography.bodyMedium,
                )
                GkIcon(imageVector = GkIcons.UnfoldMore, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(optionLabel)) },
                        onClick = {
                            expanded = false
                            onValueChange(value)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VisualLabel(label: String, required: Boolean) {
    Text(
        text = label + if (required) " *" else "",
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
    )
}

private val actionOptions: List<Pair<String, StringResource>> by lazy {
    RuleVisualAction.entries.map { it.value to it.labelResource }
}

private val resetMatchOptions: List<Pair<String, StringResource>> by lazy {
    RuleVisualResetMatch.entries.map { it.value to it.labelResource }
}
