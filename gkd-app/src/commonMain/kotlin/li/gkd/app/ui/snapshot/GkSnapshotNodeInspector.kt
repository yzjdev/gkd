package li.gkd.app.ui.snapshot

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json
import li.gkd.app.model.NodeInfo
import li.gkd.app.resources.*
import li.gkd.app.snapshot.SnapshotNodeRelations
import li.gkd.app.snapshot.SnapshotNodeTree
import li.gkd.app.ui.component.GkDialog
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkSizedIconButton
import li.gkd.app.ui.component.GkTooltipIconButtonBox
import li.gkd.app.util.copyText
import org.jetbrains.compose.resources.stringResource

private val NodeDialogButtonSize = 32.dp
private val NodeDialogIconSize = 18.dp
private val NodeDialogAvatarSize = 36.dp
private val NodeDialogRowHeight = 26.dp

/** One inspected node property; [quickFind] marks values usable by quick find selectors. */
private data class SnapshotNodeProperty(
    val name: String,
    val value: Any?,
    val quickFind: Boolean = false,
)

private const val PropertyNameWeight = .42f

@Composable
fun GkSnapshotNodePropertiesDialog(
    tree: SnapshotNodeTree,
    node: NodeInfo,
    onSelect: (Int) -> Unit,
    onShowValue: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val relations = tree.relations(node)
    val attr = node.attr
    val propertyScrollState = rememberScrollState()
    val idQuickFind = node.idQf == true
    val textQuickFind = node.textQf == true
    val identityLines =
        remember(node) {
            listOf(
                listOf(SnapshotNodeProperty("name", attr.name)),
                listOf(
                    SnapshotNodeProperty(
                        "id",
                        attr.id,
                        quickFind = idQuickFind && !attr.id.isNullOrEmpty(),
                    ),
                ),
                listOf(
                    SnapshotNodeProperty(
                        "vid",
                        attr.vid,
                        quickFind = idQuickFind && !attr.vid.isNullOrEmpty(),
                    ),
                ),
                listOf(
                    SnapshotNodeProperty(
                        "text",
                        attr.text,
                        quickFind = textQuickFind && !attr.text.isNullOrEmpty(),
                    ),
                ),
                listOf(SnapshotNodeProperty("desc", attr.desc)),
            )
        }
    val boundsLines =
        remember(node) {
            listOf(
                listOf(
                    SnapshotNodeProperty("left", attr.left),
                    SnapshotNodeProperty("top", attr.top),
                    SnapshotNodeProperty("right", attr.right),
                    SnapshotNodeProperty("bottom", attr.bottom),
                ),
                listOf(
                    SnapshotNodeProperty("width", attr.width),
                    SnapshotNodeProperty("height", attr.height),
                ),
            )
        }
    val flagLines =
        remember(node) {
            listOf(
                listOf(
                    SnapshotNodeProperty("clickable", attr.clickable),
                    SnapshotNodeProperty("longClickable", attr.longClickable),
                ),
                listOf(SnapshotNodeProperty("visibleToUser", attr.visibleToUser)),
                listOf(
                    SnapshotNodeProperty("checkable", attr.checkable),
                    SnapshotNodeProperty("checked", attr.checked),
                ),
                listOf(
                    SnapshotNodeProperty("editable", attr.editable),
                    SnapshotNodeProperty("focusable", attr.focusable),
                ),
            )
        }
    val treeLines =
        remember(node) {
            listOf(
                listOf(
                    SnapshotNodeProperty("childCount", attr.childCount),
                    SnapshotNodeProperty("index", attr.index),
                    SnapshotNodeProperty("depth", attr.depth),
                ),
            )
        }
    val groups =
        listOf(
            stringResource(Res.string.snapshot_node_group_identity) to identityLines,
            stringResource(Res.string.snapshot_node_group_bounds) to boundsLines,
            stringResource(Res.string.snapshot_node_group_flags) to flagLines,
            stringResource(Res.string.snapshot_node_group_tree) to treeLines,
        )
    GkDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 3.dp,
        ) {
            Column(Modifier.heightIn(max = 560.dp)) {
                NodeDialogHeader(node, onDismiss)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SelectionContainer(Modifier.weight(1f, fill = false)) {
                    Column(Modifier.fillMaxWidth().verticalScroll(propertyScrollState)) {
                        groups.forEachIndexed { index, (title, lines) ->
                            if (index > 0) {
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            }
                            NodeDialogGroup(title, lines, onShowValue)
                        }
                        Spacer(Modifier.size(6.dp))
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                NodeDialogRelations(relations, node, groups, onSelect)
            }
        }
    }
}

