package li.gkd.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import li.gkd.app.R
import li.gkd.app.resources.*
import li.gkd.app.ui.component.DialogRequests
import li.gkd.app.ui.page.GkShareAppDialog
import li.gkd.app.ui.platform.UiHost
import li.gkd.app.ui.share.launchUiAction
import li.gkd.app.util.ApkUtils

@Composable
fun UiHost.GkShareAppDialog(
    visible: Boolean,
    dialogs: DialogRequests,
    scope: CoroutineScope,
    onOpenUrl: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = this
    if (visible) {
        GkShareAppDialog(
            onDismiss = onDismissRequest,
            onShare = scope.launchUiAction(Dispatchers.IO) {
                context.shareFile(ApkUtils.createShareFile(), R.string.apk_share.getSync())
            },
            onSave = scope.launchUiAction(Dispatchers.IO) {
                context.saveFileToDownloads(ApkUtils.createShareFile())
            },
        )
    }
}
