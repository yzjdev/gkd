package li.gkd.app.ui.platform

import li.gkd.app.R

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import li.gkd.app.permission.PermissionStates
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.resources.*
import li.gkd.app.util.IntentUtils
import li.gkd.app.util.ToastUtils

@Composable
fun privilegeAvailable() =
    privilegeContextFlow.collectAsStateWithLifecycle().value != null

@Composable
fun ignoresBatteryOptimizations() =
    PermissionStates.ignoreBatteryOptimizations.stateFlow.collectAsStateWithLifecycle().value

@Composable
fun RefreshPermissions() {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            withContext(Dispatchers.IO) {
                while (isActive) {
                    PermissionStates.refreshAll(); delay(1000)
                }
            }
        }
    }
}

fun openAppDetails() {
    IntentUtils.openAppDetailsSettings()
}

fun openRecents() {
    val context = privilegeContextFlow.value
    if (context?.keyevent(android.view.KeyEvent.KEYCODE_APP_SWITCH) != true) {
        ToastUtils.show(R.string.recents_lock_manual_hint.getSync())
    }
}

fun openA11ySettings() {
    IntentUtils.openA11ySettings()
}

fun switchAutomator() {
    li.gkd.app.service.switchAutomatorService()
}

fun dynamicColorAvailable() = li.gkd.app.util.AndroidTarget.S
