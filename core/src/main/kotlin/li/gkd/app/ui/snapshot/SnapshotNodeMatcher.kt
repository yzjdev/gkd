package li.gkd.app.ui.snapshot

import li.gkd.app.R

import li.gkd.app.model.NodeInfo
import li.gkd.selector.NodeAdapter
import li.gkd.selector.Selector
import li.gkd.selector.SelectorCompileResult
import li.gkd.selector.SelectorTypeResult
import li.gkd.selector.createDefaultSelectorTypeModel

/** 节点语法输入的校验与匹配结果。 */
internal sealed interface NodeSelectorResult {
    /** 输入为空，不做任何提示。 */
    data object Empty : NodeSelectorResult

    /** 正在后台匹配，界面据此显示进行中状态。 */
    data object Matching : NodeSelectorResult

    /** 语法非法或属性不受支持，[message] 直接展示给用户。 */
    data class Invalid(val message: String) : NodeSelectorResult

    /** 语法合法但当前快照没有节点符合。 */
    data object NoMatch : NodeSelectorResult

    /** 语法合法且命中了 [ids] 对应的节点。 */
    data class Matched(val ids: Set<Int>) : NodeSelectorResult
}

/** 复用同一棵节点树的索引；语法生成会反复校验，不能每次都重建适配器。 */
internal class NodeSelectorMatcher(val nodes: List<NodeInfo>) {
    val byId: Map<Int, NodeInfo> by lazy { nodes.associateBy { it.id } }
    private val adapter by lazy { NodeInfoAdapter(nodes) }

    /** 命中集合；语法非法或没有命中时返回 null。 */
    fun matchedIds(source: String): Set<Int>? =
        (evaluate(source) as? NodeSelectorResult.Matched)?.ids

    fun evaluate(source: String): NodeSelectorResult {
        val text = source.trim()
        if (text.isEmpty()) return NodeSelectorResult.Empty
        if (nodes.isEmpty()) return NodeSelectorResult.NoMatch
        val selector = when (val result = Selector.compile(text)) {
            is SelectorCompileResult.Success -> result.value
            is SelectorCompileResult.Failure ->
                return NodeSelectorResult.Invalid(result.error.message.orEmpty())
        }
        val typeFailure = selector.validateType(createDefaultSelectorTypeModel())
        if (typeFailure is SelectorTypeResult.Failure) {
            return NodeSelectorResult.Invalid(typeFailure.error.message.orEmpty())
        }
        val ids = nodes.mapNotNullTo(mutableSetOf()) { node ->
            node.id.takeIf { selector.match(node, adapter) != null }
        }
        return if (ids.isEmpty()) NodeSelectorResult.NoMatch else NodeSelectorResult.Matched(ids)
    }
}

/**
 * 把快照节点列表适配成选择器可遍历的树。
 * 节点只有 `pid` 与 `index`，因此先按父节点分组并保持原始子节点顺序。
 */
private class NodeInfoAdapter(nodes: List<NodeInfo>) : NodeAdapter<NodeInfo>() {
    private val byId = nodes.associateBy { it.id }
    private val childrenByParent = nodes
        .filter { it.pid >= 0 && byId.containsKey(it.pid) }
        .groupBy { it.pid }
        .mapValues { (_, children) -> children.sortedBy { it.attr.index } }
    private val rootNode = nodes.firstOrNull { it.pid < 0 || !byId.containsKey(it.pid) }

    private fun childrenOf(node: NodeInfo): List<NodeInfo> = childrenByParent[node.id].orEmpty()

    override fun getAttr(target: Any, name: String): Any? = when (target) {
        is NodeInfo -> when (name) {
            "id" -> target.attr.id
            "vid" -> target.attr.vid
            "name" -> target.attr.name
            "text" -> target.attr.text
            "desc" -> target.attr.desc

            "clickable" -> target.attr.clickable
            "focusable" -> target.attr.focusable
            "checkable" -> target.attr.checkable
            "checked" -> target.attr.checked
            "editable" -> target.attr.editable
            "longClickable" -> target.attr.longClickable
            "visibleToUser" -> target.attr.visibleToUser

            "left" -> target.attr.left
            "top" -> target.attr.top
            "right" -> target.attr.right
            "bottom" -> target.attr.bottom
            "width" -> target.attr.width
            "height" -> target.attr.height

            "childCount" -> target.attr.childCount
            "index" -> target.attr.index
            "depth" -> target.attr.depth

            "parent" -> byId[target.pid]
            else -> null
        }

        else -> null
    }

    override fun getInvoke(target: Any, name: String, args: List<Any>): Any? = when (target) {
        is NodeInfo -> when (name) {
            "getChild" -> (args.firstOrNull() as? Number)?.toInt()?.let { childrenOf(target).getOrNull(it) }
            else -> null
        }

        else -> null
    }

    override fun getName(node: NodeInfo): String? = node.attr.name

    override fun getChildCount(node: NodeInfo): Int = childrenOf(node).size

    override fun getChild(node: NodeInfo, index: Int): NodeInfo? = childrenOf(node).getOrNull(index)

    override fun getParent(node: NodeInfo): NodeInfo? = byId[node.pid]

    override fun getNodeKey(node: NodeInfo): Any = node.id

    override fun getRoot(node: NodeInfo): NodeInfo? = rootNode
}
