package li.gkd.app.ui.snapshot

import li.gkd.app.R

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import li.gkd.app.model.ComplexSnapshot
import li.gkd.app.model.NodeInfo
import li.gkd.app.resources.*
import li.gkd.app.ui.component.GkCopyableText
import li.gkd.app.ui.component.GkDialog
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.util.copyText
import li.songe.json5.Json5
import li.songe.json5.Json5EncoderConfig
import li.songe.json5.Json5QuoteStrategy
import androidx.compose.ui.res.stringResource

private val nodeInfoJson = Json {
    prettyPrint = true
    encodeDefaults = true
    explicitNulls = true
}

private val nodeBoundsStroke = Color(0xCC00E5FF)
// 选中节点用琥珀色，匹配节点用洋红，两者色相相差大，重叠时也能分辨。
private val selectedNodeFill = Color(0x59FFC107)
private val selectedNodeStroke = Color(0xFFFFC107)
private val matchedNodeFill = Color(0x4DFF4080)
private val matchedNodeStroke = Color(0xFFFF4080)

/** Node bounds are captured in screen pixels; the screenshot is the same full screen. */
private class NodeViewport(
    val scaleX: Float,
    val scaleY: Float,
    val offsetX: Float,
    val offsetY: Float,
)

private fun nodeViewport(
    snapshot: ComplexSnapshot,
    intrinsic: Size,
    canvas: Size,
): NodeViewport? {
    if (snapshot.screenWidth <= 0 || snapshot.screenHeight <= 0) return null
    if (intrinsic.width <= 0f || intrinsic.height <= 0f) return null
    val scale = minOf(canvas.width / intrinsic.width, canvas.height / intrinsic.height)
    if (scale <= 0f) return null
    val drawnWidth = intrinsic.width * scale
    val drawnHeight = intrinsic.height * scale
    return NodeViewport(
        // A replacement screenshot may differ from the captured size; keep both axes independent.
        scaleX = drawnWidth / snapshot.screenWidth,
        scaleY = drawnHeight / snapshot.screenHeight,
        offsetX = (canvas.width - drawnWidth) / 2f,
        offsetY = (canvas.height - drawnHeight) / 2f,
    )
}

/**
 * 命中点击位置的最小节点：面积最小者优先，面积相同再取层级更深的。
 * 同一层级可能有多个重叠节点，只按 depth 取会选到较大的那个。
 */
private fun hitTestNode(
    nodes: List<NodeInfo>,
    viewport: NodeViewport,
    tap: Offset,
): NodeInfo? {
    var match: NodeInfo? = null
    var matchArea = Long.MAX_VALUE
    nodes.forEach { node ->
        val attr = node.attr
        if (!attr.visibleToUser || attr.width <= 0 || attr.height <= 0) return@forEach
        val left = attr.left * viewport.scaleX + viewport.offsetX
        val top = attr.top * viewport.scaleY + viewport.offsetY
        val right = attr.right * viewport.scaleX + viewport.offsetX
        val bottom = attr.bottom * viewport.scaleY + viewport.offsetY
        if (tap.x < left || tap.x > right || tap.y < top || tap.y > bottom) return@forEach
        val area = attr.width.toLong() * attr.height
        val current = match
        val smaller = area < matchArea ||
                (area == matchArea && current != null && attr.depth > current.attr.depth)
        if (current == null || smaller) {
            match = node
            matchArea = area
        }
    }
    return match
}

/** Direction of the neighbour lookup used by the node dialog arrows. */
private enum class NodeDirection { Up, Down, Left, Right }

/**
 * 按节点树结构定位方向目标：
 * 上 = 父级节点，下 = 子级节点，左/右 = 上一个/下一个兄弟节点。
 * 与屏幕上看到的位置无关，同屏重叠的容器与内部节点也能各自进出。
 */
private fun findNodeInDirection(
    nodes: List<NodeInfo>,
    from: NodeInfo,
    direction: NodeDirection,
): NodeInfo? = when (direction) {
    NodeDirection.Up -> nodes.firstOrNull { it.id == from.pid }
    NodeDirection.Down -> visibleChildren(nodes, from.id).minByOrNull { it.attr.index }
    NodeDirection.Left -> siblingOf(nodes, from, forward = false)
    NodeDirection.Right -> siblingOf(nodes, from, forward = true)
}

