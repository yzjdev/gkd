package li.gkd.app.subscription

import li.gkd.app.R

interface SubscriptionFiles {
    fun load(id: Long): RawSubscription
    fun readBytes(id: Long): ByteArray?
    fun write(subscription: RawSubscription)
    fun restore(id: Long, bytes: ByteArray?)
    fun delete(id: Long)
}
