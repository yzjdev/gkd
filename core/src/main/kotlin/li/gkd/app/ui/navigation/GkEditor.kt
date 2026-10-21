package li.gkd.app.ui.navigation

import li.gkd.app.R

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import li.gkd.app.ui.page.EditorActionsState
import androidx.compose.runtime.remember
import li.gkd.app.ui.component.GkEditorDialogs
import li.gkd.app.ui.component.GkEditorLayout
import li.gkd.app.ui.page.EditorSession
import li.gkd.app.ui.platform.GkBackHandler

@Composable
fun GkEditor(
    editor: EditorSession,
    onSaved: (() -> Unit)? = null,
    navigator: AppNavigator,
    hideIme: () -> Boolean,
    actions: EditorActionsState,
    content: @Composable (PaddingValues) -> Unit,
) {
    val entry = remember { navigator.topEntry }
    val dialog by actions.dialog.collectAsStateWithLifecycle()
    val close: () -> Unit = { if (navigator.topEntry === entry) navigator.pop() }
    val requestClose: () -> Unit = { actions.requestClose(editor.hasChanges, { hideIme() }, close) }
    // 页面有内部层级时先回退一层，退无可退才走关闭流程。
    val onBack: () -> Unit = { if (!editor.onBack()) requestClose() }
    val save: () -> Unit = {
        actions.save(editor.onSave, { hideIme() }) {
            if (navigator.topEntry === entry) {
                if (onSaved != null) onSaved() else navigator.pop()
            }
        }
    }
    GkBackHandler(onBack = onBack)
    GkEditorLayout(
        title = editor.titleContent,
        saveEnabled = editor.saveEnabled,
        onClose = requestClose,
        onSave = save,
        menuActions = editor.menuActions,
        navigationIcon = editor.navigationIcon,
        content = content,
    )
    GkEditorDialogs(dialog, actions::dismissDialog, close)
}