/** 直接子节点中可见且占据画面的那些。 */
private fun visibleChildren(nodes: List<NodeInfo>, parentId: Int): List<NodeInfo> =
    nodes.filter { node ->
        node.pid == parentId && node.attr.visibleToUser &&
                node.attr.width > 0 && node.attr.height > 0
    }

/** 同一父级下，[from] 之前或之后最近的可见兄弟节点。 */
private fun siblingOf(
    nodes: List<NodeInfo>,
    from: NodeInfo,
    forward: Boolean,
): NodeInfo? {
    val candidates = nodes.filter { node ->
        node.pid == from.pid && node.id != from.id && node.attr.visibleToUser &&
                if (forward) node.attr.index > from.attr.index else node.attr.index < from.attr.index
    }
    return if (forward) {
        candidates.minByOrNull { it.attr.index }
    } else {
        candidates.maxByOrNull { it.attr.index }
    }
}

/**
 * Snapshot image whose taps resolve the captured node under the finger.
 * Node bounds are only drawn in node-inspection mode; outside it the image stays clean and a
 * tap on empty space still toggles the host bars.
 */
@Composable
fun GkSnapshotNodeView(
    imageLoader: ImageLoader,
    imagePath: String,
    snapshot: ComplexSnapshot?,
    inspecting: Boolean,
    onToggleBars: () -> Unit = {},
) {
    val context = LocalPlatformContext.current
    val painter = rememberAsyncImagePainter(
        model = remember(imagePath) {
            ImageRequest.Builder(context)
                .data(imagePath)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        },
        imageLoader = imageLoader,
    )
    val intrinsicSize = painter.intrinsicSize
    var selected by remember(snapshot?.id) { mutableStateOf<NodeInfo?>(null) }
    var showNodeInfo by remember(snapshot?.id) { mutableStateOf(false) }
    val nodes = snapshot?.nodes.orEmpty()
    val density = LocalDensity.current
    // 节点树最多保留 5000 个节点，索引与遍历都放后台，避免点击节点后主线程卡顿。
    val matcher = remember(nodes) { NodeSelectorMatcher(nodes) }
    // 语法输入与匹配结果：输入变化即时重新校验，命中的节点在画面上全部高亮。
    var syntaxInput by remember(snapshot?.id) { mutableStateOf("") }
    val matchResult by produceState<NodeSelectorResult>(
        initialValue = NodeSelectorResult.Empty,
        syntaxInput,
        matcher,
    ) {
        value = if (syntaxInput.isBlank()) {
            NodeSelectorResult.Empty
        } else {
            // 先给出进行中状态，再在后台算结果，避免界面显示上一次的旧结论。
            value = NodeSelectorResult.Matching
            withContext(Dispatchers.Default) { matcher.evaluate(syntaxInput) }
        }
    }
    val matchedIds = (matchResult as? NodeSelectorResult.Matched)?.ids.orEmpty()
    // 搜索框：按文本/描述/类名/id 模糊匹配，命中的节点同样高亮。
    var searchInput by remember(snapshot?.id) { mutableStateOf("") }
    val searchIds = remember(searchInput, nodes) { searchNodes(nodes, searchInput) }
    val searching = searchInput.isNotBlank()
    val highlightedIds = matchedIds + searchIds
    // 退出查看模式时清空选中与语法，避免下次进入时残留高亮。
    LaunchedEffect(inspecting) {
        if (!inspecting) {
            selected = null
            showNodeInfo = false
            syntaxInput = ""
            searchInput = ""
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(snapshot?.id, intrinsicSize, nodes, inspecting) {
                    detectTapGestures { tap ->
                        // Outside node-inspection mode a tap keeps the plain preview behavior.
                        if (!inspecting) {
                            onToggleBars()
                            return@detectTapGestures
                        }
                        val viewport = snapshot?.let {
                            nodeViewport(
                                snapshot = it,
                                intrinsic = intrinsicSize,
                                canvas = Size(size.width.toFloat(), size.height.toFloat()),
                            )
                        }
                        val hit = viewport?.let { hitTestNode(nodes, it, tap) }
                        selected = hit
                        showNodeInfo = hit != null
                        if (hit == null) onToggleBars()
                    }
                },
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
            )
            if (inspecting) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val current = snapshot ?: return@Canvas
                    val viewport = nodeViewport(current, intrinsicSize, size) ?: return@Canvas
                    val stroke = with(density) { 1.dp.toPx() }
                    nodes.forEach { node ->
                        val attr = node.attr
                        if (!attr.visibleToUser || attr.width <= 0 || attr.height <= 0) return@forEach
                        val topLeft = Offset(
                            attr.left * viewport.scaleX + viewport.offsetX,
                            attr.top * viewport.scaleY + viewport.offsetY,
                        )
                        val nodeSize = Size(
                            attr.width * viewport.scaleX,
                            attr.height * viewport.scaleY,
                        )
                        // 搜索期间只呈现搜索结果，点击产生的选中高亮不再绘制。
                        val isSelected = node.id == selected?.id && !searching
                        val isMatched = node.id in highlightedIds
                        // 先铺匹配色，再叠选中色：同一节点既匹配又选中时两种标记都保留。
                        if (isMatched) {
                            drawRect(matchedNodeFill, topLeft, nodeSize)
                            drawRect(
                                matchedNodeStroke,
                                topLeft,
                                nodeSize,
                                style = Stroke(width = stroke * 2),
                            )
                        }
                        if (isSelected) {
                            drawRect(selectedNodeFill, topLeft, nodeSize)
                            drawRect(
                                selectedNodeStroke,
                                topLeft,
                                nodeSize,
                                style = Stroke(width = stroke * 4),
                            )
                        } else if (!isMatched) {
                            drawRect(nodeBoundsStroke, topLeft, nodeSize, style = Stroke(width = stroke))
                        }
                    }
                }
            }
        }

        if (inspecting && !showNodeInfo && !searching && selected == null) {
            Text(
                text = stringResource(R.string.snapshot_node_bounds_hint),
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // 查看模式下点中节点后弹出面板，搜索与节点详情都在对话框中。
        val infoNode = selected?.takeIf { inspecting && showNodeInfo }
        if (infoNode != null) {
            // 显示完整 activity 名，过长时由文本自身省略开头、保留可区分的结尾。
            val activityId = snapshot?.activityId?.takeIf(String::isNotBlank)
            // 选中节点变化时用自动生成的语法初始化输入；生成要反复遍历节点树，放后台执行。
            LaunchedEffect(infoNode.id, nodes) {
                syntaxInput = ""
                syntaxInput = withContext(Dispatchers.Default) {
                    nodeSelectorSyntax(infoNode, matcher)
                }
            }
            SnapshotNodeInfoDialog(
                activityId = activityId,
                node = infoNode,
                nodes = nodes,
                syntaxInput = syntaxInput,
                onSyntaxChange = { syntaxInput = it },
                matchResult = matchResult,
                searchInput = searchInput,
                onSearchChange = { searchInput = it },
                searchIds = searchIds,
                onSelectNode = { item ->
                    // 定位后收起搜索结果，回到常规的选中高亮与节点详情。
                    selected = item
                    searchInput = ""
                    showNodeInfo = true
                },
                onMove = { direction ->
                    findNodeInDirection(nodes, infoNode, direction)?.let { selected = it }
                },
                onDismiss = {
                    // 只收起面板，保留选中节点与命中高亮，回到画面上继续查看。
                    showNodeInfo = false
                },
            )
        }
    }
}

