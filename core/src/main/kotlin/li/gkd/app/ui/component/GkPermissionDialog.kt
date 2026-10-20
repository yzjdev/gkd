package li.gkd.app.ui.component

import li.gkd.app.R

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import li.gkd.app.resources.*
import androidx.compose.ui.res.stringResource

@Composable
fun GkPermissionDialog(
    title: String, message: String, confirmText: String, dismissText: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    GkAlertDialog(
        title = { Text(title) }, text = { Text(message) }, onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } })
}

@Composable
fun GkRestrictedSettingsDialog(onGrant: () -> Unit, onDismiss: () -> Unit) {
    GkPermissionDialog(
        stringResource(R.string.permission_restricted),
        stringResource(R.string.restricted_settings_permission_description),
        stringResource(R.string.permission_go_grant),
        stringResource(R.string.action_close),
        onGrant,
        onDismiss
    )
}

@Composable
fun GkAutomationOccupiedDialog(onDismiss: () -> Unit) {
    GkAlertDialog(
        onDismissRequest = onDismiss, title = { Text(stringResource(R.string.startup_failed)) },
        text = { Text(stringResource(R.string.automation_service_occupied_description)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_understood)) } })
}
