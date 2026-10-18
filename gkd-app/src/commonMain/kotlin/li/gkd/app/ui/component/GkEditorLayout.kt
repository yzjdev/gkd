package li.gkd.app.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import li.gkd.app.resources.*
import org.jetbrains.compose.resources.stringResource

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
                        contentDescription = stringResource(Res.string.dialog_close),
                        onClick = onClose
                    )
                },
                actions = {
                    if (menuActions != null) menuActions()
                    GkIconButton(
                        GkIcons.Check, contentDescription = stringResource(Res.string.action_save),
                        enabled = saveEnabled, onClick = onSave
                    )
                },
            )
        },
        content = content,
    )
}
