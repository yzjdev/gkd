package li.gkd.app.ui.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
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
import li.gkd.app.ui.component.GkPageBottomSpace
import li.gkd.app.ui.style.surfaceCardColors
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
    /** 只作用于正文滚动区：顶部 Tab 固定不动，避免键盘弹出时整页被顶起。 */
    contentInsets: Modifier = Modifier,
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
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            edgePadding = 12.dp,
            indicator = {},
            divider = {},
        ) {
            steps.forEachIndexed { index, step ->
                val selected = pagerState.currentPage == index
                Tab(
                    selected = selected,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            GkIcon(
                                imageVector = stepIcon(step, target),
                                modifier = Modifier.size(18.dp),
                                contentDescription = null,
                            )
                            Text(stepTitle(step, target), maxLines = 1)
                        }
                    },
                )
            }
        }
        val density = LocalDensity.current
        // 仅用于收起页尾留白：键盘高度大于 0 即视为已弹出。
        val imeVisible = WindowInsets.ime.getBottom(density) > 0
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            val step = steps[index.coerceIn(steps.indices)]
            Column(
                // insets 位于 verticalScroll 之前：视口随键盘收缩，内容仍可滚动。
                modifier = Modifier.fillMaxSize().then(contentInsets)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    GkIcon(
                        imageVector = GkIcons.Info,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(Res.string.rule_visual_required_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                // 键盘弹起时页尾留白会变成输入框与键盘之间的空档，此时收起。
                if (!imeVisible) {
                    GkPageBottomSpace()
                }
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

private fun stepIcon(step: VisualStep, target: VisualTarget): ImageVector = when (step) {
    is VisualStep.Group -> sectionIcon(step.section, target)
    is VisualStep.Rule -> GkIcons.Rule
    VisualStep.Preview -> GkIcons.Code
}

/** 分区图标与 Tab 一致；作用范围按全局/应用目标区分。 */
private fun sectionIcon(section: VisualSection, target: VisualTarget): ImageVector = when (section) {
    VisualSection.Identity -> GkIcons.Title
    VisualSection.Target ->
        if (target == VisualTarget.GlobalGroup) GkIcons.CenterFocusWeak
        else GkIcons.Apps

    VisualSection.Timing -> GkIcons.Schedule
    VisualSection.Links -> GkIcons.Link
    VisualSection.Rules -> GkIcons.Rule
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
    VisualSection(
        title = sectionTitle(section, target),
        icon = sectionIcon(section, target),
        section = section,
    ) {
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
    VisualSection(
        title = Res.string.rule_visual_section_rule,
        icon = GkIcons.Rule,
        section = VisualSection.Rules,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GkIcon(imageVector = GkIcons.Rule, contentDescription = null)
                Text(
                    text = stringResource(Res.string.rule_visual_rule_index, position + 1),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
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
            GkIcon(imageVector = GkIcons.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
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
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = surfaceCardColors,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    GkIcon(
                        imageVector = GkIcons.Code,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentDescription = null,
                    )
                }
                Text(
                    text = stringResource(Res.string.rule_visual_generated),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (generated != null) {
                Text(
                    text = generated,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(14.dp),
                )
            } else {
                Text(
                    text = stringResource(Res.string.rule_visual_incomplete),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 分区强调色：每页一组容器色，卡片徽章、描边与焦点态共用，保证鲜艳且成体系。 */
private data class SectionAccent(
    val container: Color,
    val onContainer: Color,
)

@Composable
private fun sectionAccent(section: VisualSection): SectionAccent {
    val scheme = MaterialTheme.colorScheme
    return when (section) {
        VisualSection.Identity ->
            SectionAccent(scheme.primaryContainer, scheme.onPrimaryContainer)

        VisualSection.Target ->
            SectionAccent(scheme.secondaryContainer, scheme.onSecondaryContainer)

        VisualSection.Timing ->
            SectionAccent(scheme.tertiaryContainer, scheme.onTertiaryContainer)

        VisualSection.Links ->
            SectionAccent(scheme.primaryContainer, scheme.onPrimaryContainer)

        VisualSection.Rules ->
            SectionAccent(scheme.secondaryContainer, scheme.onSecondaryContainer)
    }
}

/** 带图标标题的卡片；字段与顺序由调用方从 schema 传入。 */
@Composable
private fun VisualSection(
    title: StringResource,
    icon: ImageVector,
    section: VisualSection,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accent = sectionAccent(section)
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.5.dp, accent.container),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(32.dp)
                        .clip(CircleShape)
                        .background(accent.container),
                    contentAlignment = Alignment.Center,
                ) {
                    GkIcon(
                        imageVector = icon,
                        modifier = Modifier.size(18.dp),
                        tint = accent.onContainer,
                        contentDescription = null,
                    )
                }
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            content()
        }
    }
}

/** 嵌套 data class 的子字段区，用 tonal 底区分层级，不再叠加描边卡片。 */
@Composable
private fun VisualSubSection(
    title: StringResource,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

/** 字段名按 JSON 属性名显示；规则级的 "rule." 前缀只用于表单区分，不显示。 */
private fun fieldLabel(key: String) = key.removePrefix("rule.")

/** MD3 Expressive 输入控件统一外观：16dp 大圆角、填充式 tonal 底、无描边。 */
private val expressiveFieldShape = RoundedCornerShape(16.dp)

@Composable
private fun expressiveFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorContainerColor = MaterialTheme.colorScheme.errorContainer,
    // 填充式容器 + 聚焦时只有一圈 primary 描边，保持鲜艳但不过度叠加。
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
)

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
    OutlinedTextField(
        value = values[key],
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = expressiveFieldShape,
        colors = expressiveFieldColors(),
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
    OutlinedTextField(
        value = values[key],
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        // 默认只占一行高度，输入换行后自然增高。
        minLines = 1,
        maxLines = 6,
        singleLine = false,
        shape = expressiveFieldShape,
        colors = expressiveFieldColors(),
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
    OutlinedTextField(
        value = values[key],
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == '-' }) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = expressiveFieldShape,
        colors = expressiveFieldColors(),
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
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            VisualLabel(label, false)
            Text(
                text = description?.let { stringResource(it) + " · " + stateText } ?: stateText,
                style = MaterialTheme.typography.bodySmall,
                color = when (raw) {
                    "true" -> MaterialTheme.colorScheme.primary
                    "false" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        TriStateCheckbox(
            state = state,
            onClick = { onValueChange(next) },
            modifier = Modifier.semantics { contentDescription = stateText },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}

/** 枚举字段用下拉；空值显示为「不设置」。触发框与输入框同为描边样式。 */
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        VisualLabel(label, false)
        if (description != null) {
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            Surface(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = expressiveFieldShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = selectedLabel?.let { stringResource(it) }
                            ?: stringResource(Res.string.rule_visual_unset),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (values[key].isNullOrEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    GkIcon(
                        imageVector = GkIcons.UnfoldMore,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                    )
                }
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
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        )
        if (required) {
            Text(
                text = "*",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private val actionOptions: List<Pair<String, StringResource>> by lazy {
    RuleVisualAction.entries.map { it.value to it.labelResource }
}

private val resetMatchOptions: List<Pair<String, StringResource>> by lazy {
    RuleVisualResetMatch.entries.map { it.value to it.labelResource }
}
