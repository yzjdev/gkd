package li.gkd.app.data

import li.gkd.app.R

import android.os.Build
import li.gkd.app.model.DeviceInfo

fun currentDeviceInfo() = DeviceInfo(
    Build.DEVICE,
    Build.MODEL,
    Build.MANUFACTURER,
    Build.BRAND,
    Build.VERSION.SDK_INT,
    Build.VERSION.RELEASE
)
