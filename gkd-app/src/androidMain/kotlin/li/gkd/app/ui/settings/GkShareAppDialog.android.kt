package li.gkd.app.ui.settings

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import li.gkd.app.resources.*
import li.gkd.app.ui.page.GkShareAppDialog
import li.gkd.app.ui.platform.UiHost
import li.gkd.app.ui.share.launchUiAction
import li.gkd.app.util.ApkUtils

@Composable
actual fun UiHost.GkShareAppDialog(
    visible: Boolean,
    scope: CoroutineScope,
    onDismissRequest: () -> Unit,
) {
    val context = this
    if (visible) {
        GkShareAppDialog(
            onDismiss = onDismissRequest,
            onShare =
                scope.launchUiAction(Dispatchers.IO) {
                    context.shareFile(ApkUtils.createShareFile(), Res.string.apk_share.getSync())
                },
            onSave =
                scope.launchUiAction(Dispatchers.IO) {
                    context.saveFileToDownloads(ApkUtils.createShareFile())
                },
        )
    }
}
