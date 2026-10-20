package li.gkd.app.rule

import li.gkd.app.R

import li.gkd.app.resources.*
import androidx.annotation.StringRes

enum class RuleStatus(val descriptionResource: Int?) {
    Ready(null),
    ActionLimitReached(R.string.rule_status_maximum_actions),
    PrerequisitePending(R.string.rule_status_prerequisite),
    MatchDelay(R.string.rule_status_match_delay),
    MatchTimeout(R.string.rule_status_match_timeout),
    Cooldown(R.string.rule_status_cooldown),
    ActionDelay(R.string.rule_status_action_delay);

    val ok: Boolean get() = this == Ready
    val alive: Boolean
        get() = this != ActionLimitReached && this != PrerequisitePending && this != MatchTimeout
}
