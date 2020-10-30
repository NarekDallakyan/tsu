package social.tsu.android.viewModel.mainFeedViewModel

import android.util.Log
import android.widget.EditText
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.helper.PostsCache
import social.tsu.android.network.api.PostApi
import social.tsu.android.network.model.CreatePostPayload
import social.tsu.android.network.model.LikeResponse
import social.tsu.android.network.model.ReportPostPayload
import social.tsu.android.network.model.ShareResponseDTO
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.*
import social.tsu.android.ui.BaseFeedViewModel
import social.tsu.android.utils.privacystring
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

interface MainFeedViewModelCallback {
    fun didUpdatePost(post: Post)
    fun didUnsharePost(post: Post)
    fun didUpdateFeed(nextPage: Int?, reset: Boolean)
    fun didCreatePost()
    fun didErrorWith(message: String)
    fun didCompleteShare()
    fun didReportPost(postId: Long)
    fun didBlockUser(userId: Int, message: String)
    fun didUnblockUser(userId: Int, message: String)
    fun didReachedFeedEnd()
}

abstract class MainFeedViewModel : BaseFeedViewModel {
    abstract val callback: MainFeedViewModelCallback
    abstract var posts: List<Post>
    abstract fun getFeed(start: Int?, reset: Boolean)
    abstract fun createPost(composePost: EditText)
    abstract fun onDestroy()
}

