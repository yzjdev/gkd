package li.gkd.app.ui.component

import li.gkd.app.R

import androidx.compose.runtime.Composable
import li.gkd.app.resources.*
import androidx.compose.ui.res.stringResource

@Composable
fun GkRuleDisplayIconButton(
    byCategory: Boolean,
    onClick: () -> Unit,
) {
    GkIconButton(
        imageVector = if (byCategory) GkIcons.Category else GkIcons.ViewStream,
        contentDescription = stringResource(
            if (byCategory) R.string.rule_display_switch_to_flat
            else R.string.rule_display_switch_to_category,
        ),
        onClick = onClick,
        animateMorph = true,
    )
}
