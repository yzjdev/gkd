package li.gkd.app.priv

import li.gkd.app.R

data class ShellCommandResult(
    val code: Int,
    val result: String,
    val error: String?
) {
    val ok: Boolean
        get() = code == 0
}
