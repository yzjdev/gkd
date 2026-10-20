package li.gkd.app.settings

import li.gkd.app.R

import li.gkd.app.META
import li.gkd.app.util.DefaultBlockedApps

fun defaultSettings() = SettingsStore(
    actionToast = META.appName,
    customNotifTitle = META.appName,
    updateChannel = if (META.isBeta) 1 else 0,
)

fun defaultBlockMatchAppList(): Set<String> = DefaultBlockedApps.query()

fun defaultTermsAccepted(): Boolean = false