/** 节点信息面板：搜索框、生成语法与节点属性都在同一个对话框里。 */
@Composable
private fun SnapshotNodeInfoDialog(
    activityId: String?,
    node: NodeInfo,
    nodes: List<NodeInfo>,
    syntaxInput: String,
    onSyntaxChange: (String) -> Unit,
    matchResult: NodeSelectorResult,
    searchInput: String,
    onSearchChange: (String) -> Unit,
    searchIds: Set<Int>,
    onSelectNode: (NodeInfo) -> Unit,
    onMove: (NodeDirection) -> Unit,
    onDismiss: () -> Unit,
) {
    val json = remember(node) {
        runCatching { nodeInfoJson.encodeToString(NodeInfo.serializer(), node) }.getOrDefault("")
    }
    var showProperties by remember(node) { mutableStateOf(false) }
    val searching = searchInput.isNotBlank()
    val results = remember(searchIds, nodes) { nodes.filter { it.id in searchIds } }
    GkDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = attrSummary(node),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = activityId ?: stringResource(R.string.value_null),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            // 完整名通常前缀相同，隐藏开头保留能区分的结尾。
                            overflow = TextOverflow.StartEllipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (activityId != null) {
                            GkIconButton(
                                imageVector = GkIcons.ContentCopy,
                                onClick = { copyText(activityId) },
                                modifier = Modifier.size(28.dp),
                                contentDescription = stringResource(R.string.text_copy),
                            )
                        }
                    }
                }
                NodeSearchField(
                    value = searchInput,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (searching) {
                        item {
                            Text(
                                text = if (searchIds.isEmpty()) {
                                    stringResource(R.string.snapshot_node_search_empty)
                                } else {
                                    stringResource(
                                        R.string.snapshot_node_search_matched,
                                        searchIds.size.toString(),
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (searchIds.isEmpty()) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        // 命中结果直接列出，点一项即选中该节点并在画面上定位。
                        items(results, key = { it.id }) { item ->
                            SearchResultRow(node = item, onClick = { onSelectNode(item) })
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NodePanelTab(
                                text = stringResource(R.string.snapshot_node_generate_syntax),
                                selected = !showProperties,
                                modifier = Modifier.weight(1f),
                                onClick = { showProperties = false },
                            )
                            NodePanelTab(
                                text = stringResource(R.string.snapshot_node_show_properties),
                                selected = showProperties,
                                modifier = Modifier.weight(1f),
                                onClick = { showProperties = true },
                            )
                        }
                    }
                    if (showProperties) {
                        if (json.isNotEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    GkCopyableText(
                                        text = AnnotatedString(json),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(12.dp),
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        onCopy = { copyText(it) },
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            NodeSyntaxField(
                                value = syntaxInput,
                                onValueChange = onSyntaxChange,
                                isError = matchResult is NodeSelectorResult.Invalid,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        // 匹配结果常显：数量、进行中与错误提示共用同一行，避免布局跳动。
                        item {
                            val result = matchResult
                            val errorColor = MaterialTheme.colorScheme.error
                            val normalColor = MaterialTheme.colorScheme.onSurfaceVariant
                            val failed = result is NodeSelectorResult.Invalid ||
                                    result is NodeSelectorResult.NoMatch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (result is NodeSelectorResult.Matching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = normalColor,
                                    )
                                }
                                Text(
                                    text = when (result) {
                                        is NodeSelectorResult.Invalid -> result.message
                                        NodeSelectorResult.NoMatch -> stringResource(
                                            R.string.snapshot_node_syntax_no_match,
                                        )

                                        NodeSelectorResult.Matching -> stringResource(
                                            R.string.snapshot_node_syntax_matching,
                                        )

                                        is NodeSelectorResult.Matched -> stringResource(
                                            R.string.snapshot_node_syntax_matched,
                                            result.ids.size.toString(),
                                        )

                                        NodeSelectorResult.Empty -> stringResource(
                                            R.string.snapshot_node_syntax_matched,
                                            "0",
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (failed) errorColor else normalColor,
                                )
                            }
                        }
                    }
                    // 方向按钮放在语法输入框下方，切换节点时语法与高亮同步更新。
                    item {
                        SnapshotNodeDirectionRow(
                            enabled = { direction -> findNodeInDirection(nodes, node, direction) != null },
                            onMove = onMove,
                        )
                    }
                }
            }
        }
    }
}

/** 无边框胶囊搜索框：圆角底色加前后图标，避免在卡片里再叠一层描边。 */
@Composable
private fun NodeSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .heightIn(min = 44.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GkIcon(
            imageVector = GkIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.snapshot_node_search_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.replace("\n", "")) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                interactionSource = interactionSource,
            )
        }
        if (value.isNotEmpty()) {
            GkIconButton(
                imageVector = GkIcons.Close,
                onClick = { onValueChange("") },
                modifier = Modifier.size(32.dp),
                contentDescription = stringResource(R.string.action_clear),
            )
        }
    }
}

