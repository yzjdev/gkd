package li.gkd.app.settings

import java.io.File
import li.gkd.app.storage.writeTextAtomically

/** Durable settings format; paths belong to the host. */
class FileSettingsStorage(private val directory: File) : SettingsStorage {
    override fun read(filename: String): String? =
        directory.resolve(filename).takeIf { it.exists() }?.readText()

    override fun writeAtomically(filename: String, text: String) {
        directory.resolve(filename).writeTextAtomically(text)
    }
}
