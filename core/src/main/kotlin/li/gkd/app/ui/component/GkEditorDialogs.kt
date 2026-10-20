package li.gkd.app.ui.component

import li.gkd.app.R

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import li.gkd.app.error.messageResource
import li.gkd.app.resources.*
import li.gkd.app.ui.page.EditorDialog
import androidx.compose.ui.res.stringResource

@Composable
fun GkEditorDialogs(
    dialog: EditorDialog?,
    onDismissDialog: () -> Unit,
    onDiscard: () -> Unit,
) {
    when (dialog) {
        EditorDialog.Discard -> GkAlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text(stringResource(R.string.notice_title)) },
            text = { Text(stringResource(R.string.edit_discard_confirmation)) },
            confirmButton = {
                TextButton(onClick = { onDismissDialog(); onDiscard() }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDialog) { Text(stringResource(R.string.action_cancel)) }
            },
        )
        is EditorDialog.Error -> GkAlertDialog(
            onDismissRequest = onDismissDialog,
            text = { Text(dialog.cause.messageResource()) },
            confirmButton = {
                TextButton(onClick = onDismissDialog) { Text(stringResource(R.string.action_confirm)) }
            },
        )
        null -> Unit
    }
}
