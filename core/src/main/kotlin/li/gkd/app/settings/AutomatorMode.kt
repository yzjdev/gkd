package li.gkd.app.settings

import li.gkd.app.R

import li.gkd.app.model.Option
import li.gkd.app.resources.*
import androidx.annotation.StringRes

/** Explicit values are part of the persisted settings contract. */
enum class AutomatorMode(override val value: Int, override val labelResource: Int) : Option<Int> {
    A11y(1, R.string.a11y_label), Automation(2, R.string.automation_label);

    override val options get() = entries
}
