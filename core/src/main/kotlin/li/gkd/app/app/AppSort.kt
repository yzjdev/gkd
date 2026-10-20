package li.gkd.app.app

import li.gkd.app.R

import li.gkd.app.model.Option
import li.gkd.app.resources.*
import androidx.annotation.StringRes

/** Explicit values are part of the persisted settings contract. */
enum class AppSort(override val value: Int, override val labelResource: Int) : Option<Int> {
    ByAppName(0, R.string.app_sort_name), ByActionTime(2, R.string.app_sort_recent_trigger), ByUsedTime(3, R.string.app_sort_recent_used), ByTriggerCount(4, R.string.trigger_count_sort);

    override val options get() = Companion.options

    companion object {
        val options = listOf(ByAppName, ByUsedTime, ByActionTime, ByTriggerCount)
    }
}
