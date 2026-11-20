package li.gkd.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import li.gkd.app.settings.FileSettingsStorage
import li.gkd.app.storage.appStorage

/** The key stays in the private store, so it is never exported with backups. */
object AiApiKeyStore {
    private val files by lazy { FileSettingsStorage(appStorage().privateStore) }
    private val mutex = Mutex()

    val value: StateFlow<String>
        field = MutableStateFlow(files.read("ai_api_key.txt").orEmpty())

    suspend fun save(text: String) = mutex.withLock {
        val next = text.filter { it != '\n' && it != '\r' }.trim()
        withContext(Dispatchers.IO) { files.writeAtomically("ai_api_key.txt", next) }
        value.value = next
    }
}
