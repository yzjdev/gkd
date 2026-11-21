package li.gkd.app.ui.snapshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import li.gkd.app.snapshot.SnapshotNodeTree

/**
 * Hosts the platform node-boundary renderer over one snapshot image.
 *
 * Compose owns composition, gestures and visuals only; the renderer keeps the node data and is
 * toggled through its own visibility method.
 */
@Composable
expect fun GkSnapshotNodeBoundaryView(
    tree: SnapshotNodeTree?,
    selectedId: Int?,
    bounds: Rect,
    modifier: Modifier = Modifier,
)
