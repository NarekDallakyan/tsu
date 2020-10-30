package social.tsu.android.ui.model

sealed class Data<out T : Any?> {
    class Success<out T : Any>(val data: T) : Data<T>()
    class Loading<Nothing> : Data<Nothing>()
    class Error<out T : Any>(val throwable: Throwable, val data: T? = null) : Data<T>()
}
