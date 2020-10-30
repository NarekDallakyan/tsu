package social.tsu.android.ui.model

interface FeedContent< out T> {
    fun getContent(): T
}
