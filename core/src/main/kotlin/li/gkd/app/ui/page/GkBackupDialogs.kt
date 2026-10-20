package li.gkd.app.ui.page

import li.gkd.app.R

import androidx.compose.runtime.Composable
import li.gkd.app.resources.*
import li.gkd.app.ui.component.GkTextListDialog
import androidx.compose.ui.res.stringResource

enum class BackupDialog { Main, Export }

@Composable
fun GkBackupDialogs(
    dialog: BackupDialog?,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onShare: () -> Unit,
    onSaveDownloads: () -> Unit,
) {
    if (dialog == null) return
    GkTextListDialog(
        onDismiss = onDismiss,
        textList = when (dialog) {
            BackupDialog.Main -> listOf(
                stringResource(R.string.backup_import_label) to onImport,
                stringResource(R.string.backup_export) to onExport,
            )
            BackupDialog.Export -> listOf(
                stringResource(R.string.action_share) to onShare,
                stringResource(R.string.action_save_to_downloads) to onSaveDownloads,
            )
        },
    )
}
