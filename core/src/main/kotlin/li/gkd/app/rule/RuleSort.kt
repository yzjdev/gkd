package li.gkd.app.rule

import li.gkd.app.R

import li.gkd.app.model.Option
import li.gkd.app.resources.*
import androidx.annotation.StringRes

/** Explicit values are part of the persisted settings contract. */
enum class RuleSort(override val value: Int, override val labelResource: Int) : Option<Int> {
    ByDefault(0, R.string.rule_sort_default), ByActionTime(1, R.string.app_sort_recent_trigger), ByRuleName(2, R.string.rule_sort_name), ByTriggerCount(3, R.string.trigger_count_sort);

    override val options get() = entries
}