/** Avatar, node name and the identity chips of the inspected node. */
@Composable
private fun NodeDialogHeader(node: NodeInfo, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val attr = node.attr
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(NodeDialogAvatarSize)
                .clip(CircleShape)
                .background(colors.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            GkIcon(
                GkIcons.NetworkNode,
                modifier = Modifier.size(20.dp),
                tint = colors.onSecondaryContainer,
                contentDescription = null,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text =
                    attr.name?.takeIf(String::isNotEmpty)?.substringAfterLast('.')
                        ?: stringResource(Res.string.snapshot_node_inspect),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "#${node.id}",
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
                if (attr.depth > 0) {
                    NodeDialogChip("depth ${attr.depth}", colors.onSurfaceVariant)
                }
                if (attr.childCount > 0) {
                    NodeDialogChip("${attr.childCount} child", colors.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        GkSizedIconButton(
            imageVector = GkIcons.Close,
            size = NodeDialogButtonSize,
            iconSize = NodeDialogIconSize,
            contentDescription = stringResource(Res.string.action_close),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun NodeDialogChip(text: String, color: Color) {
    Text(
        text = text,
        modifier =
            Modifier.clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        style =
            MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        color = color,
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
private fun NodeDialogGroup(
    title: String,
    lines: List<List<SnapshotNodeProperty>>,
    onShowValue: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = .6.sp,
                ),
            color = MaterialTheme.colorScheme.primary,
        )
        lines.forEachIndexed { index, line ->
            SnapshotNodePropertyLine(line, striped = index % 2 == 1, onShowValue = onShowValue)
        }
    }
}

/** Puts two or more short properties on one row and leaves a single property full width. */
@Composable
private fun SnapshotNodePropertyLine(
    line: List<SnapshotNodeProperty>,
    striped: Boolean,
    onShowValue: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .then(
                if (striped) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .22f))
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 1.dp),
    ) {
        line.forEachIndexed { index, property ->
            if (index > 0) Spacer(Modifier.width(12.dp))
            SnapshotNodePropertyRow(property, Modifier.weight(1f), onShowValue)
        }
    }
}

@Composable
private fun SnapshotNodePropertyRow(
    property: SnapshotNodeProperty,
    modifier: Modifier,
    onShowValue: (String) -> Unit,
) {
    val style =
        MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    val value = property.value
    val valueText = if (value is String) Json.encodeToString(value) else value.toString()
    var overflowed by remember(valueText) { mutableStateOf(false) }
    Row(
        modifier.heightIn(min = NodeDialogRowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            property.name,
            Modifier.weight(1f - PropertyNameWeight),
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (property.quickFind) {
            val hint = stringResource(Res.string.snapshot_node_property_quick_find)
            GkTooltipIconButtonBox(hint, showOnClick = true) {
                GkIcon(
                    GkIcons.Check,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = hint,
                )
            }
        }
        val openValue =
            if (overflowed) {
                Modifier.clickable(
                    onClickLabel = stringResource(Res.string.snapshot_node_show_full_value),
                    onClick = { onShowValue(value.toString()) },
                )
            } else Modifier
        Text(
            valueText,
            Modifier.weight(PropertyNameWeight).then(openValue),
            style = style,
            color =
                when {
                    overflowed -> MaterialTheme.colorScheme.primary
                    value == null ->
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { overflowed = it.hasVisualOverflow },
        )
    }
}

/** Relation jumps and property copy, laid out as a labelled action row. */
@Composable
private fun NodeDialogRelations(
    relations: SnapshotNodeRelations,
    node: NodeInfo,
    groups: List<Pair<String, List<List<SnapshotNodeProperty>>>>,
    onSelect: (Int) -> Unit,
) {
    val actions =
        listOf(
            Triple(
                GkIcons.ArrowUpward,
                Res.string.snapshot_node_parent,
                relations.parentId,
            ),
            Triple(
                GkIcons.ArrowBack,
                Res.string.snapshot_node_before_sibling,
                relations.previousSiblingId,
            ),
            Triple(
                GkIcons.ArrowForward,
                Res.string.snapshot_node_after_sibling,
                relations.nextSiblingId,
            ),
            Triple(
                GkIcons.ArrowDownward,
                Res.string.snapshot_node_first_child,
                relations.firstChildId,
            ),
        )
    Row(
        Modifier.fillMaxWidth().padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { (icon, label, target) ->
            Row(
                Modifier.weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = target != null) { target?.let(onSelect) }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                GkIcon(
                    icon,
                    modifier = Modifier.size(NodeDialogIconSize),
                    tint =
                        if (target != null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .3f)
                        },
                    contentDescription = null,
                )
            }
        }
        GkSizedIconButton(
            imageVector = GkIcons.ContentCopy,
            size = NodeDialogButtonSize,
            iconSize = NodeDialogIconSize,
            contentDescription = stringResource(Res.string.snapshot_node_copy),
            onClick = {
                copyText(
                    buildString {
                        append("#${node.id}  parent=${node.pid}")
                        groups.forEach { (_, lines) ->
                            lines.forEach { line ->
                                append('\n')
                                append(line.joinToString("  ") { "${it.name}=${it.value}" })
                            }
                        }
                    },
                )
            },
        )
    }
}

