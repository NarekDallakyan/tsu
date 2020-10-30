package social.tsu.android

import social.tsu.android.data.local.entity.Post

abstract class OnBottomReachedListener {
    abstract fun onBottomReached(lastPost: Post?)
}
