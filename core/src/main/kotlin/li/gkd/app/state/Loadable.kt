package li.gkd.app.state

import li.gkd.app.R

sealed interface Loadable<out T : Any> {
    val value: T?

    data object Loading : Loadable<Nothing> {
        override val value: Nothing? = null
    }

    data class Ready<T : Any>(override val value: T) : Loadable<T>

    data class Failure(val cause: Throwable) : Loadable<Nothing> {
        override val value: Nothing? = null
    }
}
