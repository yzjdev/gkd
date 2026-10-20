package li.gkd.app.ui.share

import li.gkd.app.R

import java.io.File
import kotlinx.coroutines.CoroutineScope
import li.gkd.app.platform.PlatformResult
import li.gkd.app.storage.FileSource
import li.gkd.app.ui.androidState
import li.gkd.app.ui.platform.UiHost

fun UiHost.launchFileAction(block: suspend CoroutineScope.() -> Unit) =
    mainVm.scope.launchUi(block = block)
suspend fun UiHost.pickBackupFile(): FileSource? =
    mainVm.androidState.activityResults.openDocument("application/zip")?.let { FileSource.Uri(it.toString()) }

suspend fun UiHost.shareExportFile(title: String, create: suspend () -> File): PlatformResult<Unit> {
    // A native share target may read the file after returning; cache expiry releases it.
    shareFile(create(), title)
    return PlatformResult.Success(Unit)
}

suspend fun UiHost.saveExportFile(file: File) = saveFileToDownloads(file)
