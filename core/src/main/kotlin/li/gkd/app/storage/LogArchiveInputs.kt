package li.gkd.app.storage

import li.gkd.app.R

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.gkd.app.data.LogMetadataSources
import li.gkd.app.util.AndroidStorage
import li.gkd.app.util.LogUtils
import java.io.File

data class LogArchiveInputs(
    val metadata: Map<String, () -> String>,
    val additionalDirectories: List<File> = emptyList(),
)

fun logArchiveInputs() = LogArchiveInputs(
    LogMetadataSources.sources(),
    listOfNotNull(AndroidStorage.privilegeCrashDirectory),
)

suspend fun createLogArchive(): File = withContext(Dispatchers.IO) {
    LogUtils.flush()
    val inputs = logArchiveInputs()
    LogArchive.build(inputs.metadata, inputs.additionalDirectories)
}
