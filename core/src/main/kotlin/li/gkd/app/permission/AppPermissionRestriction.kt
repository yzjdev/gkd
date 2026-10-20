package li.gkd.app.permission

import li.gkd.app.R

import li.gkd.app.resources.*
import androidx.annotation.StringRes

enum class AppPermissionRestriction(val descriptionResource: Int) {
    Accessibility(R.string.restriction_app_accessibility),
    RestrictedSettings(R.string.restriction_app_settings),
    ForegroundService(R.string.restriction_app_foreground),
}
