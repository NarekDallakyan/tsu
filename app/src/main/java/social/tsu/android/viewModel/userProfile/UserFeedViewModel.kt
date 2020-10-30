package social.tsu.android.viewModel.userProfile

import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.network.model.LikeResponse
import social.tsu.android.network.model.ShareResponseDTO
import social.tsu.android.service.*
import social.tsu.android.ui.BaseFeedViewModel

interface UserFeedViewModelCallback {
    fun didUpdatePost(post: Post)
    fun didUpdateFeed(lastPostId: Int?)
    fun didSharePost(postId: Long)
    fun didUnsharePost(postId: Long)
    fun didReportPost(postId: Long)
    fun didErrorWith(message: String)
    fun didBlockUser(userId: Int, message: String)
    fun didUnblockUser(userId: Int, message: String)
    fun didFailedToBlockUser(userId: Int, message: String)
    fun didFailedToUnblockUser(userId: Int, message: String)
    fun didDeletedPost(postId: Long)
    fun didReachedFeedEnd()
}

abstract class UserFeedViewModel : BaseFeedViewModel {
    abstract var nextPage: Int?
    abstract fun getUserPost(postId: Int, userId: Int)
    abstract fun getUserFeed(userId: Int, beforePostId: Int?)
    abstract fun feedForUser(userId: Int): List<Post>
}

class DefaultUserFeedViewModel(
    private val application: TsuApplication,
    private val callback: UserFeedViewModelCallback?
): UserFeedViewModel(), LikeServiceCallback, ShareServiceCallback, FeedServiceCallback,
    BlockServiceCallback {

    override var nextPage: Int? = 0

    private val feedService: FeedService by lazy {
        DefaultFeedService(application, this)
    }

    private val likeSerivce: LikeService by lazy {
        DefaultLikeService(application, this)
    }

    private val shareService: ShareService by lazy {
        DefaultShareService(application, this)
    }

    private val blockService: BlockService by lazy {
        DefaultBlockService(application, this)
    }

    init {
        application.appComponent.inject(this)
    }

    override fun feedForUser(userId: Int): List<Post> {
        return feedService.getUserPosts(userId)
    }

    override fun like(post: Post) {
        likeSerivce.like(post)
    }

    override fun unlike(post: Post) {
        likeSerivce.unlike(post)
    }

    override fun share(post: Post) {
        shareService.share(post.id)
    }

    override fun unshare(post: Post) {
        post.shared_id?.let {
            shareService.unshare(it)
        }
    }

    override fun report(postId: Long, reasonId: Int) {
        feedService.report(postId, reasonId)
    }

    override fun delete(postId: Long) {
        feedService.deletePost(postId)
    }

    override fun getUserPost(postId: Int, userId: Int) {
        feedService.getPost(postId, userId)
    }

    override fun getUserFeed(userId: Int, beforePostId: Int?) {
        feedService.getUserFeed(userId, beforePostId)
    }

    override fun completedDeletePost(postId: Long) {
        callback?.didDeletedPost(postId)
    }

    override fun completedReport(postId: Long) {
        callback?.didReportPost(postId)
    }

    override fun completedError(message: String) {
        didErrorWith(message)
    }

    override fun didCompletePostLike(post: Post, response: LikeResponse) {
        if (response.postId > 0) {
            callback?.didUpdatePost(post)
        } else {
            didErrorWith(response.mesaage)
        }
    }

    override fun didCompletePostUnlike(post: Post, response: LikeResponse) {
        if (response.postId > 0) {
            callback?.didUpdatePost(post)
        } else {
            didErrorWith(response.mesaage)
        }
    }

    override fun didErrorWith(message: String) {
        callback?.didErrorWith(message)
    }

    override fun didCompletePostShare(response: ShareResponseDTO) {
        callback?.didSharePost(response.postId)
    }

    override fun didCompletePostUnshare(response: ShareResponseDTO) {
        callback?.didUnsharePost(response.postId)
    }

    override fun didFailToSharePost(message: String) {
        callback?.didErrorWith(message)
    }

    override fun completedGetPost(post: Post) {
        callback?.didUpdatePost(post)
    }

    override fun completedGetUserPosts(lastPostId: Int?) {
        callback?.didUpdateFeed(lastPostId)
    }

    override fun block(userId: Int) {
        blockService.blockUser(userId)
    }

    override fun unblock(userId: Int) {
        blockService.unblockUser(userId)
    }

    override fun didCompleteUserBlock(userId: Int, message: String) {
        callback?.didBlockUser(userId, message)
    }

    override fun didCompleteUserUnblockResponse(userId: Int, message: String) {
        callback?.didUnblockUser(userId, message)
    }

    override fun didFailToBlockUser(userId: Int, message: String) {
        callback?.didFailedToBlockUser(userId, message)
    }

    override fun didFailToUnblockUser(userId: Int, message: String) {
        callback?.didFailedToUnblockUser(userId, message)
    }
}