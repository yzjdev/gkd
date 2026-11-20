package li.gkd.app.ai

import li.gkd.app.model.AttrInfo
import li.gkd.app.model.ComplexSnapshot
import li.gkd.app.model.NodeInfo
import li.gkd.app.snapshot.SnapshotNodeTree

/** Keeps the request small enough for a single chat completion. */
private const val MaxPromptNodes = 600
private const val MaxPromptChars = 24_000
private const val MaxValueLength = 120

/**
 * Renders the selected snapshots as chat context. The user's question follows separately, so this
 * only describes the pages and their node trees.
 */
fun List<ComplexSnapshot>.toChatContext(): String = buildString {
    appendLine("以下是用户选中的快照，请基于这些节点回答后续问题。")
    this@toChatContext.forEachIndexed { index, snapshot ->
        appendLine()
        appendLine("### 快照 ${index + 1}")
        append(snapshot.toPageDescription())
        appendLine("节点树:")
        append(snapshot.toNodeTreeText())
    }
}

/** Page facts shared by rule generation and chat. */
private fun ComplexSnapshot.toPageDescription(): String = buildString {
    appendLine("应用包名: $appId")
    appInfo?.name?.let { appendLine("应用名称: $it") }
    activityId?.takeIf { it.isNotBlank() }?.let { appendLine("Activity: $it") }
    appendLine(
        "屏幕: ${screenWidth}x$screenHeight" + if (isLandscape) " (横屏)" else ""
    )
    appendLine("快照 ID: $id")
}

/** Depth-first node listing, truncated to keep one request affordable. */
private fun ComplexSnapshot.toNodeTreeText(): String {
    val tree = SnapshotNodeTree(this)
    return buildString {
        var printed = 0
        val stack = ArrayDeque<Pair<NodeInfo, Int>>()
        tree.roots.asReversed().forEach { stack.addLast(it to 0) }
        while (stack.isNotEmpty()) {
            val (node, depth) = stack.removeLast()
            if (printed >= MaxPromptNodes || length >= MaxPromptChars) {
                appendLine("…（节点过多，已截断）")
                break
            }
            appendLine(renderNode(node, depth))
            printed++
            tree.children[node.id]?.asReversed()?.forEach { stack.addLast(it to depth + 1) }
        }
    }
}

/**
 * Renders one snapshot as the user message: page facts first, then the node tree the model must
 * select from. Only attributes the selector engine understands are printed.
 */
fun ComplexSnapshot.toRulePrompt(goal: String): String {
    val snapshot = this
    return buildString {
        append(snapshot.toPageDescription())
        if (goal.isNotBlank()) {
            appendLine()
            appendLine("需求: $goal")
        }
        appendLine()
        appendLine("节点树（缩进表示层级，方括号内为可用属性）:")
        append(snapshot.toNodeTreeText())
        appendLine()
        appendLine("请根据上述节点生成匹配该需求的规则组。")
    }
}

private fun renderNode(node: NodeInfo, depth: Int): String {
    val attr = node.attr
    val parts = buildList {
        attr.id?.takeIf(String::isNotEmpty)?.let { add("id=${it.quote()}") }
        attr.vid?.takeIf(String::isNotEmpty)?.let { add("vid=${it.quote()}") }
        attr.text?.takeIf(String::isNotEmpty)?.let { add("text=${it.trimmed().quote()}") }
        attr.desc?.takeIf(String::isNotEmpty)?.let { add("desc=${it.trimmed().quote()}") }
        attr.name?.takeIf(String::isNotEmpty)?.let { add("name=${it.trimmed().quote()}") }
        attr.flags().forEach { add(it) }
    }
    return "  ".repeat(depth) + "#${node.id} " + parts.joinToString(" ")
}

/** Booleans and geometry only when they can narrow the selector. */
private fun AttrInfo.flags(): List<String> = buildList {
    if (clickable) add("clickable=true")
    if (longClickable) add("longClickable=true")
    if (editable) add("editable=true")
    if (checkable) add("checkable=true")
    if (checked == true) add("checked=true")
    if (!visibleToUser) add("visibleToUser=false")
    if (childCount > 0) add("childCount=$childCount")
}

private fun String.trimmed(): String =
    replace('\n', ' ').trim().let { if (it.length > MaxValueLength) it.take(MaxValueLength) + "…" else it }

private fun String.quote(): String = "\"" + replace("\"", "\\\"") + "\""
