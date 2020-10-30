package social.tsu.android.service

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.CommunityFeedCache
import social.tsu.android.helper.PostsCache
import social.tsu.android.network.api.CommunityApi
import social.tsu.android.network.model.*
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

abstract class CommunityService : DefaultService() {

    abstract fun createCommunity(
        name: String,
        description: String,
        topicId: Int,
        moderation: Boolean,
        visibility: CommunityType,
        image: String?
    )

    abstract fun updateCommunity(
        communityId: Int,
        name: String,
        description: String,
        topicId: Int,
        moderation: Boolean,
        visibility: CommunityType,
        image: String?
    )

    abstract fun loadCommunityInfo(communityId: Int)
    abstract fun loadSuggestedCommunities()
    abstract fun joinCommunity(group: Group)
    abstract fun getCommunityFeed(id: Int, page: Int?)
    abstract fun getCommunityPosts(communityId: Int): List<Post>
    abstract fun getPublishingRequests(communityId: Int)
    abstract fun getCommunityPendingRequests(communityId: Int)
    abstract fun leave(membershipId: Int)
    abstract fun deleteCommunity(communityId: Int)
}

interface CommunityServiceCallback : DefaultServiceCallback {
    fun didLoadCommunity(community: Group)
    fun didCreateCommunity()
    fun didLoadSuggestedCommunities(communities: List<Group>)
    fun didLoadCommunityPendingRequests(requests: List<PendingRequest>)
    fun didJoinCommunity(community: Group)
    fun didFailedToJoinCommunity(community: Group)
    fun completedGetCommunityPosts(nextPage: Int?)
    fun failedGetCommunityPosts(code: Int)
    fun failedLoadCommunityPendingRequests()
    fun didLoadPendingPosts(posts: List<PendingPost>)
    fun didFailToLoadPendingPosts(message: String)
    fun didLeftGroup()
    fun didFailedToLeaveGroup(message: String)
    fun didDeleteCommunity()
    fun didUpdateCommunity(group: Group)
}

enum class CommunityType(val serverValue: String) {
    OPEN("open"), RESTRICTED("restricted"), EXCLUSIVE("exclusive")
}

class DefaultCommunityService(
    private val application: TsuApplication,
    var callback: CommunityServiceCallback?
) : CommunityService() {

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override val tag: String = "DefaultCommunityService"

    private val cache: CommunityFeedCache = CommunityFeedCache
    private val postsCache: PostsCache = PostsCache

    @Inject
    lateinit var communityApi: CommunityApi

    @Inject
    lateinit var schedulers: RxSchedulers

    init {
        application.appComponent.inject(this)
    }

    override fun loadCommunityInfo(communityId: Int) {
        compositeDisposable += communityApi.getCommunityInfo(communityId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = { result ->
                        callback?.didLoadCommunity(result.group)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error creating community", err)
            })
    }

    override fun createCommunity(
        name: String,
        description: String,
        topicId: Int,
        moderation: Boolean,
        visibility: CommunityType,
        image: String?
    ) {
        compositeDisposable += communityApi.createCommunity(
            CommunityPayload(
                Community(
                    name, description, topicId, moderation, visibility.serverValue, image
                )
            )
        )
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback?.didCreateCommunity()
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error creating community", err)
            })

    }

    override fun updateCommunity(
        communityId: Int,
        name: String,
        description: String,
        topicId: Int,
        moderation: Boolean,
        visibility: CommunityType,
        image: String?
    ) {
        compositeDisposable += communityApi.updateCommunity(
            communityId,
            CommunityPayload(
                Community(
                    name, description, topicId, moderation, visibility.serverValue, image
                )
            )
        )
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = { result ->
                        if (result.group != null) {
                            callback?.didUpdateCommunity(result.group)
                        } else {
                            callback?.didErrorWith(application.getString(R.string.generic_error_message))
                        }
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error creating community", err)
            })
    }

    override fun deleteCommunity(communityId: Int) {
        compositeDisposable += communityApi.deleteCommunity(communityId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback?.didDeleteCommunity()
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error deleting community", err)
            })
    }

    override fun loadSuggestedCommunities() {
        compositeDisposable += communityApi.getSuggestedCommunities()
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = { result ->
                        callback?.didLoadSuggestedCommunities(result.groups)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                Log.e(tag, "Error loading suggested community", err)
            })
    }

    override fun joinCommunity(group: Group) {
        compositeDisposable += communityApi.joinCommunity(group.id)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback?.didJoinCommunity(group)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                        callback?.didFailedToJoinCommunity(group)
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                callback?.didFailedToJoinCommunity(group)
                Log.e(tag, "Error joining community", err)
            })
    }

    override fun getCommunityFeed(id: Int, page: Int?) {
        //Log.d("CommunityService", "Get comm feed, commId: $id, page = $page")
        compositeDisposable += communityApi.getCommunityFeed(id, page)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = { result ->
                        val incoming = result.posts
                        /*Log.d("CommunityService", "got some posts, commId: $id, reqPage = $page, meta: ${it.body()?.meta}, respNextPage = ${it.body()?.meta?.nexPage}")
                        incoming?.let {posts ->
                            Log.d("CommunityService", "received posts count: ${posts.size}")
                        }*/
                        update(id, incoming, result.meta.nexPage)
                    },
                    onFailure = { _ ->
                        if (it.code() != 403) {
                            callback?.didErrorWith("Failed to get community posts")
                        }
                        callback?.failedGetCommunityPosts(it.code())
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                callback?.failedGetCommunityPosts(-1)
                Log.e(tag, "Error getting community feed", err)
            })
    }

    private fun update(
        communityId: Int,
        posts: List<Post>?,
        nexPage: Int?
    ) {
        if (posts == null) return

        if (posts.isEmpty()) {
            callback?.completedGetCommunityPosts(nexPage)
            return
        }

        val allPosts = posts.union(this.cache[communityId.toLong()])

        val sorted = allPosts.sortedByDescending {
            it.timestamp
        }
        this.cache[communityId.toLong()] = sorted
        sorted.forEach {
            postsCache[it.id] = it
        }
        callback?.completedGetCommunityPosts(nexPage)
    }

    override fun getCommunityPosts(communityId: Int): List<Post> {
        return cache[communityId.toLong()]
    }

    
    override fun getCommunityPendingRequests(communityId: Int) {
        compositeDisposable += communityApi.getPendingRequests(communityId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = { result ->
                        val incoming = result.pendingRequests
                        callback?.didLoadCommunityPendingRequests(incoming)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                        callback?.failedLoadCommunityPendingRequests()
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                callback?.failedLoadCommunityPendingRequests()
                Log.e(tag, "Error getting pending requests", err)
            })
    }
    
    override fun getPublishingRequests(communityId: Int) {
        compositeDisposable += communityApi.getPendingPosts(communityId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = { result ->
                        callback?.didLoadPendingPosts(result.posts)
                    },
                    onFailure = { errMsg ->
                        callback?.didErrorWith(errMsg)
                        callback?.didFailToLoadPendingPosts("Failed to load")
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
            })
    }

    override fun leave(membershipId: Int) {
        compositeDisposable += communityApi.leave(membershipId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponse(
                    application,
                    it,
                    onSuccess = {
                        callback?.didLeftGroup()
                    },
                    onFailure = { errMsg ->
                        callback?.didFailedToLeaveGroup(errMsg)
                    }
                )
            }, { err ->
                callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
            })
    }
}