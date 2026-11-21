package li.gkd.app.ui.snapshot

import li.gkd.app.model.NodeInfo

/** One axis-aligned segment of the merged node outline, in snapshot pixel coordinates. */
data class SnapshotOutlineSegment(val x1: Int, val y1: Int, val x2: Int, val y2: Int)

/**
 * Merges the borders of all nodes with an area into non-overlapping horizontal and vertical
 * segments, so adjacent nodes that share an edge render one line instead of a grid.
 */
fun mergeSnapshotOutline(nodes: List<NodeInfo>): List<SnapshotOutlineSegment> {
    val horizontal = mutableMapOf<Int, MutableList<IntRange>>()
    val vertical = mutableMapOf<Int, MutableList<IntRange>>()
    for (node in nodes) {
        val attr = node.attr
        if (!attr.hasArea) continue
        val x = attr.left..attr.right
        val y = attr.top..attr.bottom
        horizontal.getOrPut(y.first) { mutableListOf() }.add(x)
        horizontal.getOrPut(y.last) { mutableListOf() }.add(x)
        vertical.getOrPut(x.first) { mutableListOf() }.add(y)
        vertical.getOrPut(x.last) { mutableListOf() }.add(y)
    }
    return buildList {
        fun merge(groups: Map<Int, MutableList<IntRange>>, horizontalEdge: Boolean) {
            for ((coordinate, ranges) in groups) {
                ranges.sortBy { it.first }
                var start = ranges.first().first
                var end = ranges.first().last
                fun emit() {
                    add(
                        if (horizontalEdge)
                            SnapshotOutlineSegment(start, coordinate, end, coordinate)
                        else SnapshotOutlineSegment(coordinate, start, coordinate, end),
                    )
                }
                for (i in 1 until ranges.size) {
                    val range = ranges[i]
                    if (range.first > end) {
                        emit()
                        start = range.first
                        end = range.last
                    } else {
                        end = maxOf(end, range.last)
                    }
                }
                emit()
            }
        }
        merge(horizontal, horizontalEdge = true)
        merge(vertical, horizontalEdge = false)
    }
}
