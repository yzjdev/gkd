package li.gkd.app.model

import kotlinx.serialization.Serializable

/** 快照节点的屏幕边界，从节点 attr 的坐标字段解析，用于在截图上叠加显示。 */
@Serializable
data class SnapshotNodeBounds(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    val isEmpty: Boolean
        get() = right <= left || bottom <= top

    val area: Int
        get() = (right - left).coerceAtLeast(0) * (bottom - top).coerceAtLeast(0)

    fun contains(x: Float, y: Float): Boolean =
        x >= left && x <= right && y >= top && y <= bottom
}
