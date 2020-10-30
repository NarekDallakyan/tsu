package social.tsu.android.ui.post_feed


import androidx.lifecycle.ViewModel
import social.tsu.android.data.local.entity.Post
import social.tsu.android.data.repository.PostFeedRepository

abstract class BaseFeedViewModel(
    private val postFeedRepo: PostFeedRepository
) : ViewModel() {

    val postsLoadState by lazy {
        postFeedRepo.loadState
    }

    fun like(post: Post) = postFeedRepo.likePost(post)

    fun unlike(post: Post) = postFeedRepo.unlikePost(post)

    fun share(post: Post) = postFeedRepo.sharePost(post.id)

    fun unshare(post: Post) = postFeedRepo.unsharePost(post)

    fun block(userId: Int) = postFeedRepo.blockUser(userId)

    fun unblock(userId: Int) = postFeedRepo.unblockUser(userId)

    fun report(post: Post, reason: Int) = postFeedRepo.report(post.id, reason)

    fun delete(postId: Long) = postFeedRepo.delete(postId)

    fun support(postId: Long) = postFeedRepo.supportPost(postId)
}