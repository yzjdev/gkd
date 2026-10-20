package li.gkd.app.snapshot

import li.gkd.app.R

import li.gkd.app.model.Option
import li.gkd.app.resources.*
import androidx.annotation.StringRes

/** Explicit values are part of the persisted settings contract. */
enum class SnapshotDisplayMode(override val value: Int, override val labelResource: Int) : Option<Int> {
    ByTime(1, R.string.snapshot_view_time), ByApp(2, R.string.snapshot_view_app);

    override val options get() = entries

    companion object {
        fun switch(value: Int): Int =
            if (value == ByApp.value) ByTime.value else ByApp.value
    }
}
