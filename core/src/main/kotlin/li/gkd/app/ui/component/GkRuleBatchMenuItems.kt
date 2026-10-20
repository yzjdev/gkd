package li.gkd.app.ui.component

import li.gkd.app.R

import androidx.compose.runtime.Composable
import li.gkd.app.resources.*
import li.gkd.app.rule.RuleSetting
import androidx.compose.ui.res.stringResource

@Composable
fun GkRuleBatchMenuItems(
    enabled: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (RuleSetting) -> Unit,
) {
    GkBatchActionMenuItem(
        stringResource(R.string.action_turn_on),
        onDismiss,
        { onUpdate(RuleSetting.Enabled) },
        enabled
    )
    GkBatchActionMenuItem(
        stringResource(R.string.action_close),
        onDismiss,
        { onUpdate(RuleSetting.Disabled) },
        enabled
    )
    GkBatchActionMenuItem(
        stringResource(R.string.settings_reset_default),
        onDismiss,
        { onUpdate(RuleSetting.FollowDefault) },
        enabled
    )
}