/**
 * 单行语法输入框：等宽字体、无描边，语法过长时横向滚动而不是换行。
 * 校验失败时整条底色与边框转为错误色。
 */
@Composable
private fun NodeSyntaxField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val accentColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .heightIn(min = 44.dp)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "{}",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            color = accentColor,
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.snapshot_node_syntax_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.replace("\n", "")) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                interactionSource = interactionSource,
            )
        }
    }
}

/** 搜索结果的一行：类名 + 文本/描述，点击定位到该节点。 */
@Composable
private fun SearchResultRow(
    node: NodeInfo,
    onClick: () -> Unit,
) {
    val attr = node.attr
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = attrSummary(node).ifEmpty { attr.name ?: node.id.toString() },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = listOfNotNull(attr.name, "depth=${attr.depth}")
                .joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 面板内的文本标签页，选中项高亮。 */
@Composable
private fun NodePanelTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 一层节点条件，例如 `[vid="2kz"][text="标题"]`。 */
private typealias NodeCondition = List<Pair<String, String>>

/** 关系运算符：`>` 只匹配直接父级，`>n` 允许跳过中间层。 */
private const val RELATION_PARENT = ">"
private const val RELATION_ANCESTOR = ">n"

/** 一条候选语法：条件链加上渲染用的关系运算符。 */
private class SyntaxChain(val groups: List<NodeCondition>, val relation: String) {
    val text: String get() = renderNodeChain(groups, relation)
}

/**
 * 生成只匹配 [node] 的节点语法。
 *
 * 两种关系都要试：`>n` 允许跳过中间层，写法短；`>` 逐层固定父级，约束更紧。
 * 只用 `>n` 时深层、同构兄弟多的节点可能永远收敛不到唯一，导致生成不出语法，
 * 因此 `>` 是必备的兜底，最后再放开到整棵树的深度。
 */
private fun nodeSelectorSyntax(node: NodeInfo, matcher: NodeSelectorMatcher): String {
    val nodes = matcher.nodes
    if (nodes.isEmpty()) return ""
    val byId = matcher.byId
    // 先试最省算力的 `>n`，成功就返回；常见节点只走这一轮，耗时与原先一致。
    bestChain(node, matcher, byId, RELATION_ANCESTOR, MAX_ANCESTOR_DEPTH)?.let { return it.text }
    // `>n` 收敛不到唯一时改用逐层固定父级的 `>`，约束更紧。
    bestChain(node, matcher, byId, RELATION_PARENT, MAX_ANCESTOR_DEPTH)?.let { return it.text }
    // 深层节点或同构兄弟很多时，常规深度内没有差异点，放开到整棵树的深度兜底。
    return bestChain(node, matcher, byId, RELATION_PARENT, nodes.size)?.text.orEmpty()
}

/** 用指定关系和深度各试一遍（是否带 index），取更短的一条。 */
private fun bestChain(
    node: NodeInfo,
    matcher: NodeSelectorMatcher,
    byId: Map<Int, NodeInfo>,
    relation: String,
    maxDepth: Int,
): SyntaxChain? {
    var best: SyntaxChain? = null
    for (allowIndex in listOf(false, true)) {
        val chain = buildChain(node, matcher, byId, allowIndex, maxDepth, relation) ?: continue
        val candidate = SyntaxChain(minimizeNodeChain(chain, node.id, matcher, relation), relation)
        if (best == null || isBetterChain(candidate, best)) best = candidate
    }
    return best
}

/** 自底向上拼出能唯一命中 [node] 的祖先链。 */
private fun buildChain(
    node: NodeInfo,
    matcher: NodeSelectorMatcher,
    byId: Map<Int, NodeInfo>,
    allowIndex: Boolean,
    maxDepth: Int,
    relation: String,
): List<NodeCondition>? {
    val self = accumulateCondition(node, matcher, allowIndex, node.id, relation = relation)
    if (self.unique) return listOf(self.condition)
    if (self.condition.isEmpty()) return null
    var suffix = listOf(self.condition)
    var ancestor = byId[node.pid]
    var depth = 0
    while (ancestor != null && depth < maxDepth) {
        val scoped = accumulateCondition(
            ancestor, matcher, allowIndex, node.id, suffix, relation,
        )
        if (scoped.unique) return listOf(scoped.condition) + suffix
        if (scoped.condition.isNotEmpty()) suffix = listOf(scoped.condition) + suffix
        ancestor = byId[ancestor.pid]
        depth++
    }
    return null
}

/** 层数少的优先，层数相同再比字面长度。 */
private fun isBetterChain(candidate: SyntaxChain, current: SyntaxChain): Boolean =
    candidate.groups.size < current.groups.size ||
            (candidate.groups.size == current.groups.size &&
                    candidate.text.length < current.text.length)

private class ConditionResult(val condition: NodeCondition, val unique: Boolean)

/** 在 [suffix] 之前逐个叠加 [node] 的属性，返回第一个整体唯一命中的组合。 */
private fun accumulateCondition(
    node: NodeInfo,
    matcher: NodeSelectorMatcher,
    allowIndex: Boolean,
    targetId: Int,
    suffix: List<NodeCondition> = emptyList(),
    relation: String = RELATION_ANCESTOR,
): ConditionResult {
    var parts: NodeCondition = emptyList()
    var best: NodeCondition = emptyList()
    for (attr in attributesOf(node, allowIndex)) {
        val attempt = parts + attr
        val ids = matcher.matchedIds(renderNodeChain(listOf(attempt) + suffix, relation))
            ?: continue
        // 目标节点必须仍在命中集合内，否则这条语法已经指向别的节点。
        if (targetId !in ids) continue
        parts = attempt
        best = parts
        if (ids.size == 1) return ConditionResult(best, unique = true)
    }
    return ConditionResult(best, unique = false)
}

/** 逐层、逐属性裁剪，直到再删任何一部分都不再唯一。 */
private fun minimizeNodeChain(
    chain: List<NodeCondition>,
    targetId: Int,
    matcher: NodeSelectorMatcher,
    relation: String,
): List<NodeCondition> {
    var current = chain
    var changed = true
    while (changed) {
        changed = false
        // 先尝试整层删除（最后一层是目标节点本身，必须保留）。
        var index = 0
        while (index < current.size - 1) {
            val candidate = current.toMutableList().apply { removeAt(index) }
            if (isUniqueTarget(candidate, targetId, matcher, relation)) {
                current = candidate
                changed = true
            } else {
                index++
            }
        }
        // 再尝试删单层里的属性，长属性优先删，得到更短的写法。
        for (groupIndex in current.indices) {
            var condition = current[groupIndex]
            if (condition.size <= 1) continue
            val ordered = condition.sortedByDescending { (key, value) -> key.length + value.length }
            for (attr in ordered) {
                if (condition.size <= 1) break
                val reduced = condition.filterNot { it == attr }
                if (reduced.isEmpty()) continue
                val candidate = current.toMutableList().apply { set(groupIndex, reduced) }
                if (isUniqueTarget(candidate, targetId, matcher, relation)) {
                    current = candidate
                    condition = reduced
                    changed = true
                }
            }
        }
    }
    return current
}

private fun isUniqueTarget(
    groups: List<NodeCondition>,
    targetId: Int,
    matcher: NodeSelectorMatcher,
    relation: String,
): Boolean {
    if (groups.isEmpty()) return false
    val ids = matcher.matchedIds(renderNodeChain(groups, relation)) ?: return false
    return ids.size == 1 && targetId in ids
}

private fun renderNodeChain(groups: List<NodeCondition>, relation: String): String =
    groups.joinToString(" $relation ") { condition ->
        condition.joinToString("") { (key, value) -> attributeSyntax(key, value) }
    }

/** 节点的可读特征，按区分能力从强到弱排列；[allowIndex] 时补上兄弟位置。 */
private fun attributesOf(node: NodeInfo, allowIndex: Boolean): List<Pair<String, String>> {
    val attr = node.attr
    return buildList {
        attr.vid?.takeIf { it.isNotBlank() }?.let { add("vid" to it) }
        attr.text?.takeIf { it.isNotBlank() }?.let { add("text" to it) }
        attr.desc?.takeIf { it.isNotBlank() }?.let { add("desc" to it) }
        // id 是「包名:id/vid」的全限定名，与 vid 重复，只在没有 vid 时使用。
        if (attr.vid.isNullOrBlank()) {
            attr.id?.takeIf { it.isNotBlank() }?.let { add("id" to it) }
        }
        attr.name?.takeIf { it.isNotBlank() }?.let { add("name" to it) }
        if (attr.clickable) add("clickable" to "true")
        if (attr.checkable) add("checkable" to "true")
        if (attr.checked == true) add("checked" to "true")
        if (attr.editable) add("editable" to "true")
        if (attr.focusable) add("focusable" to "true")
        if (attr.longClickable) add("longClickable" to "true")
        if (allowIndex) add("index" to attr.index.toString())
    }
}

/** 语法里的字符串统一用双引号：Json5 默认是单引号，这里显式指定。 */
private val syntaxStringConfig = Json5EncoderConfig(
    indent = "",
    quoteStrategy = Json5QuoteStrategy.Double,
    unquotedKey = false,
    trailingComma = false,
)

private fun attributeSyntax(key: String, rawValue: String): String {
    val value = if (rawValue == "true" || rawValue == "false" || rawValue.toIntOrNull() != null) {
        rawValue
    } else {
        Json5.encodeString(rawValue, syntaxStringConfig)
    }
    return "[$key=$value]"
}

/**
 * 按关键字搜索节点：文本、描述、类名、vid、id 任一包含关键字即命中。
 * 空白关键字视为未搜索，返回空集合，避免把整屏都点亮。
 */
private fun searchNodes(nodes: List<NodeInfo>, query: String): Set<Int> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return emptySet()
    return nodes.mapNotNullTo(mutableSetOf()) { node ->
        val attr = node.attr
        val hit = listOf(attr.text, attr.desc, attr.name, attr.vid, attr.id)
            .any { it?.contains(keyword, ignoreCase = true) == true }
        node.id.takeIf { hit }
    }
}

