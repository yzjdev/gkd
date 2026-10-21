package li.gkd.app.ui.subscription

import li.gkd.app.R

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import li.gkd.app.resources.*
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkPageBottomSpace

/** 可视化编辑器当前聚焦的层级：规则组设置或某一条规则。 */
sealed interface VisualEditorTarget {
    object GroupSettings : VisualEditorTarget
    data class Rule(val index: Int) : VisualEditorTarget
}

/**
 * 可视化规则编辑器：总览列表 + 聚焦编辑。
 *
 * 总览只呈现规则组摘要与每条规则的一行摘要，点击任一入口后进入该单项的
 * 全屏编辑页。聚焦层级由调用方持有，宿主顶栏据此显示标题与返回按钮；
 * 字段集与控件类型来自 schema，始终与 data class 一致。
 */
@Composable
fun RuleVisualEditor(
    draft: VisualRuleGroupDraft,
    target: VisualTarget,
    onDraftChange: (VisualRuleGroupDraft) -> Unit,
    /** 当前聚焦层级；null 表示总览。 */
    editing: VisualEditorTarget?,
    onEditingChange: (VisualEditorTarget?) -> Unit,
    modifier: Modifier = Modifier,
    /** 只作用于正文滚动区：顶栏固定不动，避免键盘弹出时整页被顶起。 */
    contentInsets: Modifier = Modifier,
) {
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val overviewScroll = rememberScrollState()
    // 正在编辑的规则被删除后自动退回总览。
    val active = when (val current = editing) {
        is VisualEditorTarget.Rule ->
            if (draft.rules.any { it.index == current.index }) current else null

        else -> current
    }
    Box(modifier = modifier.fillMaxSize()) {
        when (val current = active) {
            null -> Overview(
                draft = draft,
                target = target,
                onDraftChange = onDraftChange,
                scrollState = overviewScroll,
                contentInsets = contentInsets,
                imeVisible = imeVisible,
                onOpen = onEditingChange,
            )

            VisualEditorTarget.GroupSettings -> GroupSettingsDetail(
                draft = draft,
                target = target,
                onDraftChange = onDraftChange,
                contentInsets = contentInsets,
                imeVisible = imeVisible,
            )

            is VisualEditorTarget.Rule -> RuleDetail(
                draft = draft,
                target = target,
                ruleIndex = current.index,
                onDraftChange = onDraftChange,
                contentInsets = contentInsets,
                imeVisible = imeVisible,
                onBack = { onEditingChange(null) },
            )
        }
    }
}

/** 总览：规则组摘要 + 规则摘要列表 + 新增入口。 */
@Composable
private fun Overview(
    draft: VisualRuleGroupDraft,
    target: VisualTarget,
    onDraftChange: (VisualRuleGroupDraft) -> Unit,
    scrollState: ScrollState,
    contentInsets: Modifier,
    imeVisible: Boolean,
    onOpen: (VisualEditorTarget) -> Unit,
) {
    val onAddRule: () -> Unit = {
        val nextIndex = (draft.rules.maxOfOrNull { it.index } ?: -1) + 1
        onDraftChange(draft.copy(rules = draft.rules + VisualRuleGroupDraft.newRule(nextIndex)))
        // 新增后直接进入该规则的聚焦编辑。
        onOpen(VisualEditorTarget.Rule(nextIndex))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(contentInsets)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GroupSummary(
            draft = draft,
            target = target,
            onClick = { onOpen(VisualEditorTarget.GroupSettings) },
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Text(
                    text = stringResource(R.string.rule_visual_tab_rules),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.rule_visual_rules_count, draft.rules.size.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                draft.rules.forEachIndexed { position, rule ->
                    if (position > 0) {
                        ThinDivider(startInset = 50.dp)
                    }
                    RuleSummaryRow(
                        position = position,
                        rule = rule,
                        onClick = { onOpen(VisualEditorTarget.Rule(rule.index)) },
                    )
                }
                if (draft.rules.isNotEmpty()) {
                    ThinDivider(startInset = 50.dp)
                }
                AddRuleRow(onClick = onAddRule)
            }
        }
        // 键盘弹起时页尾留白会变成输入框与键盘之间的空档，此时收起。
        if (!imeVisible) {
            GkPageBottomSpace()
        }
    }
}

