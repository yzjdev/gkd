package li.gkd.app.ui.component

import li.gkd.app.R

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import li.gkd.app.resources.*
import androidx.compose.ui.res.stringResource

sealed interface UpdateDownloadState {
    data class Loading(val progress: Float) : UpdateDownloadState
    data class Failure(val exception: Exception) : UpdateDownloadState
    data object Success : UpdateDownloadState
}

@Composable
fun GkUpgradeDialogs(
    text: String?, allowIgnore: Boolean, downloadStatus: UpdateDownloadState?,
    onDownload: () -> Unit, onDismissVersion: () -> Unit, onIgnore: () -> Unit,
    onCancelDownload: () -> Unit, onDismissDownload: () -> Unit, onInstall: () -> Unit,
) {
    if (text != null) {
        val scrollState = rememberScrollState()
        GkAlertDialog(
            title = {
                Text(text = stringResource(R.string.update_new_version))
            },
            text = {
                Text(
                    text = text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(scrollState)
                )
            },
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = {
                    onDownload()
                }) {
                    Text(text = stringResource(R.string.update_download))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismissVersion() }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
                if (allowIgnore) {
                    TextButton(onClick = {
                        onIgnore()
                    }) {
                        Text(text = stringResource(R.string.action_ignore))
                    }
                }
            },
        )
    }
    downloadStatus?.let { downloadStatusVal ->
        when (downloadStatusVal) {
            is UpdateDownloadState.Loading -> {
                GkAlertDialog(
                    title = { Text(text = stringResource(R.string.image_downloading)) },
                    text = {
                        LinearProgressIndicator(
                            progress = { downloadStatusVal.progress },
                        )
                    },
                    onDismissRequest = {},
                    confirmButton = {
                        TextButton(onClick = {
                            onCancelDownload()
                        }) {
                            Text(text = stringResource(R.string.download_abort))
                        }
                    },
                )
            }

            is UpdateDownloadState.Failure -> {
                GkAlertDialog(
                    title = { Text(text = stringResource(R.string.download_failed)) },
                    text = {
                        Text(text = downloadStatusVal.exception.let {
                            it.message ?: it.toString()
                        })
                    },
                    onDismissRequest = { onDismissDownload() },
                    confirmButton = {
                        TextButton(onClick = {
                            onDismissDownload()
                        }) {
                            Text(text = stringResource(R.string.action_close))
                        }
                    },
                )
            }

            UpdateDownloadState.Success -> {
                GkAlertDialog(
                    title = { Text(text = stringResource(R.string.download_complete)) },
                    text = {
                        Text(text = stringResource(R.string.update_ready_to_install))
                    },
                    onDismissRequest = {},
                    dismissButton = {
                        TextButton(onClick = {
                            onDismissDownload()
                        }) {
                            Text(text = stringResource(R.string.action_close))
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = onInstall) {
                            Text(text = stringResource(R.string.action_install))
                        }
                    })
            }
        }
    }
}