/**
 * 借用祖先特征的层数上限。列表里同构的行往往要到列表容器那一层才出现差异，
 * 实测常见差异点在 6 层左右，因此留出余量；再深则语法会冗长难读。
 */
private const val MAX_ANCESTOR_DEPTH = 8

/** 上下左右四个方向按钮；该方向没有相邻节点时按钮禁用。 */
@Composable
private fun SnapshotNodeDirectionRow(
    enabled: (NodeDirection) -> Boolean,
    onMove: (NodeDirection) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val buttons = listOf(
            Triple(NodeDirection.Up, GkIcons.ArrowUpward, R.string.snapshot_node_move_up),
            Triple(NodeDirection.Down, GkIcons.ArrowDownward, R.string.snapshot_node_move_down),
            Triple(NodeDirection.Left, GkIcons.ArrowBack, R.string.snapshot_node_move_left),
            Triple(NodeDirection.Right, GkIcons.ArrowForward, R.string.snapshot_node_move_right),
        )
        buttons.forEach { (direction, icon, label) ->
            val buttonEnabled = enabled(direction)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    GkIconButton(
                        imageVector = icon,
                        onClick = { onMove(direction) },
                        enabled = buttonEnabled,
                        contentDescription = stringResource(label),
                    )
                }
            }
        }
    }
}

/** One-line identity for the selected node. */
private fun attrSummary(node: NodeInfo): String {
    val attr = node.attr
    return listOfNotNull(
        attr.vid ?: attr.id,
        attr.name,
        attr.text,
        attr.desc,
    ).filter { it.isNotBlank() }.joinToString(" · ")
}
