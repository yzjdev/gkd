package li.gkd.app.time

import li.gkd.app.R

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.format(pattern: String): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
