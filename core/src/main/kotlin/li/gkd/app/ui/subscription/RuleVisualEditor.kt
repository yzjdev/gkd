package li.gkd.app.ui.subscription

import li.gkd.app.R

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ripple
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import li.gkd.app.resources.*
import kotlin.math.roundToInt
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkExpandableSection
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkPageBottomSpace
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

/** 步进页：规则组属性按 schema 分区，规则各占一页。 */
private sealed interface VisualStep {
    data class Group(val section: VisualSection) : VisualStep
    data class Rule(val position: Int) : VisualStep
}

/** Tab 在滚动行内的水平边界（px）。 */
private data class TabBounds(val left: Int, val width: Int) {
    val right: Int get() = left + width
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
) {
    val steps = remember(draft.rules.size, target) { buildSteps(draft.rules.size, target) }
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    var pendingPage by remember { mutableStateOf<Int?>(null) }
    // 每个 Tab 在滚动行内的边界（px），用于跟随滚动把选中 Tab 居中。
    var tabPositions by remember { mutableStateOf(listOf<TabBounds>()) }
    // 各页正文滚动状态：翻到新页后可将其滚回页首。
    val pageScrollStates = remember { mutableStateMapOf<Int, ScrollState>() }
    val values = draft.values
    val setValue: (String, String) -> Unit = { key, value ->
        onDraftChange(draft.copy(values = values.with(key, value)))
    }
    LaunchedEffect(steps.size, pendingPage) {
        val next = pendingPage
        if (next != null && next in steps.indices) {
            pendingPage = null
            pagerState.animateScrollToPage(next)
            // 新规则页从页首开始浏览，避免停留在上一页的滚动位置。
            pageScrollStates[next]?.scrollTo(0)
        } else if (pagerState.currentPage > steps.lastIndex) {
            pagerState.scrollToPage(steps.lastIndex)
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        // 轻量扁平 Tab：Row + clickable，避免 TabRow/Tab 组件的额外语义与指示器开销。
        // 选中态用 currentPage 驱动：拖动跨过中线时高亮立即跟随切换。
        val settledIndex = pagerState.currentPage.coerceIn(steps.indices)
        val tabScrollState = rememberScrollState()
        // Tab 行跟随：选中 Tab 超出视口时，把它滚动到可视范围中央。
        // key 含 tabPositions.size：新增规则后新 Tab 首次完成测量也会触发一次居中。
        LaunchedEffect(settledIndex, tabPositions.size, tabScrollState.viewportSize) {
            val tabInfo = tabPositions.getOrNull(settledIndex) ?: return@LaunchedEffect
            val target = (tabInfo.left + tabInfo.width / 2) - tabScrollState.viewportSize / 2
            tabScrollState.animateScrollTo(target.coerceIn(0, tabScrollState.maxValue))
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(tabScrollState)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.forEachIndexed { index, step ->
                val selected = settledIndex == index
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.tertiaryContainer
                            else Color.Transparent
                        )
                        .clickable(
                            interactionSource = null,
                            indication = ripple(),
                        ) { scope.launch { pagerState.animateScrollToPage(index) } }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        // 记录每个 Tab 在行内的位置与宽度，供跟随滚动计算。
                        .onGloballyPositioned { coordinates ->
                            val offset = coordinates.positionInParent()
                            val width = coordinates.size.width
                            val updated = tabPositions.toMutableList().apply {
                                while (size <= index) add(TabBounds(0, 0))
                                this[index] = TabBounds(offset.x.roundToInt(), width)
                            }
                            tabPositions = updated
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val tabColor = if (selected) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                    GkIcon(
                        imageVector = stepIcon(step, target),
                        modifier = Modifier.size(14.dp),
                        tint = tabColor,
                        contentDescription = null,
                    )
                    Text(
                        stepTitle(step, target),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = tabColor,
                    )
                }
            }
        }
        val density = LocalDensity.current
        // 仅用于收起页尾留白：键盘高度大于 0 即视为已弹出。
        val imeVisible = WindowInsets.ime.getBottom(density) > 0
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // 预组合相邻页：切页时两侧内容已就绪，避免滑入过程中现场构建组合导致掉帧。
            beyondViewportPageCount = 1,
        ) { index ->
            val step = steps[index.coerceIn(steps.indices)]
            val pageScroll = remember(index) { ScrollState(0) }.also { pageScrollStates[index] = it }
            Column(
                // insets 位于 verticalScroll 之前：视口随键盘收缩，内容仍可滚动。
                modifier = Modifier.fillMaxSize().then(contentInsets)
                    .verticalScroll(pageScroll)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
}

@Composable
private fun stepTitle(step: VisualStep, target: VisualTarget): String = when (step) {
    is VisualStep.Group -> stringResource(sectionTitle(step.section, target))
    is VisualStep.Rule -> stringResource(R.string.rule_visual_rule_index, step.position + 1)
}

private fun stepIcon(step: VisualStep, target: VisualTarget): ImageVector = when (step) {
    is VisualStep.Group -> sectionIcon(step.section, target)
    is VisualStep.Rule -> GkIcons.Rule
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

private fun sectionTitle(section: VisualSection, target: VisualTarget): Int =
    when (section) {
        VisualSection.Identity -> R.string.rule_visual_section_group
        VisualSection.Target ->
            if (target == VisualTarget.GlobalGroup) R.string.rule_help_global_props
            else R.string.rule_help_app_props

        VisualSection.Timing -> R.string.rule_visual_section_timing
        VisualSection.Links -> R.string.rule_visual_section_urls
        VisualSection.Rules -> R.string.rule_visual_section_rule
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
                description = R.string.rule_help_p_apps,
                required = true,
                placeholder = R.string.rule_visual_app_id_hint,
                onValueChange = { onValueChange(RuleVisualDraft.AppId, it) },
            )
        }
        RenderFieldGroups(fields = fields) { field ->
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
        title = R.string.rule_visual_section_rule,
        icon = GkIcons.Rule,
        section = VisualSection.Rules,
    ) {
        if (draft.rules.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                GkIconButton(
                    imageVector = GkIcons.Delete,
                    contentDescription = stringResource(R.string.rule_visual_remove_rule),
                    onClick = onRemoveRule,
                )
            }
        }
        var overrideHintShown = false
        val fields = ruleFieldsFor(target)
        RenderFieldGroups(fields = fields) { field ->
            if (field.overridesGroup && !overrideHintShown) {
                overrideHintShown = true
                Text(
                    text = stringResource(R.string.rule_visual_inherit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FieldEditor(rule.values, field, setValue)
        }
        FilledTonalButton(
            onClick = onAddRule,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            GkIcon(imageVector = GkIcons.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.rule_visual_add_rule))
        }
    }
}

/**
 * 按 schema 的子分组拆分渲染：未归组的字段平铺在前，
 * 其余字段按 [VisualSubGroup] 收进默认折叠的手风琴。
 */
@Composable
private fun RenderFieldGroups(
    fields: List<VisualField>,
    fieldContent: @Composable (VisualField) -> Unit,
) {
    val grouping = remember(fields) { fields.groupBySubGroup() }
    grouping.plain.forEach { field -> fieldContent(field) }
    grouping.grouped.forEach { (subGroup, groupFields) ->
        VisualSubSection(title = subGroup.title) {
            groupFields.forEach { field -> fieldContent(field) }
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
    description: Int,
    onValueChange: (String, String) -> Unit,
) {
    VisualSubSection(R.string.rule_visual_section_position) {
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
    description: Int,
    onValueChange: (String, String) -> Unit,
) {
    VisualSubSection(R.string.rule_visual_section_swipe) {
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
            description = R.string.rule_help_p_swipe_arg,
            onValueChange = { onValueChange(durationKey, it) },
            label = VisualSubFields.duration,
        )
    }
}

/** `IntegerMatcher`：minimum / maximum / include / exclude。 */
@Composable
private fun VersionCodeEditor(
    values: VisualValues,
    description: Int,
    onValueChange: (String, String) -> Unit,
) {
    VisualSubSection(R.string.rule_help_p_version_code) {
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
    description: Int,
    onValueChange: (String, String) -> Unit,
) {
    VisualSubSection(R.string.rule_help_p_version_name) {
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

/**
 * 分区容器：无底色，字段直接平铺在页面上；标题由顶部 Tab 承载。
 */
@Composable
private fun VisualSection(
    title: Int,
    icon: ImageVector,
    section: VisualSection,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

/** 嵌套 data class 的子字段区：圆角卡片包裹的手风琴，默认收起。 */
@Composable
private fun VisualSubSection(
    title: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    GkExpandableSection(
        expanded = expanded,
        onToggle = { expanded = !expanded },
        header = { indicator ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (expanded) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                indicator()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                start = 14.dp, end = 14.dp, bottom = 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            content()
        }
    }
}

/** 字段名按 JSON 属性名显示；规则级的 "rule." 前缀只用于表单区分，不显示。 */
private fun fieldLabel(key: String) = key.removePrefix("rule.")

/**
 * 文本框：大圆角填充式容器，无任何装饰——无描边、无指示条，仅靠容器色区分。
 */
@Composable
private fun expressiveFieldShape(): Shape = RoundedCornerShape(24.dp)

@Composable
private fun expressiveFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    errorContainerColor = MaterialTheme.colorScheme.errorContainer,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.tertiary,
)

@Composable
private fun VisualText(
    values: VisualValues,
    key: String,
    description: Int?,
    onValueChange: (String) -> Unit,
    label: String = fieldLabel(key),
    required: Boolean = false,
    placeholder: Int? = null,
) {
    TextField(
        value = values[key],
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = expressiveFieldShape(),
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
    description: Int?,
    onValueChange: (String) -> Unit,
    label: String = fieldLabel(key),
    required: Boolean = false,
) {
    TextField(
        value = values[key],
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        // 默认只占一行高度，输入换行后自然增高。
        minLines = 1,
        maxLines = 6,
        singleLine = false,
        shape = expressiveFieldShape(),
        colors = expressiveFieldColors(),
        label = { VisualLabel(label, required) },
        supportingText = description?.let { { Text(stringResource(it)) } },
    )
}

@Composable
private fun VisualNumber(
    values: VisualValues,
    key: String,
    description: Int?,
    onValueChange: (String) -> Unit,
    label: String = fieldLabel(key),
) {
    TextField(
        value = values[key],
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == '-' }) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = expressiveFieldShape(),
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
    description: Int?,
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
        "true" -> stringResource(R.string.turned_on)
        "false" -> stringResource(R.string.turned_off)
        else -> stringResource(R.string.rule_visual_unset)
    }
    val next = when (raw) {
        "" -> "true"
        "true" -> "false"
        else -> ""
    }
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            VisualLabel(label, false)
            Text(
                text = description?.let { stringResource(it) + " · " + stateText } ?: stateText,
                style = MaterialTheme.typography.bodySmall,
                color = when (raw) {
                    "true" -> MaterialTheme.colorScheme.tertiary
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
                checkedColor = MaterialTheme.colorScheme.tertiary,
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
    description: Int?,
    options: List<Pair<String, Int>>,
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
                shape = expressiveFieldShape(),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = selectedLabel?.let { stringResource(it) }
                            ?: stringResource(R.string.rule_visual_unset),
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
                        tint = MaterialTheme.colorScheme.tertiary,
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

private val actionOptions: List<Pair<String, Int>> by lazy {
    RuleVisualAction.entries.map { it.value to it.labelResource }
}

private val resetMatchOptions: List<Pair<String, Int>> by lazy {
    RuleVisualResetMatch.entries.map { it.value to it.labelResource }
}
