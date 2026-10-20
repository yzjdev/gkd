package li.gkd.app.storage

import li.gkd.app.R

import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import li.gkd.app.app
import li.gkd.app.error.BusinessError
import li.gkd.app.error.ErrorMessage
import li.gkd.app.resources.*

/** A selected source, not a file-picker or a permission owner. */
sealed interface FileSource {
    data class Local(val file: File) : FileSource
    data class Uri(val value: String) : FileSource
}

fun openFileSource(source: FileSource): InputStream = when (source) {
    is FileSource.Local -> source.file.inputStream()
    is FileSource.Uri -> app.contentResolver.openInputStream(source.value.toUri())
        ?: throw FileNotFoundException("Cannot open selected document")
}

class FileTooLargeException : IOException("File exceeds the size limit"), BusinessError {
    override val errorCode: String get() = "FileTooLarge"
    override val errorMessage: ErrorMessage get() = ErrorMessage.Resource(R.string.file_too_large)
}

suspend fun readFileBytes(source: FileSource, maxBytes: Int = 32 * 1024 * 1024): ByteArray =
    withContext(Dispatchers.IO) {
        openFileSource(source).use { input ->
            ByteArrayOutputStream(minOf(maxBytes, 8192)).use { output ->
                BoundedStreams.copy(input, output, maxBytes.toLong()) { FileTooLargeException() }
                output.toByteArray()
            }
        }
    }
