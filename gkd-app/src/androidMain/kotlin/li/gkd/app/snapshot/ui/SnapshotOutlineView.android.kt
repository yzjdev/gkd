package li.gkd.app.snapshot.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import li.gkd.app.model.NodeInfo
import li.gkd.app.ui.snapshot.mergeSnapshotOutline

/**
 * Draws the merged node outline of one snapshot image plus the selected node rectangle.
 *
 * Node data lives in the view, so [setNodes] only rebuilds the path when the data changes and
 * [setOutlineVisible] toggles painting without touching it. The view fills its parent; the image
 * rectangle is supplied by the Compose zoomable state so nodes follow pan and zoom.
 */
@SuppressLint("ViewConstructor")
class SnapshotOutlineView(
    context: Context,
) : View(context) {
    private val outlinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.RED
        }
    private val selectionFillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.RED
            alpha = 41
        }
    private val selectionStrokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.RED
        }
    private val outline = Path()

    private var nodes: List<NodeInfo> = emptyList()
    private var screenWidth = 0
    private var screenHeight = 0
    private var imageLeft = 0f
    private var imageTop = 0f
    private var imageWidth = 0f
    private var imageHeight = 0f
    private var outlineVisible = false
    private var selectedId: Int? = null
    private var selectedNode: NodeInfo? = null

    init {
        setWillNotDraw(false)
        val density = resources.displayMetrics.density
        outlinePaint.strokeWidth = 1f * density
        selectionStrokePaint.strokeWidth = 2f * density
    }

    /**
     * Node input for one snapshot. Pass a null [list] when the snapshot has no node information;
     * the previous outline is dropped but the visibility flag is kept.
     */
    fun setNodes(
        list: List<NodeInfo>?,
        screenWidth: Int,
        screenHeight: Int,
        selectedId: Int?,
    ) {
        val next = list.orEmpty()
        val nodesChanged = nodes != next
        val screenChanged = this.screenWidth != screenWidth || this.screenHeight != screenHeight
        val selectionChanged = this.selectedId != selectedId
        if (!nodesChanged && !screenChanged && !selectionChanged) return
        nodes = next
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
        this.selectedId = selectedId
        selectedNode = next.firstOrNull { it.id == selectedId }?.takeIf { it.attr.hasArea }
        rebuildPath()
        invalidate()
    }

    /**
     * Image rectangle inside this view, un-clipped so the caller may pass bounds larger than the
     * window. Off-image nodes are clipped in [onDraw].
     */
    fun setImageBounds(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
    ) {
        if (imageLeft == left && imageTop == top && imageWidth == width && imageHeight == height) {
            return
        }
        imageLeft = left
        imageTop = top
        imageWidth = width
        imageHeight = height
        invalidate()
    }

    /** Single switch for showing or hiding all node bounds, including the selection highlight. */
    fun setOutlineVisible(visible: Boolean) {
        if (outlineVisible == visible) return
        outlineVisible = visible
        invalidate()
    }

    private fun rebuildPath() {
        outline.reset()
        if (screenWidth <= 0 || screenHeight <= 0) return
        for (segment in mergeSnapshotOutline(nodes)) {
            outline.moveTo(segment.x1.toFloat(), segment.y1.toFloat())
            outline.lineTo(segment.x2.toFloat(), segment.y2.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!outlineVisible) return
        if (screenWidth <= 0 || screenHeight <= 0 || imageWidth <= 0f || imageHeight <= 0f) return
        val sx = imageWidth / screenWidth
        val sy = imageHeight / screenHeight
        canvas.save()
        canvas.translate(imageLeft, imageTop)
        canvas.scale(sx, sy)
        canvas.clipRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat())
        outlinePaint.strokeWidth = resources.displayMetrics.density / sx
        canvas.drawPath(outline, outlinePaint)
        selectedNode?.let { node ->
            val attr = node.attr
            val left = attr.left.toFloat()
            val top = attr.top.toFloat()
            val right = attr.right.toFloat()
            val bottom = attr.bottom.toFloat()
            canvas.drawRect(left, top, right, bottom, selectionFillPaint)
            selectionStrokePaint.strokeWidth = 2f * resources.displayMetrics.density / sx
            canvas.drawRect(left, top, right, bottom, selectionStrokePaint)
        }
        canvas.restore()
    }
}
