package li.gkd.app.ui.settings

import li.gkd.app.R

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import li.gkd.app.resources.*
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.home.scopeAppSource
import li.gkd.app.app.AppSort
import li.gkd.app.ui.state.BaseViewModel
import li.gkd.app.util.ToastUtils

class A11yScopeAppListViewModel : BaseViewModel() {
    val editorState = AppListEditorState()

    val searchStr: StateFlow<String>
        field = MutableStateFlow("")

    fun setSearchStr(value: String) {
        searchStr.value = value
    }

    val uiState = scopeAppSource.stateLoadable()
    private val editor = AppIdListEditor(
        { SettingsRepository.a11yScopeAppList.value },
        SettingsRepository::replaceA11yScopeAppList
    )

    suspend fun showHelp() {
        MainViewModel.requireCurrent().dialogRequests.showMessage(
            title = getString(R.string.a11y_scoped),
            text = getString(R.string.scoped_a11y_help_problem) +
                getString(R.string.scoped_a11y_help_behavior) +
                getString(R.string.scoped_a11y_help_scope),
        )
    }

    fun startEditing() {
        editorState.closeSearch(::setSearchStr)
        editorState.startEditing(editor.initialText())
    }

    suspend fun closeEditor() {
        if (editor.hasChanges(editorState.state.value.draft)) {
            val confirmed = MainViewModel.requireCurrent().dialogRequests.confirm(
                title = getString(R.string.notice_title),
                text = getString(R.string.edit_discard_confirmation),
            )
            if (!confirmed) return
        }
        editorState.closeEditor()
    }

    suspend fun onBack() {
        if (editorState.state.value.editing) closeEditor()
        else MainViewModel.requireCurrent().navigator.pop()
    }

    suspend fun saveEditor() {
        val draft = editorState.state.value.draft
        val changed = editor.save(draft)
        ToastUtils.show(getString(if (changed) R.string.update_success else R.string.unchanged))
        if (editorState.state.value.draft == draft) editorState.closeEditor()
    }

    fun setSortType(value: AppSort) {
        SettingsRepository.updateSettings { it.copy(a11yScopeAppSort = value.value) }
    }

    fun setAppGroupType(value: Int) {
        SettingsRepository.updateSettings { it.copy(a11yScopeAppGroupType = value) }
    }

    fun toggleApp(appId: String) {
        SettingsRepository.updateA11yScopeAppList { if (appId in it) it - appId else it + appId }
    }
}
