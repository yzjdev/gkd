package li.gkd.app.ai

import li.gkd.app.subscription.RawSubscription
import li.gkd.app.subscription.SubscriptionEditResult
import li.gkd.app.subscription.SubscriptionRepository
import li.gkd.db.SubscriptionIds

/** Group names must be unique per app; a generated name may already exist. */
private fun uniqueGroupName(used: List<String>, name: String): String {
    if (name !in used) return name
    var index = 2
    while ("$name ($index)" in used) index++
    return "$name ($index)"
}

/**
 * Appends one generated group to the local subscription of the snapshot's app. Key collisions are
 * resolved by the editor, so an existing group is never overwritten.
 */
suspend fun addRuleGroupToLocalSubscription(
    appId: String,
    appName: String?,
    group: RawSubscription.RawAppGroup,
): SubscriptionEditResult<Boolean> =
    SubscriptionRepository.editSubscription(SubscriptionIds.LOCAL) { subscription ->
        val app = subscription.getApp(appId, appName)
        val name = uniqueGroupName(app.groups.map { it.name }, group.name)
        subscription.editResult { appendAppGroups(app, listOf(group.copy(name = name))) }
    }
