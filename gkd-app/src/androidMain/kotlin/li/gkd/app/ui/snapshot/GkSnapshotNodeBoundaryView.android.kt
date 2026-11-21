package li.gkd.app.ui.snapshot

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.viewinterop.AndroidView
import li.gkd.app.snapshot.SnapshotNodeTree
import li.gkd.app.snapshot.ui.SnapshotOutlineView

@Composable
actual fun GkSnapshotNodeBoundaryView(
    tree: SnapshotNodeTree?,
    selectedId: Int?,
    bounds: Rect,
    modifier: Modifier,
) {
    var outlineView by remember { mutableStateOf<SnapshotOutlineView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            SnapshotOutlineView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                // Pointer input stays with the Compose image, so this layer is presentation only.
                isClickable = false
                outlineView = this
            }
        },
        onRelease = { released ->
            if (outlineView === released) outlineView = null
            released.setOutlineVisible(false)
        },
        update = { view ->
            if (outlineView == null) outlineView = view
            // Null tree means the lookup failed: drop the old bounds instead of showing stale ones.
            view.setNodes(
                tree?.snapshot?.nodes,
                tree?.snapshot?.screenWidth ?: 0,
                tree?.snapshot?.screenHeight ?: 0,
                selectedId,
            )
            view.setImageBounds(bounds.left, bounds.top, bounds.width, bounds.height)
            view.setOutlineVisible(tree != null)
        },
    )
}