/** 规则组摘要：名称 + 目标类型 + 启用状态，点击进入设置。 */
@Composable
private fun GroupSummary(
    draft: VisualRuleGroupDraft,
    target: VisualTarget,
    onClick: () -> Unit,
) {
    val name = draft.values.text("name") ?: stringResource(R.string.rule_visual_group_untitled)
    val enabled = draft.values.bool("enable") ?: true
    val subtitle = listOfNotNull(
        targetLabel(target),
        draft.values.text(RuleVisualDraft.AppId),
    ).joinToString("  ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(
                interactionSource = null,
                indication = ripple(),
            ) { onClick() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                GkIcon(
                    imageVector = sectionIcon(VisualSection.Target, target),
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    contentDescription = null,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GkIcon(
                imageVector = GkIcons.KeyboardArrowRight,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = null,
            )
        }
        Spacer(Modifier.height(12.dp))
        InfoChip(
            text = stringResource(if (enabled) R.string.turned_on else R.string.turned_off),
            tinted = enabled,
        )
    }
}

/** 摘要标签。 */
@Composable
private fun InfoChip(text: String, tinted: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (tinted) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (tinted) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** 规则摘要行：序号 + 名称 + 动作与选择器预览，点击进入该规则的聚焦编辑。 */
@Composable
private fun RuleSummaryRow(
    position: Int,
    rule: VisualRuleDraft,
    onClick: () -> Unit,
) {
    val name = rule.values.text("name") ?: stringResource(R.string.rule_visual_rule_untitled)
    val actionLabel = rule.values.text("action")
        ?.let { value -> RuleVisualAction.entries.firstOrNull { it.value == value }?.labelResource }
        ?.let { stringResource(it) }
    val selectorCount = rule.values.lines("matches").size
    val subtitle = listOfNotNull(
        actionLabel,
        if (selectorCount > 0) {
            stringResource(R.string.rule_visual_selector_count, selectorCount.toString())
        } else {
            null
        },
    ).joinToString("  ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = ripple(),
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (position + 1).toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        GkIcon(
            imageVector = GkIcons.KeyboardArrowRight,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            contentDescription = null,
        )
    }
}

/** 列表末尾的新增入口。 */
@Composable
private fun AddRuleRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = ripple(),
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(26.dp),
            contentAlignment = Alignment.Center,
        ) {
            GkIcon(
                imageVector = GkIcons.Add,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.tertiary,
                contentDescription = null,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.rule_visual_add_rule),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

/** 规则组设置的聚焦编辑页。 */
@Composable
private fun GroupSettingsDetail(
    draft: VisualRuleGroupDraft,
    target: VisualTarget,
    onDraftChange: (VisualRuleGroupDraft) -> Unit,
    contentInsets: Modifier,
    imeVisible: Boolean,
) {
    val setValue: (String, String) -> Unit = { key, value ->
        onDraftChange(draft.copy(values = draft.values.with(key, value)))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(contentInsets)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // 分区顺序固定为「身份 → 作用范围 → 计时 → 链接」。
        val present = groupFieldsFor(target).mapTo(mutableSetOf()) { it.section }
        listOf(VisualSection.Identity, VisualSection.Target, VisualSection.Timing, VisualSection.Links)
            .filter { it in present }
            .forEach { section ->
                SettingSection(
                    values = draft.values,
                    section = section,
                    target = target,
                    onValueChange = setValue,
                )
            }
        // 键盘弹起时页尾留白会变成输入框与键盘之间的空档，此时收起。
        if (!imeVisible) {
            GkPageBottomSpace()
        }
    }
}

/** 单条规则的聚焦编辑页。 */
@Composable
private fun RuleDetail(
    draft: VisualRuleGroupDraft,
    target: VisualTarget,
    ruleIndex: Int,
    onDraftChange: (VisualRuleGroupDraft) -> Unit,
    contentInsets: Modifier,
    imeVisible: Boolean,
    onBack: () -> Unit,
) {
    val position = draft.rules.indexOfFirst { it.index == ruleIndex }
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
    val onRemove: () -> Unit = {
        onDraftChange(draft.copy(rules = draft.rules.filterIndexed { i, _ -> i != position }))
        onBack()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(contentInsets)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        RenderFieldBlocks(
            values = rule.values,
            fields = ruleFieldsFor(target),
            onValueChange = setValue,
        )
        if (draft.rules.size > 1) {
            Spacer(Modifier.height(20.dp))
            RemoveRuleRow(onClick = onRemove)
        }
        // 键盘弹起时页尾留白会变成输入框与键盘之间的空档，此时收起。
        if (!imeVisible) {
            GkPageBottomSpace()
        }
    }
}

/** 删除当前规则。 */
@Composable
private fun RemoveRuleRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(
                interactionSource = null,
                indication = ripple(),
            ) { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GkIcon(
            imageVector = GkIcons.Delete,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.error,
            contentDescription = null,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.rule_visual_remove_rule),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/** 一个设置分区：左侧强调竖条 + 图标标题 + 说明，字段为扁平列表。 */
@Composable
private fun SettingSection(
    values: VisualValues,
    section: VisualSection,
    target: VisualTarget,
    onValueChange: (String, String) -> Unit,
) {
    val fields = groupFieldsFor(target).filter { it.section == section }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.tertiary),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GkIcon(
                        imageVector = sectionIcon(section, target),
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(sectionTitle(section, target)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(sectionDescription(section, target)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        RenderFieldBlocks(
            values = values,
            fields = fields,
            onValueChange = onValueChange,
            leading = if (section == VisualSection.Identity && target == VisualTarget.NewApp) {
                // 新建应用时先填应用 id，它属于顶层的 RawApp 而不是规则组。
                {
                    LabeledField(
                        label = RuleVisualDraft.AppId,
                        description = R.string.rule_help_p_apps,
                        required = true,
                    ) {
                        EditorTextField(
                            value = values[RuleVisualDraft.AppId],
                            onValueChange = { onValueChange(RuleVisualDraft.AppId, it) },
                            singleLine = true,
                            placeholder = R.string.rule_visual_app_id_hint,
                        )
                    }
                }
            } else {
                null
            },
        )
    }
}

/**
 * 字段块：按 schema 顺序把连续同类字段合并成渲染单位。
 * 嵌套 data class 字段（position/swipe/version*）自带折叠分组，单独成块。
 */
private sealed interface FieldBlock {
    data class Plain(val fields: List<VisualField>) : FieldBlock
    data class Sub(val subGroup: VisualSubGroup, val fields: List<VisualField>) : FieldBlock
    data class Self(val field: VisualField) : FieldBlock
    /** 按用途收拢成组的平铺字段，组内共用一个折叠分组。 */
    data class Group(val group: FieldGroupDef, val fields: List<VisualField>) : FieldBlock
    /** 四个匹配选择器共用一组标签与输入框，整体折叠成一组。 */
    data class Selectors(val fields: List<VisualField>) : FieldBlock
}

/**
 * 一组同用途的平铺字段：schema 里它们是散在普通字段中的，
 * 这里按名字收进一个折叠分组，避免与其它零散字段混在一起。
 */
private data class FieldGroupDef(
    val id: String,
    @StringRes val title: Int,
    val icon: ImageVector,
    val names: List<String>,
    /** 分组一律默认收起，避免长表单首次进入时铺满屏幕。 */
    val expanded: Boolean = false,
)

private val basicFieldGroup = FieldGroupDef(
    id = "basic",
    title = R.string.rule_visual_section_identity_fields,
    icon = GkIcons.Key,
    // 组级另有 desc / enable / scopeKeys，规则级则是 preKeys；
    // 两者互不重叠，共用一个定义即可。
    names = listOf("key", "name", "desc", "enable", "scopeKeys", "preKeys"),
)

private val actionFieldGroup = FieldGroupDef(
    id = "action",
    title = R.string.rule_visual_section_action,
    icon = GkIcons.PlayArrow,
    names = listOf("action"),
)

private val orderFieldGroup = FieldGroupDef(
    id = "order",
    title = R.string.rule_visual_section_order,
    icon = GkIcons.Sort,
    names = listOf("order"),
)

private val fieldGroupDefs = listOf(basicFieldGroup, actionFieldGroup, orderFieldGroup)

private fun VisualField.fieldGroupDef(): FieldGroupDef? =
    fieldGroupDefs.firstOrNull { name in it.names }

private fun VisualField.isSelfCarding(): Boolean = when (control) {
    VisualControl.Position,
    VisualControl.SwipeArg,
    VisualControl.VersionCode,
    VisualControl.VersionName -> true

    else -> false
}

private fun fieldBlocks(fields: List<VisualField>): List<FieldBlock> = buildList {
    fields.forEach { field ->
        when {
            field.isSelfCarding() -> add(FieldBlock.Self(field))
            field.name in selectorFieldNames -> {
                // 四个选择器合成一组，无论中间是否夹了别的字段。
                val existing = indexOfLast { it is FieldBlock.Selectors }
                if (existing >= 0) {
                    val block = get(existing) as FieldBlock.Selectors
                    set(existing, block.copy(fields = block.fields + field))
                } else {
                    add(FieldBlock.Selectors(listOf(field)))
                }
            }
            field.fieldGroupDef() != null -> {
                val group = field.fieldGroupDef()!!
                val existing = indexOfLast { it is FieldBlock.Group && it.group.id == group.id }
                if (existing >= 0) {
                    val block = get(existing) as FieldBlock.Group
                    set(existing, block.copy(fields = block.fields + field))
                } else {
                    add(FieldBlock.Group(group, listOf(field)))
                }
            }
            field.subGroup != null -> {
                val subGroup = field.subGroup!!
                // 同一子分组只输出一次：schema 中会有非本组的字段（如 order）夹在
                // 同组字段中间，若只在相邻时合并，就会切出两个同名分组。
                val existing = indexOfLast { it is FieldBlock.Sub && it.subGroup == subGroup }
                if (existing >= 0) {
                    val block = get(existing) as FieldBlock.Sub
                    set(existing, block.copy(fields = block.fields + field))
                } else {
                    add(FieldBlock.Sub(subGroup, listOf(field)))
                }
            }
            else -> {
                val last = lastOrNull()
                if (last is FieldBlock.Plain) {
                    removeAt(lastIndex)
                    add(last.copy(fields = last.fields + field))
                } else {
                    add(FieldBlock.Plain(listOf(field)))
                }
            }
        }
    }
    // 参考链接是纯参考资料，固定排到最后，不参与前面的编辑顺序。
    val linksIndex = indexOfFirst { it is FieldBlock.Sub && it.subGroup == VisualSubGroup.RefLinks }
    if (linksIndex >= 0 && linksIndex != lastIndex) {
        val links = removeAt(linksIndex)
        add(links)
    }
}

/**
 * 按字段块顺序渲染扁平列表：平铺字段逐行输出，子分组进折叠分组，
 * 自分组字段直接输出；行与行之间以细分隔线区隔。
 */
@Composable
private fun RenderFieldBlocks(
    values: VisualValues,
    fields: List<VisualField>,
    onValueChange: (String, String) -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    val blocks = remember(fields) { fieldBlocks(fields) }
    Column(modifier = Modifier.fillMaxWidth()) {
        var first = true
        if (leading != null) {
            leading()
            first = false
        }
        blocks.forEach { block ->
            when (block) {
                is FieldBlock.Self -> {
                    if (!first) {
                        ThinDivider()
                    }
                    first = false
                    SelfGroup(field = block.field, values = values, onValueChange = onValueChange)
                }

                is FieldBlock.Sub -> {
                    if (!first) {
                        ThinDivider()
                    }
                    first = false
                    ConvergedFieldGroup(
                        icon = subGroupIcon(block.subGroup),
                        title = block.subGroup.title,
                        values = values,
                        fields = block.fields,
                        onValueChange = onValueChange,
                    )
                }

                is FieldBlock.Group -> {
                    if (!first) {
                        ThinDivider()
                    }
                    first = false
                    ConvergedFieldGroup(
                        icon = block.group.icon,
                        title = block.group.title,
                        values = values,
                        fields = block.fields,
                        onValueChange = onValueChange,
                        expanded = block.group.expanded,
                    )
                }

                is FieldBlock.Selectors -> {
                    if (!first) {
                        ThinDivider()
                    }
                    first = false
                    // 选择器是规则的核心内容，默认展开；其余分组保持收起。
                    CollapsibleGroup(
                        GkIcons.Search,
                        R.string.rule_visual_section_selectors,
                        initiallyExpanded = true,
                    ) {
                        SelectorField(values = values, onValueChange = onValueChange)
                    }
                }

                is FieldBlock.Plain -> {
                    block.fields.forEach { field ->
                        if (!first) {
                            ThinDivider()
                        }
                        first = false
                        FieldRow(
                            values = values,
                            field = field,
                            onValueChange = onValueChange,
                        )
                    }
                }
            }
        }
    }
}

/** 列表与字段之间的细分隔线；[startInset] 用于与列表文字左对齐。 */
@Composable
private fun ThinDivider(startInset: Dp = 0.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startInset)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/**
 * 收敛式字段分组：所有字段名以分段标签排列，同一时刻只展开一个字段的编辑器。
 * 字段类型决定编辑器形态（三态胶囊 / 下拉 / 单行或多行输入），避免逐个纵向堆叠。
 */
@Composable
private fun ConvergedFieldGroup(
    icon: ImageVector,
    @StringRes title: Int,
    values: VisualValues,
    fields: List<VisualField>,
    onValueChange: (String, String) -> Unit,
    expanded: Boolean = false,
) {
    if (fields.isEmpty()) return
    var selected by remember(fields) { mutableStateOf(fields.first().name) }
    val active = fields.firstOrNull { it.name == selected } ?: fields.first()
    CollapsibleGroup(icon, title, initiallyExpanded = expanded) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 只有一个字段时分组标题已说明用途，不再显示单个分段标签。
            if (fields.size > 1) {
                ChipRows(
                    labels = fields.map { it.name },
                    selectedIndex = fields.indexOf(active),
                    onSelect = { selected = fields[it].name },
                )
            }
            // 字段名已由上方分段标签标出，这里不再重复。
            FieldRow(
                values = values,
                field = active,
                onValueChange = onValueChange,
                showLabel = false,
            )
        }
    }
}

/** 选择器模式：四个列表共用一个输入框，分段切换当前编辑的列表。 */
private data class SelectorMode(
    val name: String,
    @StringRes val label: Int,
    @StringRes val description: Int,
    val required: Boolean = false,
)

private val selectorModes = listOf(
    SelectorMode(
        name = "matches",
        label = R.string.rule_visual_selector_matches,
        description = R.string.rule_help_p_matches,
        required = true,
    ),
    SelectorMode(
        name = "excludeMatches",
        label = R.string.rule_visual_selector_exclude,
        description = R.string.rule_help_p_exclude_matches,
    ),
    SelectorMode(
        name = "excludeAllMatches",
        label = R.string.rule_visual_selector_exclude_all,
        description = R.string.rule_help_p_exclude_all_matches,
    ),
    SelectorMode(
        name = "anyMatches",
        label = R.string.rule_visual_selector_any,
        description = R.string.rule_help_p_any_matches,
    ),
)

private val selectorFieldNames = selectorModes.mapTo(mutableSetOf()) { it.name }

/**
 * 四个选择器列表共用一个输入框：上方分段切换当前编辑的列表，
 * 下方始终复用同一个输入框，避免四份几乎相同的长文本框挤占页面。
 */
@Composable
private fun SelectorField(
    values: VisualValues,
    onValueChange: (String, String) -> Unit,
) {
    var mode by remember { mutableStateOf(selectorModes.first()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChipRows(
            labels = selectorModes.map { stringResource(it.label) },
            selectedIndex = selectorModes.indexOf(mode),
            onSelect = { mode = selectorModes[it] },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(mode.description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (mode.required) {
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "*",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        EditorTextField(
            value = values[mode.name],
            onValueChange = { onValueChange(mode.name, it) },
            singleLine = false,
            monospace = true,
        )
    }
}

/**
 * 按字段的控件类型渲染一行；类型来自 data class 的声明。
 * [showLabel] 为 false 时不重复字段名，供收敛分组使用。
 */
@Composable
private fun FieldRow(
    values: VisualValues,
    field: VisualField,
    onValueChange: (String, String) -> Unit,
    showLabel: Boolean = true,
) {
    val set: (String) -> Unit = { onValueChange(field.name, it) }
    when (field.control) {
        VisualControl.Bool -> TriStateField(
            values = values,
            key = field.name,
            description = field.description,
            onValueChange = set,
            showLabel = showLabel,
        )

        VisualControl.Int, VisualControl.Long -> LabeledField(
            label = field.name,
            description = field.description,
            required = field.required,
            showLabel = showLabel,
        ) {
            EditorTextField(
                value = values[field.name],
                onValueChange = set,
                singleLine = true,
                number = true,
            )
        }

        VisualControl.Text -> LabeledField(
            label = field.name,
            description = field.description,
            required = field.required,
            showLabel = showLabel,
        ) {
            EditorTextField(
                value = values[field.name],
                onValueChange = set,
                singleLine = true,
            )
        }

        VisualControl.Action -> ChoiceField(
            values = values,
            key = field.name,
            description = field.description,
            options = actionOptions,
            onValueChange = set,
            showLabel = showLabel,
        )

        VisualControl.ResetMatch -> ChoiceField(
            values = values,
            key = field.name,
            description = field.description,
            options = resetMatchOptions,
            onValueChange = set,
            showLabel = showLabel,
        )

        VisualControl.TextList, VisualControl.IntList, VisualControl.Apps -> LabeledField(
            label = field.name,
            description = field.description,
            required = field.required,
            showLabel = showLabel,
        ) {
            EditorTextField(
                value = values[field.name],
                onValueChange = set,
                singleLine = false,
            )
        }

        // position/swipe/version 等自分组字段由 FieldBlock.Self 承载，不会走到这里。
        VisualControl.Position,
        VisualControl.SwipeArg,
        VisualControl.VersionCode,
        VisualControl.VersionName,
        VisualControl.Rules -> Unit
    }
}

/**
 * 字段行：等宽字段名 + 必填标记 + 说明，控件在下。
 * [showLabel] 为 false 时省略字段名——收敛分组已用分段标签标出当前字段。
 */
@Composable
private fun LabeledField(
    label: String,
    @StringRes description: Int?,
    required: Boolean,
    showLabel: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showLabel) {
            Row {
                Text(
                    text = fieldLabel(label),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (required) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "*",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        if (description != null) {
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

/** 编辑输入框：填充式圆角容器，无指示线，无浮动 label。 */
@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    monospace: Boolean = false,
    number: Boolean = false,
    @StringRes placeholder: Int? = null,
) {
    val textStyle = if (monospace) {
        MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    } else {
        MaterialTheme.typography.bodyMedium
    }
    CompositionLocalProvider(LocalTextStyle provides textStyle) {
        TextField(
            value = value,
            onValueChange = { input ->
                onValueChange(if (number) input.filter { it.isDigit() || it == '-' } else input)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = 1,
            maxLines = if (singleLine) 1 else 6,
            shape = RoundedCornerShape(10.dp),
            colors = filledFieldColors(),
            placeholder = placeholder?.let { res -> { Text(stringResource(res)) } },
            keyboardOptions = if (number) {
                KeyboardOptions(keyboardType = KeyboardType.Number)
            } else {
                KeyboardOptions.Default
            },
        )
    }
}

@Composable
private fun filledFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    errorContainerColor = MaterialTheme.colorScheme.errorContainer,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
)

/**
 * 布尔字段用三态胶囊：跟随 = 未设置（不写入该字段），开 = `true`，关 = `false`。
 * 比复选框更直观地表达「继承组默认值」这一中间态。
 */
@Composable
private fun TriStateField(
    values: VisualValues,
    key: String,
    @StringRes description: Int?,
    onValueChange: (String) -> Unit,
    showLabel: Boolean = true,
) {
    val raw = values[key]
    LabeledField(
        label = key,
        description = description,
        required = false,
        showLabel = showLabel,
    ) {
        val labels = listOf(
            stringResource(R.string.rule_visual_tristate_follow),
            stringResource(R.string.turned_on),
            stringResource(R.string.turned_off),
        )
        ChipRows(
            labels = labels,
            selectedIndex = when (raw) {
                "true" -> 1
                "false" -> 2
                else -> 0
            },
            onSelect = { onValueChange(when (it) { 1 -> "true"; 2 -> "false"; else -> "" }) },
        )
    }
}

@Composable
private fun TriChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipBg by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(160),
        label = "chipBg",
    )
    val chipFg by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(160),
        label = "chipFg",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chipBg)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(
                interactionSource = null,
                indication = ripple(),
            ) { onClick() }
            // 横向留出内边距：文字不贴边，标签过宽时也不会被挤成窄条。
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = chipFg,
        )
    }
}

/** 枚举字段：整行下拉触发框，空值显示为「不设置」。 */
@Composable
private fun ChoiceField(
    values: VisualValues,
    key: String,
    @StringRes description: Int?,
    options: List<Pair<String, Int>>,
    onValueChange: (String) -> Unit,
    showLabel: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = values[key]
    val selectedLabel = options.firstOrNull { it.first == current }?.second
    LabeledField(
        label = key,
        description = description,
        required = false,
        showLabel = showLabel,
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(
                        interactionSource = null,
                        indication = ripple(),
                    ) { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedLabel?.let { stringResource(it) }
                        ?: stringResource(R.string.rule_visual_unset),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (current.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                GkIcon(
                    imageVector = GkIcons.UnfoldMore,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (value, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(optionLabel)) },
                        trailingIcon = {
                            if (value == current) {
                                GkIcon(
                                    imageVector = GkIcons.Check,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = null,
                                )
                            }
                        },
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

/** 嵌套 data class 字段：按控件类型进入对应的折叠分组。 */
@Composable
private fun SelfGroup(
    field: VisualField,
    values: VisualValues,
    onValueChange: (String, String) -> Unit,
) {
    when (field.control) {
        VisualControl.Position -> PositionGroup(values, field.description, onValueChange)
        VisualControl.SwipeArg -> SwipeGroup(values, field.description, onValueChange)
        VisualControl.VersionCode -> VersionCodeGroup(values, field.description, onValueChange)
        VisualControl.VersionName -> VersionNameGroup(values, field.description, onValueChange)
        else -> Unit
    }
}

/** `Position`：六个分量共用一个输入框，分段切换当前编辑的分量。 */
@Composable
private fun PositionGroup(
    values: VisualValues,
    @StringRes description: Int?,
    onValueChange: (String, String) -> Unit,
) {
    var mode by remember { mutableStateOf(VisualSubFields.position.first()) }
    val key = RuleVisualDraft.positionKey(mode)
    CollapsibleGroup(GkIcons.CenterFocusWeak, R.string.rule_visual_section_position) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChipRows(
                labels = VisualSubFields.position,
                selectedIndex = VisualSubFields.position.indexOf(mode),
                onSelect = { mode = VisualSubFields.position[it] },
            )
            if (description != null) {
                Text(
                    text = stringResource(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EditorTextField(
                value = values[key],
                onValueChange = { onValueChange(key, it) },
                singleLine = true,
                monospace = true,
            )
        }
    }
}

/** `SwipeArg`：起点/终点、六个分量与 `duration` 共用一个输入框。 */
@Composable
private fun SwipeGroup(
    values: VisualValues,
    @StringRes description: Int?,
    onValueChange: (String, String) -> Unit,
) {
    // 选中的分量；null 表示正在编辑 duration。
    var isStart by remember { mutableStateOf(true) }
    var mode by remember { mutableStateOf<String?>(VisualSubFields.position.first()) }
    val isDuration = mode == null
    val key = when (val current = mode) {
        null -> RuleVisualDraft.SwipeDuration
        else ->
            if (isStart) RuleVisualDraft.swipeStartKey(current) else RuleVisualDraft.swipeEndKey(current)
    }
    CollapsibleGroup(GkIcons.AutoMode, R.string.rule_visual_section_swipe) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 起点/终点、分量与 duration 排在同一片区域，组间用细竖线分隔。
            ChipRows(
                listOf(
                    ChipGroup(
                        labels = listOf("start", "end"),
                        // 编辑 duration 时两级都不高亮，避免同时出现三个选中项。
                        selectedIndex = when {
                            isDuration -> -1
                            isStart -> 0
                            else -> 1
                        },
                        // 编辑 duration 时切回起终点，自动落到第一个分量。
                        onSelect = {
                            isStart = it == 0
                            if (mode == null) mode = VisualSubFields.position.first()
                        },
                    ),
                    ChipGroup(
                        labels = VisualSubFields.position,
                        // duration 为选中项时分量不高亮。
                        selectedIndex = mode?.let { VisualSubFields.position.indexOf(it) } ?: -1,
                        onSelect = { mode = VisualSubFields.position[it] },
                    ),
                    ChipGroup(
                        labels = listOf(VisualSubFields.duration),
                        selectedIndex = if (isDuration) 0 else -1,
                        onSelect = { mode = null },
                    ),
                ),
            )
            if (description != null) {
                Text(
                    text = stringResource(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EditorTextField(
                value = values[key],
                onValueChange = { onValueChange(key, it) },
                singleLine = true,
                monospace = !isDuration,
                number = isDuration,
            )
        }
    }
}

/** 合并输入框的一个分段：键名 + 该分段的输入形态。 */
private data class MatcherMode(
    val name: String,
    val multiline: Boolean = false,
    val number: Boolean = false,
)

/** 一组互斥标签及其选中项。 */
private data class ChipGroup(
    val labels: List<String>,
    val selectedIndex: Int,
    val onSelect: (Int) -> Unit,
)

/**
 * 分段标签：用 [FlowRow] 自动换行，每个标签按自身内容取宽。
 * 不做等分压缩，长字段名不会被截断；放不下时自然落到下一行。
 */
@Composable
private fun ChipRows(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) = ChipRows(listOf(ChipGroup(labels, selectedIndex, onSelect)))

/**
 * 多组互斥标签排在同一片区域内：组间以细竖线分隔，组内共用一次换行。
 * 用于「起点/终点」这类前置切换与「分量」这类次级切换并列显示的场景。
 */
@Composable
private fun ChipRows(groups: List<ChipGroup>) {
    val all = groups.filter { it.labels.isNotEmpty() }
    if (all.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        all.forEachIndexed { groupIndex, group ->
            if (groupIndex > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
            group.labels.forEachIndexed { index, label ->
                TriChip(
                    text = label,
                    selected = group.selectedIndex == index,
                    onClick = { group.onSelect(index) },
                )
            }
        }
    }
}

/**
 * 嵌套匹配器：全部分量共用一个输入框，分段切换当前编辑的分量。
 * `minimum`/`maximum` 等数字分量用单行数字键盘，`include`/`exclude` 等列表用多行。
 */
@Composable
private fun MatcherGroup(
    icon: ImageVector,
    @StringRes title: Int,
    values: VisualValues,
    modes: List<MatcherMode>,
    keyOf: (String) -> String,
    @StringRes description: Int?,
    onValueChange: (String, String) -> Unit,
) {
    var mode by remember { mutableStateOf(modes.first()) }
    val key = keyOf(mode.name)
    CollapsibleGroup(icon, title) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChipRows(
                labels = modes.map { it.name },
                selectedIndex = modes.indexOf(mode),
                onSelect = { mode = modes[it] },
            )
            // 标题已说明整组含义时不再重复同一句说明。
            if (description != null && description != title) {
                Text(
                    text = stringResource(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EditorTextField(
                value = values[key],
                onValueChange = { onValueChange(key, it) },
                singleLine = !mode.multiline,
                monospace = !mode.number,
                number = mode.number,
            )
        }
    }
}

/** `IntegerMatcher`：minimum / maximum / include / exclude。 */
@Composable
private fun VersionCodeGroup(
    values: VisualValues,
    @StringRes description: Int?,
    onValueChange: (String, String) -> Unit,
) {
    MatcherGroup(
        icon = GkIcons.Layers,
        title = R.string.rule_help_p_version_code,
        values = values,
        modes = versionCodeModes,
        keyOf = RuleVisualDraft::versionCodeKey,
        description = description,
        onValueChange = onValueChange,
    )
}

/** `StringMatcher`：pattern / include / exclude。 */
@Composable
private fun VersionNameGroup(
    values: VisualValues,
    @StringRes description: Int?,
    onValueChange: (String, String) -> Unit,
) {
    MatcherGroup(
        icon = GkIcons.TextFields,
        title = R.string.rule_help_p_version_name,
        values = values,
        modes = versionNameModes,
        keyOf = RuleVisualDraft::versionNameKey,
        description = description,
        onValueChange = onValueChange,
    )
}

private val versionCodeModes = listOf(
    MatcherMode("minimum", number = true),
    MatcherMode("maximum", number = true),
    MatcherMode("include", multiline = true),
    MatcherMode("exclude", multiline = true),
)

private val versionNameModes = listOf(
    MatcherMode("pattern"),
    MatcherMode("include", multiline = true),
    MatcherMode("exclude", multiline = true),
)

/**
 * 扁平可折叠分组：标题行可点击，默认收起，箭头随状态旋转；
 * 展开后内容缩进并带左侧强调线。
 */
@Composable
private fun CollapsibleGroup(
    icon: ImageVector,
    @StringRes title: Int,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "chevron",
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = null,
                    indication = ripple(),
                ) { expanded = !expanded }
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GkIcon(
                imageVector = icon,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.tertiary,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            GkIcon(
                imageVector = GkIcons.ExpandMore,
                modifier = Modifier.size(18.dp).rotate(chevronRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = null,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(200)) + fadeIn(tween(150)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(120)),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(MaterialTheme.colorScheme.tertiary),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        }
    }
}

/** 分区图标；作用范围按全局/应用目标区分。 */
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

private fun sectionDescription(section: VisualSection, target: VisualTarget): Int = when (section) {
    VisualSection.Identity -> R.string.rule_visual_desc_identity
    VisualSection.Target ->
        if (target == VisualTarget.GlobalGroup) R.string.rule_visual_desc_target_global
        else R.string.rule_visual_desc_target_app

    VisualSection.Timing -> R.string.rule_visual_desc_timing
    VisualSection.Links -> R.string.rule_visual_desc_links
    VisualSection.Rules -> R.string.rule_visual_desc_rule
}

/** 子分组图标。 */
private fun subGroupIcon(subGroup: VisualSubGroup): ImageVector = when (subGroup) {
    VisualSubGroup.ActionTiming -> GkIcons.Schedule
    VisualSubGroup.AppScope -> GkIcons.Apps
    VisualSubGroup.RefLinks -> GkIcons.Link
}

/** 目标类型标签。 */
@Composable
private fun targetLabel(target: VisualTarget): String = when (target) {
    VisualTarget.GlobalGroup -> stringResource(R.string.global_rules)
    VisualTarget.AppGroup -> stringResource(R.string.app_rules)
    VisualTarget.NewApp -> stringResource(R.string.rule_visual_target_new_app)
}

/** 字段名按 JSON 属性名显示；规则级的 "rule." 前缀只用于表单区分，不显示。 */
private fun fieldLabel(key: String) = key.removePrefix("rule.")

private val actionOptions: List<Pair<String, Int>> by lazy {
    RuleVisualAction.entries.map { it.value to it.labelResource }
}

private val resetMatchOptions: List<Pair<String, Int>> by lazy {
    RuleVisualResetMatch.entries.map { it.value to it.labelResource }
}
