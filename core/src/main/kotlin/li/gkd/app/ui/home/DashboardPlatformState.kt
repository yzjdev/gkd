package li.gkd.app.ui.home

import li.gkd.app.R

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import li.gkd.app.a11y.useA11yServiceEnabledFlow
import li.gkd.app.permission.AppPermissionRestriction
import li.gkd.app.permission.PermissionStates
import li.gkd.app.priv.PrivilegeCapabilities
import li.gkd.app.priv.PrivilegeServiceStatus
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.priv.privilegeServiceStatusFlow
import li.gkd.app.priv.uiAutomationFlow
import li.gkd.app.resources.*
import li.gkd.app.service.A11yService
import li.gkd.app.service.ActivityService
import li.gkd.app.service.StatusService
import li.gkd.app.service.a11yPartDisabledFlow
import li.gkd.app.service.topAppIdFlow
import li.gkd.app.settings.SettingsStore
import li.gkd.app.ui.navigation.AppRoute
import li.gkd.app.ui.navigation.PrivilegeServiceRoute
import li.gkd.app.ui.navigation.WorkModeRoute
import androidx.annotation.StringRes

/** Values supplied by Android services or the Desktop Android simulator. */
data class DashboardPlatformState(
    val a11yRunning: Boolean,
    val automationRunning: Boolean,
    val a11yEnabled: Boolean,
    val writeSecureSettings: Boolean,
    val partiallyDisabled: Boolean,
    val privilegeAvailable: Boolean,
    val privilegeStatus: DashboardPrivilegeStatus,
    val statusRunning: Boolean,
    val activityRunning: Boolean,
    val restricted: Boolean,
    val topAppId: String,
    val privilegeCapabilities: PrivilegeCapabilities? = null,
    val appRestrictions: Set<AppPermissionRestriction> = emptySet(),
) {
    fun usesA11y(store: SettingsStore, scope: Set<String>) =
        store.useA11y || (store.useAutomation && topAppId in scope)

    fun serviceEnabled(store: SettingsStore, scope: Set<String>) =
        if (usesA11y(store, scope)) a11yRunning else automationRunning

    fun subtitle(store: SettingsStore, scope: Set<String>): Int =
        if (usesA11y(store, scope)) {
            when {
                a11yRunning -> R.string.a11y_running
                a11yEnabled -> R.string.a11y_fault
                !writeSecureSettings -> R.string.a11y_unauthorized
                store.enableAutomator && partiallyDisabled -> R.string.a11y_partially_disabled
                else -> R.string.a11y_stopped
            }
        } else {
            when {
                automationRunning -> R.string.automation_running
                !privilegeAvailable -> R.string.automation_unauthorized
                store.enableAutomator && partiallyDisabled -> R.string.automation_partially_disabled
                else -> R.string.automation_stopped
            }
        }

    fun authorizationRoute(enable: Boolean, store: SettingsStore, scope: Set<String>): AppRoute? =
        when {
            enable && usesA11y(store, scope) && !writeSecureSettings -> WorkModeRoute
            enable && !usesA11y(store, scope) &&
                    (!privilegeAvailable || privilegeCapabilities?.restricted == true) -> PrivilegeServiceRoute
            else -> null
        }
}

@androidx.compose.runtime.Composable
fun dashboardPlatformState(): DashboardPlatformState {
    val privilege by privilegeContextFlow.collectAsStateWithLifecycle()
    val privilegeStatus by privilegeServiceStatusFlow.collectAsStateWithLifecycle()
    val capabilities by PermissionStates.privilegeCapabilities.collectAsStateWithLifecycle()
    val a11y by A11yService.isRunning.collectAsStateWithLifecycle()
    val automation by uiAutomationFlow.collectAsStateWithLifecycle()
    val status by StatusService.isRunning.collectAsStateWithLifecycle()
    val secure by PermissionStates.writeSecureSettings.stateFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val enabled = remember { useA11yServiceEnabledFlow(scope) }
    val a11yEnabled by enabled.collectAsStateWithLifecycle()
    val partial by a11yPartDisabledFlow.collectAsStateWithLifecycle()
    val topApp by topAppIdFlow.collectAsStateWithLifecycle()
    val restrictions by PermissionStates.appRestrictionsFlow.collectAsStateWithLifecycle()
    val activity by ActivityService.isRunning.collectAsStateWithLifecycle()
    return DashboardPlatformState(
        a11y, automation != null, a11yEnabled, secure, partial, privilege != null,
        when (privilegeStatus) {
            PrivilegeServiceStatus.Connected -> DashboardPrivilegeStatus.Connected
            PrivilegeServiceStatus.DisconnectedDesired -> DashboardPrivilegeStatus.DisconnectedDesired
            PrivilegeServiceStatus.Disconnected -> DashboardPrivilegeStatus.Disconnected
        },
        status, activity, restrictions.isNotEmpty(), topApp, capabilities, restrictions
    )
}
