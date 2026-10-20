package li.gkd.app.model

import li.gkd.app.R

import androidx.annotation.StringRes

interface Option<T> {
    val value: T
    val labelResource: Int
    val options: List<Option<T>>
}

fun <V, T : Option<V>> Iterable<T>.findOption(value: V): T =
    find { it.value == value } ?: first()
