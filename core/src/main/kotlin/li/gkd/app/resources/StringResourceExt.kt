package li.gkd.app.resources

import android.content.res.Resources
import androidx.annotation.StringRes
import li.gkd.app.app

/** Blocking bridge for synchronous callbacks. Prefer stringResource in Compose and getString in coroutines.
 * Reads can perform I/O. Never call during static initialization or from high-frequency work.
 * The result is deliberately not cached here so a later call observes the current language.
 */
fun @receiver:StringRes Int.getSync(vararg arguments: Any?): String {
    val resources: Resources = app.resources
    return if (arguments.isEmpty()) {
        resources.getString(this)
    } else {
        resources.getString(this, *arguments.map { it.toString() }.toTypedArray())
    }
}

/** Coroutine bridge: resolves the current language on the calling dispatcher. */
suspend fun getString(@StringRes id: Int, vararg arguments: Any?): String =
    app.resources.getString(id, *arguments.map { it.toString() }.toTypedArray())
