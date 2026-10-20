package li.gkd.app.rule

import li.gkd.app.R

import li.gkd.app.resources.*
import li.gkd.db.SubsCategoryConfig
import androidx.annotation.StringRes

enum class CategorySetting(
    val value: Boolean?,
    val labelResource: Int,
    val descriptionResource: Int,
) {
    FollowSubscription(null, R.string.category_follow_subscription, R.string.category_follow_subscription_description),
    Enabled(true, R.string.category_enabled, R.string.category_enabled_description),
    Disabled(false, R.string.category_disabled, R.string.category_disabled_description),
    GroupDefault(null, R.string.category_use_group_default, R.string.category_use_group_default_description);

    companion object {
        fun from(config: SubsCategoryConfig?): CategorySetting =
            config?.let { fromOverride(it.enable) } ?: FollowSubscription

        fun fromOverride(value: Boolean?): CategorySetting = when (value) {
            true -> Enabled
            false -> Disabled
            null -> GroupDefault
        }
    }
}
