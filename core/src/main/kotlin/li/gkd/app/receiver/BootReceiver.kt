package li.gkd.app.receiver

import li.gkd.app.R

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import li.gkd.app.service.StatusService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            StatusService.autoStart()
        }
    }
}
