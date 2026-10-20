package li.gkd.app.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import li.gkd.app.R
import li.gkd.app.ui.component.GkTextListDialog

@Composable
fun GkShareAppDialog(
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    GkTextListDialog(
        onDismiss = onDismiss, textList = listOf(
            stringResource(R.string.action_share) to onShare,
            stringResource(R.string.action_save_to_downloads) to onSave,
        )
    )
}
