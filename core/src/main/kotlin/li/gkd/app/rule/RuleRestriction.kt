package li.gkd.app.rule

import li.gkd.app.R

import li.gkd.app.error.ErrorMessage
import li.gkd.app.resources.*
import li.gkd.app.subscription.RuleValidationError
import androidx.annotation.StringRes

sealed interface RuleRestriction {
    val message: ErrorMessage
        get() = when (this) {
            is Invalid -> error?.message ?: ErrorMessage.Resource(R.string.rule_invalid_cannot_enable)
            AppNotApplicable -> ErrorMessage.Resource(R.string.rule_app_not_applicable)
            SubscriptionDisabled -> ErrorMessage.Resource(R.string.subscription_disabled)
            SubscriptionAppDisabled -> ErrorMessage.Resource(R.string.subscription_app_disabled)
            GlobalGroupDisabled -> ErrorMessage.Resource(R.string.global_rule_group_disabled)
            ShadowedByAppRule -> ErrorMessage.Resource(R.string.global_rule_shadowed_by_app_rule)
            AppExcluded -> ErrorMessage.Resource(R.string.rule_app_excluded)
            VersionMismatch -> ErrorMessage.Resource(R.string.rule_app_version_mismatch)
        }

    data class Invalid(val error: RuleValidationError?) : RuleRestriction
    data object AppNotApplicable : RuleRestriction
    data object SubscriptionDisabled : RuleRestriction
    data object SubscriptionAppDisabled : RuleRestriction
    data object GlobalGroupDisabled : RuleRestriction
    data object ShadowedByAppRule : RuleRestriction
    data object AppExcluded : RuleRestriction
    data object VersionMismatch : RuleRestriction
}

enum class RuleControlScope(val labelResource: Int) {
    Group(R.string.rule_group_scope),
    CurrentApp(R.string.rule_current_app_scope),
    SubscriptionApp(R.string.subscription_app_switch_scope),
}
