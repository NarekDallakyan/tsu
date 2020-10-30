package social.tsu.android.service

import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.network.api.CommunityApi
import javax.inject.Inject

abstract class CommunityFeedService : DefaultService() {

    abstract fun getPost(postId: Int, userId: Int)
    abstract fun deletePost(postId: Long)
    abstract fun getUserFeed(userId: Int, start: Int?)
    abstract fun report(postId: Long, reasonId: Int)
    abstract fun getUserPosts(userId: Int): List<Post>
}

interface CommunityFeedServiceCallback : DefaultServiceCallback {
    fun completedGetPost(post: Post)
    fun completedGetUserPosts()
    fun completedDeletePost()
    fun completedReport(postId: Long)
    fun completedError(message: String)
}

class DefaultCommunityFeedService(
    private val application: TsuApplication,
    var callback: CommunityFeedServiceCallback?
) : CommunityFeedService() {

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()


    override val tag: String = "DefaultCommunityFeedService"

    @Inject
    lateinit var communityApi: CommunityApi

    @Inject
    lateinit var schedulers: RxSchedulers

    init {
        application.appComponent.inject(this)
    }

    override fun getPost(postId: Int, userId: Int) {
        TODO("Not yet implemented")
    }

    override fun deletePost(postId: Long) {
        TODO("Not yet implemented")
    }

    override fun getUserFeed(userId: Int, start: Int?) {
        TODO("Not yet implemented")
    }

    override fun report(postId: Long, reasonId: Int) {
        TODO("Not yet implemented")
    }

    override fun getUserPosts(userId: Int): List<Post> {
        TODO("Not yet implemented")
    }
}