package li.gkd.app.notif

import li.gkd.app.R

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import li.gkd.app.META
import li.gkd.app.app
import li.gkd.app.resources.*
import androidx.annotation.StringRes

enum class AppNotificationChannel(
    val id: String,
    private val labelRes: Int? = null,
    val description: String? = null,
    val importance: Int = NotificationManager.IMPORTANCE_LOW,
) {
    Service(id = "0"),
    Snapshot(id = "1", labelRes = R.string.snapshot_notification_channel);

    val displayName: String
        get() = labelRes?.getSync() ?: META.appName
}

object NotificationChannels {
    fun initialize() {
        val manager = NotificationManagerCompat.from(app)
        val channelIds = AppNotificationChannel.entries.mapTo(mutableSetOf()) { it.id }

        manager.notificationChannels
            .filter { it.id !in channelIds }
            .forEach { manager.deleteNotificationChannel(it.id) }

        manager.createNotificationChannels(
            AppNotificationChannel.entries.map { spec ->
                NotificationChannel(
                    spec.id,
                    spec.displayName,
                    spec.importance,
                ).apply {
                    description = spec.description
                }
            }
        )
    }
}
