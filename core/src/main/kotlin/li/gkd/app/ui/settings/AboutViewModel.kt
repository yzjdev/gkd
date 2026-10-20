package li.gkd.app.ui.settings

import li.gkd.app.R

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import li.gkd.app.ui.state.BaseViewModel

class AboutViewModel : BaseViewModel() {
    val showVersionInfoDialog: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun setShowVersionInfoDialog(value: Boolean) {
        showVersionInfoDialog.value = value
    }

    val showShareAppDialog: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun setShowShareAppDialog(value: Boolean) {
        showShareAppDialog.value = value
    }
}
