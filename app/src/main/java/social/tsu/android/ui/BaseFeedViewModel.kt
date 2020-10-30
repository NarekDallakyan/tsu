package social.tsu.android.ui

import social.tsu.android.data.local.entity.Post


interface BaseFeedViewModel {

    fun like(post: Post)
    fun unlike(post: Post)
    fun share(post: Post)
    fun unshare(post: Post)
    fun delete(postId: Long)
    fun report(postId: Long, reasonId: Int)
    fun block(userId: Int)
    fun unblock(userId: Int)
}