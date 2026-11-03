package li.gkd.app.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import li.gkd.app.resources.*
import org.jetbrains.compose.resources.stringResource

val LocalEditorWindowInsets = staticCompositionLocalOf<WindowInsets?> { null }

@Composable
fun GkEditorLayout(
    title: @Composable () -> Unit,
    saveEnabled: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    saveBeforeMenu: Boolean = false,
    menuContent: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val saveButton: @Composable () -> Unit = {
        GkIconButton(
            GkIcons.Save,
            contentDescription = stringResource(Res.string.action_save),
            enabled = saveEnabled,
            onClick = onSave,
        )
    }
    GkScaffold(
        contentWindowInsets =
            LocalEditorWindowInsets.current ?: ScaffoldDefaults.contentWindowInsets,
        topBar = {
            GkTopAppBar(
                title = title,
                navigationIcon = {
                    GkIconButton(
                        GkIcons.Close,
                        contentDescription = stringResource(Res.string.dialog_close),
                        onClick = onClose,
                    )
                },
                actions = {
                    if (saveBeforeMenu) {
                        saveButton()
                    }
                    if (menuContent != null) {
                        menuContent()
                    }
                    if (!saveBeforeMenu) {
                        saveButton()
                    }
                },
            )
        },
        content = content,
    )
}
