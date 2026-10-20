package li.gkd.app.subscription

import li.gkd.app.R

import li.gkd.app.resources.*

/** Names used only when initialization creates missing built-in subscription files. */
data class SubscriptionDefaults(val localName: String, val memoryName: String) {
    companion object {
        suspend fun fromResources(): SubscriptionDefaults = SubscriptionDefaults(
            localName = getString(R.string.subscription_local),
            memoryName = getString(R.string.subscription_memory),
        )
    }
}
