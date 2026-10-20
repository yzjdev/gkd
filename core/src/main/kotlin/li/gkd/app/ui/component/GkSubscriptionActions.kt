package li.gkd.app.ui.component

import li.gkd.app.R

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import li.gkd.app.resources.*
import androidx.compose.ui.res.stringResource

@Composable
fun RowScope.GkSubscriptionActions(
    matching: Boolean, onToggleMatching: () -> Unit, onSettings: () -> Unit,
    onAddAppRule: () -> Unit, onAddGlobalRule: () -> Unit, onMenuOpen: () -> Boolean = { true },
) {
    var expanded by remember { mutableStateOf(false) }
    if (expanded) LocalOverlayBackHandler.current { expanded = false }
    GkIconButton(
        imageVector = if (matching) GkIcons.FlashOn else GkIcons.FlashOff, animateMorph = true,
        colors = IconButtonDefaults.iconButtonColors(contentColor = if (!matching) CheckboxDefaults.colors().checkedBoxColor else LocalContentColor.current),
        contentDescription = stringResource(R.string.rule_matching_label) + stringResource(if (matching) R.string.enabled else R.string.disabled),
        onClickLabel = stringResource(R.string.switch_toggle), onClick = onToggleMatching
    )
    GkIconButton(
        GkIcons.PageInfo, contentDescription = stringResource(R.string.subscription_settings),
        onClickLabel = stringResource(R.string.settings_dialog_open), onClick = onSettings
    )
    Box {
        GkIconButton(
            GkIcons.MoreVert,
            contentDescription = stringResource(R.string.more_actions),
            onClick = { expanded = onMenuOpen() })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.app_rule_add)) },
                onClick = { expanded = false; onAddAppRule() })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.global_rule_add)) },
                onClick = { expanded = false; onAddGlobalRule() })
        }
    }
}
