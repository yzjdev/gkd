package li.gkd.app.app

import li.gkd.app.R

object ActivityNames {
    fun getShowActivityId(appId: String, activityId: String?): String? =
        activityId?.let { if (it.startsWith("$appId.")) it.substring(appId.length) else it }
}
