package li.gkd.app.ui.snapshot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import li.gkd.app.model.SnapshotNodeBounds

private val NodeBoundsColor = Color(0xFF22D3EE)
private val NodeHighlightColor = Color(0xFFFFB300)
private val NodeHighlightFillColor = Color(0x33FFB300)

/**
 * 在快照截图之上叠加节点边界。
 *
 * 该视图由预览放进图片实际绘制区域，坐标按屏幕像素等比映射，[showBounds] 决定是否绘制。
 * 点击时命中包含该点的最小节点并高亮。
 */
@Composable
fun GkSnapshotBoundsOverlay(
    showBounds: Boolean,
    nodes: List<SnapshotNodeBounds>,
    screenWidth: Int,
    screenHeight: Int,
    modifier: Modifier = Modifier,
) {
    if (!showBounds) return
    var selectedIndex by remember(nodes) { mutableIntStateOf(-1) }
    Canvas(
        modifier =
            modifier.fillMaxSize().pointerInput(nodes, screenWidth, screenHeight) {
                detectTapGestures { tap ->
                    if (size.width <= 0 || size.height <= 0) return@detectTapGestures
                    val x = tap.x / size.width * screenWidth
                    val y = tap.y / size.height * screenHeight
                    selectedIndex =
                        nodes.indices
                            .filter { nodes[it].contains(x, y) }
                            .minByOrNull { nodes[it].area }
                            ?: -1
                }
            },
    ) {
        val scaleX = size.width / screenWidth.coerceAtLeast(1)
        val scaleY = size.height / screenHeight.coerceAtLeast(1)
        val outline = Stroke(width = 1.5.dp.toPx())
        val highlightOutline = Stroke(width = 2.5.dp.toPx())
        nodes.forEachIndexed { index, node ->
            val width = (node.right - node.left) * scaleX
            val height = (node.bottom - node.top) * scaleY
            if (width <= 0f || height <= 0f) return@forEachIndexed
            val topLeft = Offset(node.left * scaleX, node.top * scaleY)
            val rectSize = Size(width, height)
            val selected = index == selectedIndex
            if (selected) {
                drawRect(color = NodeHighlightFillColor, topLeft = topLeft, size = rectSize)
            }
            drawRect(
                color = if (selected) NodeHighlightColor else NodeBoundsColor,
                topLeft = topLeft,
                size = rectSize,
                style = if (selected) highlightOutline else outline,
            )
        }
    }
}
