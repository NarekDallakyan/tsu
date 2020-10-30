package social.tsu.android.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.gson.Gson
import social.tsu.android.data.local.dao.PostFeedDao
import social.tsu.android.data.local.entity.FeedSource
import social.tsu.android.data.local.entity.Post
import social.tsu.android.execute
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.*
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.post_feed.*
import social.tsu.android.ui.model.Data
import javax.inject.Inject

class PostFeedRepository @Inject constructor(
    private val postFeedService: PostFeedService,
    private val postLikeService: PostLikeService,
    private val postShareService: PostShareService,
    private val postBlockService: PostBlockService,
    private val postSupportService: PostSupportService,
    private val postFeedDao: PostFeedDao
) {

    private val likeLoadStateMap = HashMap<Long, MutableLiveData<Data<Boolean>>>()
    private val sharePostLoadStateMap = HashMap<Long, MutableLiveData<Data<Boolean>>>()

    private val _initialLoadState = MutableLiveData<Data<Boolean>>()
    val initialLoadState: LiveData<Data<Boolean>> = _initialLoadState

    private val _userRefreshLoadState = MutableLiveData<Data<Boolean>>()
    val userRefreshLoadState: LiveData<Data<Boolean>> = _userRefreshLoadState

    private val _loadState = MutableLiveData<Data<Boolean>>()
    val loadState: LiveData<Data<Boolean>> = _loadState

    private var actionRetry: () -> Unit = {}
    private var config: PagedList.Config = PagedList.Config.Builder()
        .setPageSize(PAGE_SIZE)
        .setInitialLoadSizeHint(PAGE_SIZE)
        .setEnablePlaceholders(false)
        .build()

    companion object {
        const val PAGE_SIZE = 10
    }

    private val gson: Gson = Gson()

    fun getMainFeedChrono(): LiveData<PagedList<Post>> {
        refreshPosts(FeedSource.Type.MAIN, true)
        return LivePagedListBuilder(postFeedDao.getPosts(FeedSource.Type.MAIN), config)
            .setBoundaryCallback(
                MainFeedBoundaryCallback(
                    postFeedService,
                    postFeedDao,
                    _loadState,
                    ::setRetry
                )
            )
            .build()
    }

    fun getMainFeedTrending(isInitial: Boolean): LiveData<PagedList<Post>> {
        return LivePagedListBuilder(postFeedDao.getPostsWithOrder(FeedSource.Type.ORDER), config)
            .setBoundaryCallback(
                MainFeedOrderBoundaryCallback(
                    postFeedService,
                    postFeedDao,
                    _loadState,
                    ::setRetry,
                    isInitial
                )
            )
            .build()
    }


    fun getUserPosts(userId: Int): LiveData<PagedList<Post>> {
        refreshUserPosts(userId)
        return LivePagedListBuilder(postFeedDao.getUserPosts(userId), config)
            .setBoundaryCallback(
                UserFeedBoundaryCallback(
                    userId,
                    postFeedService,
                    postFeedDao,
                    _loadState,
                    ::setRetry
                )
            ).build()
    }

    fun getUserPhotoPosts(userId: Int, includePosition: Int): LiveData<PagedList<Post>> {
        val initialLoadSize = if (includePosition < PAGE_SIZE) {
            PAGE_SIZE
        } else {
            includePosition + (PAGE_SIZE - includePosition % PAGE_SIZE)
        }
        val config: PagedList.Config = PagedList.Config.Builder()
            .setPageSize(PAGE_SIZE)
            .setInitialLoadSizeHint(initialLoadSize)
            .setEnablePlaceholders(false)
            .build()
        return LivePagedListBuilder(postFeedDao.getUserPhotoPosts(userId), config)
            .setBoundaryCallback(
                UserPhotosFeedBoundaryCallback(
                    userId,
                    postFeedService,
                    postFeedDao,
                    initialLoadSize,
                    PAGE_SIZE,
                    _loadState,
                    ::setRetry
                )
            ).build()
    }

    fun getHashtagPosts(hashtag: String, includePosition: Int): LiveData<PagedList<Post>> {
        val initialLoadSize = if (includePosition < PAGE_SIZE) {
            PAGE_SIZE
        } else {
            includePosition + (PAGE_SIZE - includePosition % PAGE_SIZE)
        }
        val config: PagedList.Config = PagedList.Config.Builder()
            .setPageSize(PAGE_SIZE)
            .setInitialLoadSizeHint(initialLoadSize)
            .setEnablePlaceholders(false)
            .build()
        return LivePagedListBuilder(postFeedDao.getHashtagPosts(hashtag), config)
            .setBoundaryCallback(
                HashtagFeedBoundaryCallback(
                    hashtag,
                    postFeedService,
                    postFeedDao,
                    initialLoadSize,
                    PAGE_SIZE,
                    _loadState,
                    ::setRetry
                )
            ).build()
    }

    fun getUserVideoPosts(userId: Int, includePosition: Int): LiveData<PagedList<Post>> {
        val initialLoadSize = if (includePosition < PAGE_SIZE) {
            PAGE_SIZE
        } else {
            includePosition + (PAGE_SIZE - includePosition % PAGE_SIZE)
        }
        val config: PagedList.Config = PagedList.Config.Builder()
            .setPageSize(PAGE_SIZE)
            .setInitialLoadSizeHint(initialLoadSize)
            .setEnablePlaceholders(false)
            .build()
        return LivePagedListBuilder(postFeedDao.getUserVideoPosts(userId), config)
            .setBoundaryCallback(
                UserVideosFeedBoundaryCallback(
                    userId,
                    postFeedService,
                    postFeedDao,
                    initialLoadSize,
                    PAGE_SIZE,
                    _loadState,
                    ::setRetry
                )
            ).build()
    }

    fun getDiscoveryFeed(userId: Int, includePosition: Int): LiveData<PagedList<Post>> {
        val initialLoadSize = if (includePosition < PAGE_SIZE) {
            PAGE_SIZE
        } else {
            includePosition + (PAGE_SIZE - includePosition % PAGE_SIZE)
        }
        val config: PagedList.Config = PagedList.Config.Builder()
            .setPageSize(PAGE_SIZE)
            .setInitialLoadSizeHint(initialLoadSize)
            .setEnablePlaceholders(false)
            .build()
        return LivePagedListBuilder(postFeedDao.getDiscoveryFeedPosts(), config)
            .setBoundaryCallback(
                DiscoveryFeedBoundaryCallback(
                    userId,
                    postFeedService,
                    postFeedDao,
                    initialLoadSize,
                    PAGE_SIZE,
                    _loadState,
                    ::setRetry
                )
            ).build()
    }

    fun getCommunityPosts(communityId: Int): LiveData<PagedList<Post>> {
        refreshCommunityPosts(communityId)
        return LivePagedListBuilder(
            postFeedDao.getCommunityPosts(
                communityId,
                FeedSource.Type.COMMUNITY
            ), config
        )
            .setBoundaryCallback(
                CommunityFeedBoundaryCallback(
                    communityId,
                    postFeedService,
                    postFeedDao,
                    _loadState,
                    ::setRetry
                )
            )
            .build()
    }

    fun refreshUserPosts(userId: Int) {
        postFeedService.getUserPostFeed(
            userId,
            null,
            object : ServiceCallback<List<Post>> {
                override fun onSuccess(result: List<Post>) {
                    _initialLoadState.value = Data.Success(true)
                    val finalResults = result.filter { !it.is_share }
                    execute {
                        postFeedDao.savePosts(finalResults, FeedSource.Type.USER)
                    }
                }

                override fun onFailure(errMsg: String) {
                    _initialLoadState.value = Data.Error(Throwable(errMsg))
                }
            })

    }

    fun refreshUserPhotoPosts(userId: Int) {
        postFeedService.getUserPhotoPostFeed(
            userId,
            null,
            PAGE_SIZE,
            object : ServiceCallback<UserMediaResponse> {
                override fun onSuccess(result: UserMediaResponse) {
                    _initialLoadState.value = Data.Success(true)
                    val finalResults = result.data.filter { !it.is_share }
                    execute {
                        postFeedDao.savePosts(finalResults, FeedSource.Type.USER_PHOTOS)
                    }
                }

                override fun onFailure(errMsg: String) {
                    _initialLoadState.value = Data.Error(Throwable(errMsg))
                }
            })

    }

    fun refreshUserVideoPosts(userId: Int) {
        postFeedService.getUserVideoPostFeed(
            userId,
            null,
            PAGE_SIZE,
            object : ServiceCallback<UserMediaResponse> {
                override fun onSuccess(result: UserMediaResponse) {
                    _initialLoadState.value = Data.Success(true)
                    execute {
                        postFeedDao.savePosts(result.data, FeedSource.Type.USER_VIDEOS)
                    }
                }

                override fun onFailure(errMsg: String) {
                    _initialLoadState.value = Data.Error(Throwable(errMsg))
                }
            })

    }

    fun refreshDiscoveryFeedPosts(userId: Int) {
        postFeedService.getDiscoveryFeed(
            null,
            PAGE_SIZE,
            object : ServiceCallback<DiscoveryFeedResponse> {
                override fun onSuccess(result: DiscoveryFeedResponse) {
                    _initialLoadState.value = Data.Success(true)
                    execute {
                        postFeedDao.savePosts(result.data, FeedSource.Type.DISCOVERY_FEED)
                    }
                }

                override fun onFailure(errMsg: String) {
                    _initialLoadState.value = Data.Error(Throwable(errMsg))
                }
            })

    }


    fun refreshCommunityPosts(communityId: Int, userInitiatedRefresh: Boolean = false) {
        postFeedService.getCommunityFeed(communityId, null,
            object : ServiceCallback<PostFeedService.PostFeedResults> {
                override fun onSuccess(result: PostFeedService.PostFeedResults) {
                    if (!userInitiatedRefresh) {
                        _initialLoadState.value = Data.Success(true)
                    } else {
                        _userRefreshLoadState.value = Data.Success(true)
                    }
                    execute {
                        postFeedDao.savePosts(result.posts, FeedSource.Type.COMMUNITY)
                    }
                }

                override fun onFailure(errMsg: String) {
                    if (!userInitiatedRefresh) {
                        _initialLoadState.value = Data.Success(true)
                    } else {
                        _userRefreshLoadState.value = Data.Success(true)
                    }
                }
            })
    }

    fun refreshHashtagPosts(hashtag: String) {
        postFeedService.getHashtagFeed(
            hashtag,
            null,
            object : ServiceCallback<UserMediaResponse> {
                override fun onSuccess(result: UserMediaResponse) {
                    _initialLoadState.value = Data.Success(true)
                    val finalResults = result.data.filter { !it.is_share }
                    execute {
                        postFeedDao.savePosts(finalResults, FeedSource.Type.HASHTAG)
                    }
                }

                override fun onFailure(errMsg: String) {
                    _initialLoadState.value = Data.Error(Throwable(errMsg))
                }
            })
    }

    fun report(postId: Long, reasonId: Int): LiveData<Data<Boolean>> {
        val reportLoadState = MutableLiveData<Data<Boolean>>()

        reportLoadState.value = Data.Loading()
        postFeedService.report(postId, reasonId, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                reportLoadState.value = Data.Success(result)
                execute { postFeedDao.deletePost(postId) }
            }

            override fun onFailure(errMsg: String) {
                reportLoadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return reportLoadState
    }

    fun delete(postId: Long):LiveData<Data<Boolean>>{
        val deleteLoadState= MutableLiveData<Data<Boolean>>()

        deleteLoadState.value = Data.Loading()
        postFeedService.delete(postId, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                deleteLoadState.value = Data.Success(result)
                execute { postFeedDao.deletePost(postId) }
            }

            override fun onFailure(errMsg: String) {
                deleteLoadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return deleteLoadState
    }

    fun getPostById(postId: Long): LiveData<PagedList<Post>> {

        _initialLoadState.value = Data.Loading()
        postFeedService.getPostById(postId, object : ServiceCallback<Post> {
            override fun onSuccess(result: Post) {
                _initialLoadState.value = Data.Success(true)
                execute {
                    postFeedDao.upsertPosts(result)
                }
            }

            override fun onFailure(errMsg: String) {
                _initialLoadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return LivePagedListBuilder(postFeedDao.getPostById(postId), config).build()
    }

    fun getPost(postId: Long): LiveData<Post> {
        postFeedService.getPostById(postId, object : ServiceCallback<Post> {
            override fun onSuccess(result: Post) {
                _initialLoadState.value = Data.Success(true)
                execute {
                    postFeedDao.upsertPosts(result)
                }
            }

            override fun onFailure(errMsg: String) {
                _initialLoadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return postFeedDao.getPost(postId)
    }

    fun likePost(post: Post): LiveData<Data<Boolean>> {
        if (likeLoadStateMap[post.id] != null) {
            return likeLoadStateMap[post.id]!!
        }

        val likeLoadState = MutableLiveData<Data<Boolean>>()

        likeLoadState.value = Data.Loading()
        likeLoadStateMap[post.id] = likeLoadState
        execute { postFeedDao.likePost(post.id, post.like_count + 1) }
        postLikeService.like(post.id, object : ServiceCallback<LikeResponse> {
            override fun onSuccess(result: LikeResponse) {
                likeLoadState.value = Data.Success(true)
                if (result.likeCount > 0 && result.likeCount != post.like_count + 1) {
                    execute {
                        postFeedDao.likePost(post.id, result.likeCount)
                    }
                }
                Log.d("POST_FEED_LIKE", "LIKE POST SUCCESS: ${Gson().toJson(post)}")

                likeLoadStateMap.remove(post.id)
            }

            override fun onFailure(errMsg: String) {
                likeLoadState.value = Data.Error(Throwable(errMsg))
                execute { postFeedDao.unlikePost(post.id, post.like_count - 1) }

                likeLoadStateMap.remove(post.id)
                Log.e(
                    "POST_FEED_LIKE",
                    "LIKE POST FAILED WITH ERROR $errMsg FOR POST: ${Gson().toJson(post)}"
                )
            }
        })
        return likeLoadState
    }

    fun supportPost(postId: Long): LiveData<Data<Boolean>> {
        val supportLoadState = MutableLiveData<Data<Boolean>>()
        supportLoadState.value = Data.Loading()
        postSupportService.support(postId, object : ServiceCallback<SupportResponse> {
            override fun onSuccess(result: SupportResponse) {
                supportLoadState.value = Data.Success(true)
            }

            override fun onFailure(errMsg: String) {
                supportLoadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return supportLoadState
    }

    fun sharePost(postId: Long): LiveData<Data<Boolean>> {
        if (sharePostLoadStateMap[postId] != null) {
            return sharePostLoadStateMap[postId]!!
        }
        val shareLoadState = MutableLiveData<Data<Boolean>>()

        shareLoadState.value = Data.Loading()
        postShareService.sharePost(postId, object : ServiceCallback<ShareResponseDTO> {
            override fun onSuccess(result: ShareResponseDTO) {
                shareLoadState.value = Data.Success(true)
                execute { postFeedDao.sharePost(postId, result.shareCount) }
                sharePostLoadStateMap.remove(postId)
            }

            override fun onFailure(errMsg: String) {
                shareLoadState.value = Data.Error(Throwable(errMsg))
                sharePostLoadStateMap.remove(postId)
            }
        })
        return shareLoadState
    }

    fun unlikePost(post: Post): LiveData<Data<Boolean>> {
        if (likeLoadStateMap[post.id] != null) {
            return likeLoadStateMap[post.id]!!
        }
        val likeLoadState = MutableLiveData<Data<Boolean>>()

        likeLoadState.value = Data.Loading()
        val currUserId = AuthenticationHelper.currentUserId?.toLong()
        val likeList = post.like_list.filter {
            it?.id != currUserId
        }
        val listJson = gson.toJson(likeList)
        execute { postFeedDao.unlikePost(post.id, post.like_count - 1, listJson) }
        postLikeService.unlike(post.id, object : ServiceCallback<LikeResponse> {
            override fun onSuccess(result: LikeResponse) {
                likeLoadState.value = Data.Success(true)

                if (result.likeCount > -1 && result.likeCount != post.like_count - 1) {
                    execute { postFeedDao.unlikePost(post.id, result.likeCount, listJson) }
                }

                likeLoadStateMap.remove(post.id)
            }

            override fun onFailure(errMsg: String) {
                likeLoadState.value = Data.Error(Throwable(errMsg))
                execute {
                    val listJson = gson.toJson(post.like_list)
                    postFeedDao.likePost(post.id, post.like_count, listJson)
                }

                likeLoadStateMap.remove(post.id)
            }
        })
        return likeLoadState
    }

    fun unsharePost(post: Post): LiveData<Data<Boolean>> {
        if (sharePostLoadStateMap[post.id] != null) {
            return sharePostLoadStateMap[post.id]!!
        }
        val shareLoadState = MutableLiveData<Data<Boolean>>()

        post.shared_id?.let {
            shareLoadState.value = Data.Loading()
            postShareService.unsharePost(it, object : ServiceCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    shareLoadState.value = Data.Success(result)
                    execute { postFeedDao.unsharePost(it) }
                    sharePostLoadStateMap.remove(post.id)
                }

                override fun onFailure(errMsg: String) {
                    shareLoadState.value = Data.Error(Throwable(errMsg))
                    sharePostLoadStateMap.remove(post.id)
                }
            })
        }

        return shareLoadState
    }

    fun blockUser(userId: Int): LiveData<Data<String>> {
        val blockLoadState = MutableLiveData<Data<String>>()

        blockLoadState.value = Data.Loading()
        postBlockService.blockUser(userId, object : ServiceCallback<String> {
            override fun onSuccess(result: String) {
                blockLoadState.value = Data.Success(result)
                execute { postFeedDao.deleteAllPostsByUser(userId) }
            }

            override fun onFailure(errMsg: String) {
                blockLoadState.value = Data.Error(Throwable(errMsg))
            }
        })

        return blockLoadState
    }

    fun unblockUser(userId: Int): LiveData<Data<String>> {
        val blockLoadState = MutableLiveData<Data<String>>()

        blockLoadState.value = Data.Loading()
        postBlockService.unblockUser(userId, object : ServiceCallback<String> {
            override fun onSuccess(result: String) {
                blockLoadState.value = Data.Success(result)
            }

            override fun onFailure(errMsg: String) {
                blockLoadState.value = Data.Error(Throwable(errMsg))
            }
        })

        return blockLoadState
    }

    fun createPost(text: String, isTrendingFeed: Boolean): LiveData<Data<Boolean>> {
        val createLoadState = MutableLiveData<Data<Boolean>>()

        createLoadState.value = Data.Loading()
        postFeedService.createPost(text, serviceCallback = object : ServiceCallback<Post> {
            override fun onSuccess(result: Post) {
                createLoadState.value = Data.Success(true)
                execute {
                    val feedType = when (isTrendingFeed) {
                        true -> FeedSource.Type.ORDER
                        false -> FeedSource.Type.MAIN
                    }
                    postFeedDao.savePosts(result, feedType)
                }
            }

            override fun onFailure(errMsg: String) {
                createLoadState.value = Data.Error(Throwable(errMsg))
            }
        })

        return createLoadState
    }

    fun savePostEdit(postId: Long, value: String): LiveData<Data<Boolean>> {
        val editLoadState = MutableLiveData<Data<Boolean>>()
        editLoadState.value = Data.Loading()
        postFeedService.savePostEdit(postId, value, object : ServiceCallback<Post> {
            override fun onSuccess(result: Post) {
                editLoadState.value = Data.Success(true)
                execute { postFeedDao.upsertPosts(result) }
            }

            override fun onFailure(errMsg: String) {
                editLoadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return editLoadState
    }

    fun refreshPosts(type: FeedSource.Type, isInitial: Boolean = false, userInitiatedRefresh: Boolean = false) {
        if (isInitial) {
            _initialLoadState.value = Data.Loading()
        } else if (userInitiatedRefresh) {
            _userRefreshLoadState.value = Data.Loading()
        }
        postFeedService.getPostFeed(null, PAGE_SIZE,
            object : ServiceCallback<PostFeedService.PostFeedResults> {
                override fun onSuccess(result: PostFeedService.PostFeedResults) {
                    if (isInitial) {
                        _initialLoadState.value = Data.Success(true)
                    } else if (userInitiatedRefresh) {
                        _userRefreshLoadState.value = Data.Success(true)
                    }
                    execute {
                        Log.d("ABCD", "savePosts() - BY REPOSITORY REFRESH POSTS")
                         postFeedDao.savePosts(result.posts, type)
                    }
                }

                override fun onFailure(errMsg: String) {
                    if (isInitial) {
                        _initialLoadState.value = Data.Error(Throwable(errMsg))
                    } else {
                        _userRefreshLoadState.value = Data.Error(Throwable(errMsg))
                    }
                }
            })
    }

    fun refreshTrendingPosts(type: FeedSource.Type, isInitial: Boolean = false, userInitiatedRefresh: Boolean = false) {
        if (isInitial) {
            _initialLoadState.value = Data.Loading()
        } else if (userInitiatedRefresh) {
            _userRefreshLoadState.value = Data.Loading()
        }
        postFeedService.getPostFeedTrending(null, PAGE_SIZE,
            object : ServiceCallback<PostFeedService.PostFeedResults> {
                override fun onSuccess(result: PostFeedService.PostFeedResults) {
                    if (isInitial) {
                        _initialLoadState.value = Data.Success(true)
                    } else if (userInitiatedRefresh) {
                        _userRefreshLoadState.value = Data.Success(true)
                    }
                    execute {
                        removeCachePosts()
                        postFeedDao.savePosts(result.posts, type, true)
                    }
                }

                override fun onFailure(errMsg: String) {
                    if (isInitial) {
                        _initialLoadState.value = Data.Error(Throwable(errMsg))
                    } else {
                        _userRefreshLoadState.value = Data.Error(Throwable(errMsg))
                    }
                }
            }, userInitiatedRefresh)
    }

    fun removeCachePosts() = execute {
        Log.d("PostFeedRepository", "removeCachePosts" )
        postFeedDao.removeCache()
    }

    private fun setRetry(retryAction: () -> Unit) {
        actionRetry = retryAction
    }

    fun retry() {
        actionRetry.invoke()
    }

}

