package social.tsu.android.viewModel.community

import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.network.api.CommunityApi
import social.tsu.android.network.model.PendingPost
import social.tsu.android.service.handleResponse
import social.tsu.android.ui.BaseFeedViewModel
import javax.inject.Inject

abstract class CommunityPublishingRequestsViewModel : BaseFeedViewModel {

    override fun like(post: Post) {
    }

    override fun unlike(post: Post) {
    }

    override fun share(post: Post) {
    }

    override fun unshare(post: Post) {
    }

    override fun delete(postId: Long) {
    }

    override fun report(postId: Long, reasonId: Int) {
    }

    override fun block(userId: Int) {
    }

    override fun unblock(userId: Int) {
    }

    abstract fun approve(
        post: PendingPost,
        communityId: Int
    )
    abstract fun decline(
        post: PendingPost,
        communityId: Int
    )

    abstract fun getPendingPosts(groupId: Int)
}

interface CommunityPublishingRequestsCallback {

    fun didLoadPendingPosts(posts: List<PendingPost>)
    fun didApprovePost(post: PendingPost)
    fun didFailApprovePost(post: PendingPost)
    fun didDeclinePost(post: PendingPost)
    fun didFailDeclinePost(post: PendingPost)
    fun didFailedToLoad(message: String)
}

class DefaultCommunityPublishingRequestsViewModel(
    private val application: TsuApplication,
    private val callback: CommunityPublishingRequestsCallback
) : CommunityPublishingRequestsViewModel() {

    init {
        application.appComponent.inject(this)
    }

    @Inject
    lateinit var communityApi: CommunityApi

    @Inject
    lateinit var schedulers: RxSchedulers


    override fun getPendingPosts(groupId: Int) {
        val subscribe = communityApi.getPendingPosts(groupId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback.didLoadPendingPosts(it.posts)
                    },
                    onFailure = {
                        callback.didFailedToLoad(it)
                    }
                )
            }, { err ->
                err.message?.let {
                    callback.didFailedToLoad(it)
                }
            })
    }

    override fun approve(
        post: PendingPost,
        communityId: Int
    ) {
        val subscribe = communityApi.approvePost(communityId, post.id)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback.didApprovePost(post)
                    },
                    onFailure = {
                        callback.didFailApprovePost(post)
                    }
                )
            }, { err ->
                callback.didFailApprovePost(post)
            })
    }

    override fun decline(
        post: PendingPost,
        communityId: Int
    ) {
        val subscribe = communityApi.declinePost(communityId, post.id)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback.didDeclinePost(post)
                    },
                    onFailure = {
                        callback.didFailDeclinePost(post)
                    }
                )
            }, { err ->
                callback.didFailDeclinePost(post)
            })
    }
}