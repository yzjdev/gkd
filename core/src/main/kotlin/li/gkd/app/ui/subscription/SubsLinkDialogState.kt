package li.gkd.app.ui.subscription

import li.gkd.app.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import li.gkd.app.network.AppLinks
import li.gkd.app.network.LocalNetworkUrls
import li.gkd.app.permission.AppPermission
import li.gkd.app.permission.PermissionRequester
import li.gkd.app.resources.*
import li.gkd.app.subscription.SubscriptionRepository
import li.gkd.app.ui.component.GkAlertDialog
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.autoFocus
import li.gkd.app.ui.navigation.AppNavigator
import li.gkd.app.util.ToastUtils
import li.gkd.app.util.copyText
import androidx.compose.ui.res.stringResource

private data class SubsLinkDialogRequest(
    val initialValue: String,
    val existingUrls: Set<String>,
    val value: String,
)

class SubsLinkDialogState(
    private val navigator: AppNavigator,
    private val permissions: PermissionRequester,
) {
    private val requestFlow = MutableStateFlow<SubsLinkDialogRequest?>(null)
    private val requestMutex = Mutex()
    private var currentContinuation: CancellableContinuation<String?>? = null

    private fun complete(value: String?) {
        val continuation = currentContinuation ?: return
        if (continuation.isActive) {
            requestFlow.value = null
            continuation.resume(value)
        }
    }

    private fun updateValue(value: String) {
        val request = requestFlow.value ?: return
        requestFlow.value = request.copy(value = value.trim())
    }

    private fun submit(request: SubsLinkDialogRequest) {
        val value = request.value
        if (!LocalNetworkUrls.isNetworkUrl(value)) {
            ToastUtils.show(R.string.link_invalid.getSync())
            return
        }
        if (value in request.existingUrls) {
            ToastUtils.show(R.string.subscription_duplicate_link.getSync())
            return
        }
        complete(value)
    }

    private fun cancel() = complete(null)

    private fun openHelp() {
        cancel()
        navigator.openWebPage(AppLinks.SubscriptionHelp)
    }

    suspend fun request(initialValue: String = ""): String? {
        val existingUrls = withContext(Dispatchers.IO) {
            SubscriptionRepository.existingUpdateUrls()
        }
        val value = withContext(Dispatchers.Main.immediate) {
            requestMutex.withLock {
                try {
                    requestFlow.value = SubsLinkDialogRequest(
                        initialValue = initialValue,
                        existingUrls = existingUrls,
                        value = initialValue,
                    )
                    suspendCancellableCoroutine { continuation ->
                        currentContinuation = continuation
                    }
                } finally {
                    currentContinuation = null
                    requestFlow.value = null
                }
            }
        }
        if (initialValue.isEmpty() && value != null && LocalNetworkUrls.isLocalNetworkUrl(value) && !permissions.ensurePermissions(AppPermission.LocalNetwork)) {
            return null
        }
        return value
    }

    @Composable
    fun Render() {
        val request by requestFlow.collectAsStateWithLifecycle()
        val currentRequest = request
        if (currentRequest != null) {
            val editing = currentRequest.initialValue.isNotEmpty()
            GkAlertDialog(
                properties = DialogProperties(dismissOnClickOutside = false),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (editing) {
                                stringResource(R.string.subscription_link)
                            } else {
                                stringResource(R.string.subscription_add)
                            },
                        )
                        GkIconButton(
                            imageVector = GkIcons.HelpOutline,
                            contentDescription = stringResource(R.string.subscription_help),
                            onClick = ::openHelp,
                        )
                    }
                },
                text = {
                    OutlinedTextField(
                        value = currentRequest.value,
                        onValueChange = ::updateValue,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (editing) Modifier else Modifier.autoFocus()),
                        placeholder = {
                            Text(text = stringResource(R.string.subscription_link_input_hint))
                        },
                        isError = currentRequest.value.isNotEmpty() &&
                                !LocalNetworkUrls.isNetworkUrl(currentRequest.value),
                    )
                },
                onDismissRequest = ::cancel,
                confirmButton = {
                    TextButton(
                        enabled = currentRequest.value.isNotEmpty() &&
                                currentRequest.value != currentRequest.initialValue,
                        onClick = {
                            submit(currentRequest)
                        },
                    ) {
                        Text(text = stringResource(if (editing) R.string.action_save else R.string.action_ok))
                    }
                },
                dismissButton = {
                    Row {
                        if (editing) {
                            TextButton(
                                enabled = currentRequest.value.isNotEmpty(),
                                onClick = { copyText(currentRequest.value) },
                            ) {
                                Text(text = stringResource(R.string.action_copy))
                            }
                        }
                        TextButton(onClick = ::cancel) {
                            Text(text = stringResource(R.string.action_cancel))
                        }
                    }
                },
            )
        }
    }
}
