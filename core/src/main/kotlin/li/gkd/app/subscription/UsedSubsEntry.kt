package li.gkd.app.subscription

import li.gkd.app.R

import li.gkd.db.SubsItem

data class UsedSubsEntry(
    val subsItem: SubsItem,
    val subscription: RawSubscription,
)
