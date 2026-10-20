package li.gkd.app.ui.settings

import li.gkd.app.R

import li.gkd.app.resources.*

object SettingsFeedback {
    fun backupImported(skipped: Int): String = if (skipped > 0) {
        R.string.backup_import_skipped_config_count.getSync(skipped)
    } else R.string.import_success.getSync()
}
