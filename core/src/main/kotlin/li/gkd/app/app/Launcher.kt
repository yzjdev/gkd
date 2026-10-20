package li.gkd.app.app

import li.gkd.app.R

import kotlinx.coroutines.flow.StateFlow

val launcherAppIdFlow: StateFlow<String>
    get() = li.gkd.app.a11y.launcherAppIdFlow
