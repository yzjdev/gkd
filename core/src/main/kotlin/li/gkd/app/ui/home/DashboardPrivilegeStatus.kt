package li.gkd.app.ui.home

import li.gkd.app.R

import li.gkd.app.resources.*
import androidx.annotation.StringRes

enum class DashboardPrivilegeStatus(val labelResource: Int) {
    Disconnected(R.string.privilege_service_state_disconnected),
    Connected(R.string.privilege_service_state_connected),
    DisconnectedDesired(R.string.privilege_service_state_lost),
}