class DefaultMainFeedViewModel(
    callback: MainFeedViewModelCallback,
    private val application: TsuApplication
) : MainFeedViewModel(), LikeServiceCallback, ShareServiceCallback, BlockServiceCallback {

    private val FEED_POSTS_LOAD_COUNT = 10

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var api: PostApi

    private val compositeDisposable = CompositeDisposable()

    private val cache: PostsCache = PostsCache

    override val callback: MainFeedViewModelCallback = callback

    override var posts: List<Post> = emptyList()

    var nextPage: Int? = 0

    val properties = HashMap<String, Any?>()

    private var loading = AtomicBoolean(false)

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    private val likeService: LikeService by lazy {
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

    override fun onDestroy() {
        compositeDisposable.dispose()
        likeService.compositeDisposable.dispose()
        shareService.compositeDisposable.dispose()
        blockService.compositeDisposable.dispose()
    }

    override fun createPost(composePost: EditText) {
        val textPostRequest = CreatePostPayload(composePost.text.toString())

        compositeDisposable += api.createPost(textPostRequest)
            .observeOn(schedulers.main())
            .doFinally {
                callback.didCreatePost()
            }
            .subscribeOn(schedulers.io())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        Log.i("POST", "post created")

                        val post = result.data?.post
                        composePost.text = null

                        post?.let {
                            Log.i("POS", "pic = ${it.picture_url}")
                        }
                        getFeed(null, true)
                        if(post.groupId != null) {
                            properties["groupId"] = post.groupId
                        }
                        properties["type"] = privacystring(post.privacy)
                        properties["has_video"] = post.has_video
                        properties["has_picture"] = post.has_picture
                        properties["has_gif"] = post.has_gif
                        properties["has_text"] = !post.title.trim().isEmpty()

                        analyticsHelper.logEvent("post_created",properties)
                    },
                    onFailure = {
                        callback.didErrorWith(it)
                    }
                )
            }, { err ->
                Log.e("POST", "could not create post", err)
                callback.didErrorWith(err.getNetworkCallErrorMessage(application))
            })
    }

    override fun getFeed(start: Int?, reset: Boolean) {
        if (loading.get()) {
            Log.w("MainFeedViewModel", "Feed request already running.")
            return
        }

        if (start == null) {
            //manual refresh, need to reset nextPage
            nextPage = null
        }

        loading.set(true)
        compositeDisposable += api.getFeedV2Chrono(start, FEED_POSTS_LOAD_COUNT)
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .doFinally {
                loading.set(false)
            }
            .subscribe(
                { response ->
                    handleResponse(
                        application,
                        response,
                        onSuccess = { result ->
                            val incoming = result.data.sources.flatMap { it.posts }
                            nextPage = result.data.next_page
                            Log.d("FeedModel", "Next page: ${result.data.next_page}")
                            updateWithNewPosts(incoming, nextPage, reset)
                        },
                        onFailure = {
                            callback.didErrorWith(it)
                        }
                    )
                },
                { err ->
                    callback.didErrorWith(err.getNetworkCallErrorMessage(application))
                }
            )
    }

    fun getPost(postId: Int) {
        compositeDisposable += api.getPost(postId)
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe(
                { response ->
                    handleResponse(
                        application,
                        response,
                        onSuccess = { result ->
                            val incoming = result.data
                            updateWithNewPost(incoming)
                        },
                        onFailure = {
                            callback.didErrorWith(it)
                        }
                    )
                },
                { err ->
                    callback.didErrorWith(err.getNetworkCallErrorMessage(application))
                }
            )
    }

    override fun like(post: Post) {
        likeService.like(post)
    }

    override fun unlike(post: Post) {
        likeService.unlike(post)
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
        compositeDisposable += api.reportPost(ReportPostPayload(postId, reasonId))
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = {
                        callback.didReportPost(postId)
                        deleteAllPosts(postId)
                    },
                    onFailure = {
                        callback.didErrorWith(it)
                    }
                )
            }, { err ->
                callback.didErrorWith(err.getNetworkCallErrorMessage(application))
            })
    }

    override fun delete(postId: Long) {
        compositeDisposable += api.deletePost(postId)
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
                            }
                        },
                        onFailure = {
                            callback.didErrorWith(it)
                        }
                    )
                },
                { err ->
                    callback.didErrorWith(err.getNetworkCallErrorMessage(application))
                }
            )
    }

    override fun block(userId: Int) {
        blockService.blockUser(userId)
    }

    override fun unblock(userId: Int) {
        blockService.unblockUser(userId)
    }

    override fun didCompleteUserBlock(userId: Int, message: String) {
        deleteAllPostsByUserId(userId)
        callback.didBlockUser(userId, message)
    }

    override fun didCompleteUserUnblockResponse(userId: Int, message: String) {
        callback.didUnblockUser(userId, message)
    }

    override fun didFailToBlockUser(userId: Int, message: String) {
        callback.didErrorWith(message)
    }

    override fun didFailToUnblockUser(userId: Int, message: String) {
        callback.didErrorWith(message)
    }

    override fun didCompletePostLike(post: Post, response: LikeResponse) {
        if (response.postId > 0) {
            callback.didUpdatePost(post)
        } else {
            didErrorWith(response.mesaage)
        }
    }

    override fun didCompletePostUnlike(post: Post, response: LikeResponse) {
        if (response.postId > 0) {
            callback.didUpdatePost(post)
        } else {
            didErrorWith(response.mesaage)
        }
    }

    override fun didErrorWith(message: String) {
        callback.didErrorWith(message)
    }

    private fun updateWithNewPost(post: Post?) {
        if (post != null) {
            val posts = ArrayList(this.posts)
            val idx = posts.indexOfFirst {
                it.originalPostId == post.originalPostId
            }

            //old logic
            /*if (idx < 0) {
                posts.add(0, post)
            } else {
                posts.removeAt(idx)
                posts.add(idx, post)
            }*/
            var removed: Post? = null
            //new logic
            if (idx >= 0) {
                removed = posts.removeAt(idx)
            }

            this.posts = posts
            cache.remove(post.id)
            removed?.let {
                cache.remove(it.id)
            }
            callback.didUnsharePost(post)
        }
    }

    private fun updateWithNewPosts(posts: List<Post>?, nextPage: Int?, reset: Boolean) {
        if (nextPage == null) {
            callback.didReachedFeedEnd()
        }

        if (posts == null || posts.isEmpty()) return

        val allPosts = posts.union(this.posts)
        // Nothing changed
        if (posts.size == 1 && allPosts.contains(posts[0])) return

        this.posts = allPosts.sortedByDescending {
            it.timestamp
        }
        this.posts.forEach { cache.set(it.id, it) }
        callback.didUpdateFeed(nextPage, reset)
    }

    fun deleteAllPosts(postId: Long) {
        cache.remove(postId)
        var mutable = this.posts.toMutableList()
        val post = mutable.find { it.id == postId }
        post?.let {
            mutable.remove(it)
        }
        this.posts = mutable.toList()
        callback.didUpdateFeed(null, true)
    }

    private fun deleteAllPostsByUserId(userId: Int) {
        cache.allCases().toMutableSet().filter { cache[it]?.user_id == userId }
            .forEach { cache.remove(it) }
        val filter = posts.filter { userId == it.user_id }
        val remaining = posts.toMutableList()
        remaining.removeAll(filter)
        posts = remaining.toList()
        callback.didUpdateFeed(null, true)
    }

    override fun didCompletePostShare(response: ShareResponseDTO) {
        this.posts = listOf()
        getFeed(null, true)
        callback.didCompleteShare()
    }

    override fun didCompletePostUnshare(response: ShareResponseDTO) {
        getPost(response.postId.toInt())
    }

    override fun didFailToSharePost(message: String) {
        callback.didErrorWith(message)
    }
}
