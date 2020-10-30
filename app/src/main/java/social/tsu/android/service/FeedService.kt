package social.tsu.android.service

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.Cache
import social.tsu.android.helper.CommunityFeedCache
import social.tsu.android.helper.PostsCache
import social.tsu.android.helper.UserFeedCache
import social.tsu.android.network.api.PostApi
import social.tsu.android.network.model.ReportPostPayload
import social.tsu.android.rx.plusAssign
import social.tsu.android.utils.updateWith
import javax.inject.Inject

interface FeedServiceCallback: DefaultServiceCallback {
    fun completedGetPost(post: Post)
    fun completedGetUserPosts(lastPostId: Int?)
    fun completedDeletePost(postId: Long)
    fun completedReport(postId: Long)
    fun completedError(message: String)
}

abstract class FeedService: DefaultService() {
    abstract fun getPost(postId: Int, userId: Int, isCommunityPost: Boolean = false)
    abstract fun deletePost(postId: Long)
    abstract fun getUserFeed(userId: Int, start: Int?)
    abstract fun report(postId: Long, reasonId: Int)
    abstract fun getUserPosts(userId: Int): List<Post>
}

class DefaultFeedService(
    private val application: TsuApplication,
    private var callback: FeedServiceCallback?
) : FeedService() {

    @Inject
    lateinit var postApi: PostApi

    @Inject
    lateinit var schedulers: RxSchedulers

    private val userFeedCache: UserFeedCache = UserFeedCache
    private val postsCache: PostsCache = PostsCache
    private val communityFeedCache: CommunityFeedCache = CommunityFeedCache

    override val tag: String = "DefaultFeedService"

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    init {
        application.appComponent.inject(this)
    }

    override fun getPost(postId: Int, userId: Int, isCommunityPost: Boolean) {
            compositeDisposable += postApi.getPost(postId)
                .observeOn(schedulers.main())
                .subscribeOn(schedulers.io())
                .subscribe(
                    { response ->
                        handleResponse(
                            application,
                            response,
                            onSuccess = { result ->
                                updateWithNewPost(userId, result.data, isCommunityPost)
                            },
                            onFailure = { errMsg ->
                                callback?.didErrorWith(errMsg)
                                Log.e(
                                    tag,
                                    "failed to get a post from response body error ${response.code()}"
                                )
                            }
                        )
                    },
                    { err ->
                        callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                        Log.e(tag, "error = ${err.message}")
                    }
                )

    }

    override fun getUserPosts(userId: Int): List<Post> {
        return userFeedCache[userId.toLong()]
    }

    override fun deletePost(postId: Long) {
            compositeDisposable += postApi.deletePost(postId)
                .observeOn(schedulers.main())
                .subscribeOn(schedulers.io())
                .subscribe(
                    { response ->
                        handleResponse(
                            application,
                            response,
                            onSuccess = { result ->
                                if (result.success) {
                                    deleteAllPosts(postId)
                                } else {
                                    callback?.completedDeletePost(postId)
                                }
                            },
                            onFailure = {
                                callback?.completedDeletePost(postId)
                                Log.e(tag, "failed to delete post error ${response.code()}")
                            }
                        )
                    },
                    { err ->
                        Log.e(tag, "error = ${err.message}")
                        callback?.completedDeletePost(postId)
                    }
                )

    }

    override fun getUserFeed(userId: Int, start: Int?) {
            compositeDisposable += postApi.getUserFeed(userId, start)
                        .observeOn(schedulers.main())
                        .subscribeOn(schedulers.io())
                        .subscribe(
                            { response ->
                                handleResponse(
                                    application,
                                    response,
                                    onSuccess = { result ->
                                        val incoming = result.data.posts
                                        update(userId, incoming)
                                    },
                                    onFailure = {
                                        callback?.completedGetUserPosts(null)
                                        Log.e(
                                            tag,
                                            "failed to get user feed error ${response.code()}"
                                        )
                                    }
                                )
                            },
                    { err ->
                        Log.e(tag, "error = ${err.message}")
                        callback?.completedGetUserPosts(null)
                    }
                )

    }

    override fun report(postId: Long, reasonId: Int) {
            compositeDisposable += postApi.reportPost( ReportPostPayload(postId, reasonId))
                .observeOn(schedulers.main())
                .subscribeOn(schedulers.io())
                .subscribe({ response ->
                    handleResponse(
                        application,
                        response,
                        onSuccess = {
                            callback?.completedReport(postId)
                        },
                        onFailure = {
                            Log.e(tag, "error ${response.code()}")
                            callback?.completedError(it)
                        }
                    )
                }, { err ->
                    Log.e(tag, "error = ${err.message}")
                    callback?.completedError(err.getNetworkCallErrorMessage(application))
                })
    }

    private fun deleteAllPosts(postId: Long) {
        userFeedCache.allKeys().forEach {
            val allPosts = userFeedCache[it]
            val post = allPosts.find { it.id == postId } ?: return@forEach
            val mutable = allPosts.toMutableList()
            mutable.remove(post)
            userFeedCache[it] = mutable.toList()
        }
        communityFeedCache.allKeys().forEach {
            val allPosts = communityFeedCache[it]
            val post = allPosts.find { it.id == postId } ?: return@forEach
            val mutable = allPosts.toMutableList()
            mutable.remove(post)
            communityFeedCache[it] = mutable.toList()
        }
        postsCache.remove(postId)
        callback?.completedDeletePost(postId)
    }

    private fun update(userId: Int, posts: List<Post>?) {
        if (posts == null || posts.isEmpty()) {
            callback?.completedGetUserPosts(null)
            return
        }

        val savedPosts = this.userFeedCache[userId.toLong()]
        if (savedPosts.containsAll(posts)) {
            // Nothing changed
            callback?.completedGetUserPosts(savedPosts.last().id.toInt())
            return
        }

        val sorted = posts.union(savedPosts).sortedByDescending {
            it.timestamp
        }
        this.userFeedCache[userId.toLong()] = sorted
        callback?.completedGetUserPosts(sorted.last().id.toInt())
        sorted.forEach {
            postsCache[it.id] = it
        }
    }

    private fun updateWithNewPost(userId: Int, post: Post?, isCommunityPost: Boolean) {
        val cache: Cache<Long, List<Post>> = if (isCommunityPost) communityFeedCache else userFeedCache
        if (post != null) {
            val sorted = cache[userId.toLong()]!!
                .updateWith(post)
                .sortedByDescending {
                    it.timestamp
                }
            cache[userId.toLong()] = sorted
            postsCache[post.id] = post
            callback?.completedGetPost(post)
        }
    }

}