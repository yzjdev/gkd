package li.gkd.app.ui.component

import li.gkd.app.R

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import li.gkd.app.resources.*
import androidx.compose.ui.res.stringResource

val LocalEditorWindowInsets = staticCompositionLocalOf<WindowInsets?> { null }

@Composable
fun GkEditorLayout(
    title: @Composable () -> Unit, saveEnabled: Boolean,
    onClose: () -> Unit, onSave: () -> Unit,
    menuActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    GkScaffold(
        contentWindowInsets = LocalEditorWindowInsets.current
            ?: ScaffoldDefaults.contentWindowInsets,
        topBar = {
            GkTopAppBar(
                title = title,
                navigationIcon = {
                    GkIconButton(
                        GkIcons.Close,
                        contentDescription = stringResource(R.string.dialog_close),
                        onClick = onClose
                    )
                },
                actions = {
                    if (menuActions != null) menuActions()
                    GkIconButton(
                        GkIcons.Check, contentDescription = stringResource(R.string.action_save),
                        enabled = saveEnabled, onClick = onSave
                    )
                },
            )
        },
        content = content,
    )
}
