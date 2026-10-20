package li.gkd.app.ui.component

import li.gkd.app.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import li.gkd.app.permission.AppPermissionRestriction
import li.gkd.app.priv.PrivilegeCapabilities
import li.gkd.app.resources.*
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

@Composable
fun GkPermissionRestrictionDialog(
    privilegeAvailable: Boolean,
    capabilities: PrivilegeCapabilities?,
    appRestrictions: Set<AppPermissionRestriction>,
    onDismiss: () -> Unit,
    onPrivilege: () -> Unit,
) {
    val adbRestricted = privilegeAvailable && capabilities?.restricted == true
    val appRestricted = !privilegeAvailable && appRestrictions.isNotEmpty()
    val items = buildList<Int> {
        if (adbRestricted) {
            if (!capabilities.injectEvents) add(R.string.restriction_adb_input)
            if (!capabilities.grantRuntimePermissions) add(R.string.restriction_adb_grant)
            if (!capabilities.writeSecureSettings) add(R.string.restriction_adb_secure)
            if (!capabilities.updateAppOps) add(R.string.restriction_adb_app_ops)
        } else if (appRestricted) {
            addAll(appRestrictions.map { it.descriptionResource })
        }
    }
    GkAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(when {
                adbRestricted -> R.string.adb_permission_restricted
                appRestricted -> R.string.app_permission_restricted
                else -> R.string.privilege_service_required
            }))
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(when {
                    adbRestricted -> R.string.restriction_adb_intro
                    appRestricted -> R.string.restriction_app_intro
                    privilegeAvailable && capabilities == null -> R.string.privilege_permissions_unverified
                    else -> R.string.automation_privilege_required_description
                }))
                if (items.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.forEach { Text("• " + stringResource(it)) }
                    }
                    Text(stringResource(if (adbRestricted) R.string.restriction_adb_footer else R.string.restriction_app_footer))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onPrivilege()
            }) { Text(stringResource(R.string.restriction_details_go)) }
        },
    )
}
