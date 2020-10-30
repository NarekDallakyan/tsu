package social.tsu.android.service.post_feed

import android.app.Application
import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.data.local.entity.Post
import social.tsu.android.data.local.entity.PostPayload
import social.tsu.android.data.local.entity.PostsPayload
import social.tsu.android.helper.runIfUserIsAuthenticated
import social.tsu.android.network.api.CommunityApi
import social.tsu.android.network.api.PostApi
import social.tsu.android.network.model.*
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.*
import social.tsu.android.utils.hashtags
import javax.inject.Inject


abstract class PostFeedService: DefaultService() {

    abstract fun createPost(
        text: String,
        streamId: String? = null,
        privacy: Int = 0,
        serviceCallback: ServiceCallback<Post>
    )

    abstract fun getPostFeed(
        start: Int?,
        count: Int,
        serviceCallback: ServiceCallback<PostFeedResults>
    )

    abstract fun getPostFeedTrending(
        start: Int?,
        count: Int,
        serviceCallback: ServiceCallback<PostFeedResults>,
        userInitiatedRefresh: Boolean = false
    )

    abstract fun getUserPostFeed(
        userId: Int,
        lastPostId: Int? = null,
        serviceCallback: ServiceCallback<List<Post>>
    )

    abstract fun getHashtagFeed(
        hashtag: String,
        nextPage: Int?,
        serviceCallback: ServiceCallback<UserMediaResponse>
    )

    abstract fun getUserPhotoPostFeed(
        userId: Int,
        lastPostId: Int? = null,
        count: Int,
        serviceCallback: ServiceCallback<UserMediaResponse>
    )

    abstract fun getUserVideoPostFeed(
        userId: Int,
        nextPage: Int? = null,
        count: Int,
        serviceCallback: ServiceCallback<UserMediaResponse>
    )

    abstract fun getDiscoveryFeed(
        nextPage: Int? = null,
        count: Int,
        serviceCallback: ServiceCallback<DiscoveryFeedResponse>
    )

    abstract fun getCommunityFeed(
        id: Int,
        page: Int?,
        serviceCallback: ServiceCallback<PostFeedResults>
    )

    abstract fun getPostById(postId: Long, serviceCallback: ServiceCallback<Post>)
    abstract fun savePostEdit(postId: Long, value: String, serviceCallback: ServiceCallback<Post>)
    abstract fun report(postId: Long, reasonId: Int, serviceCallback: ServiceCallback<Boolean>)
    abstract fun delete(postId: Long, serviceCallback: ServiceCallback<Boolean>)

    data class PostFeedResults(val posts: List<Post>, val nextPage: Int?)
}

class DefaultPostFeedService @Inject constructor(
    private val application: Application,
    private val postApi: PostApi,
    private val communityApi: CommunityApi,
    private val schedulers: RxSchedulers
): PostFeedService() {

    override val tag: String = "DefaultPostFeedService"

    override val compositeDisposable = CompositeDisposable()

    override fun createPost(
        text: String,
        streamId: String?,
        privacy: Int,
        serviceCallback: ServiceCallback<Post>
    ) {
        if (text.hashtags().size > PostApi.MAX_HASHTAG_COUNT) {
            serviceCallback.onFailure(application.getString(R.string.create_post_hashtag_error))
            return
        }

        runIfUserIsAuthenticated {
            compositeDisposable += postApi.createPost(CreatePostPayload(text, streamId, privacy))
                .observeOn(schedulers.main())
                .subscribeOn(schedulers.io())
                .subscribe({ response ->
                    handleResponseWithWrapper(application, response, object : ServiceCallback<PostPayload> {
                        override fun onSuccess(result: PostPayload) {
                            serviceCallback.onSuccess(result.post)
                        }

                        override fun onFailure(errMsg: String) {
                            serviceCallback.onFailure(errMsg)
                        }
                    })
                }, { err ->
                    handleApiCallError(application, err, serviceCallback)
                })
        }

    }

    override fun getPostFeed(
        start: Int?,
        count: Int,
        serviceCallback: ServiceCallback<PostFeedResults>
    ) {
        runIfUserIsAuthenticated {
            compositeDisposable += postApi.getFeedV2Chrono(start, count)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe({
                    handleResponseWithWrapper(
                        application,
                        it,
                        object : ServiceCallback<PostsResponseV2> {
                            override fun onSuccess(result: PostsResponseV2) {
                                serviceCallback.onSuccess(
                                    PostFeedResults(
                                        result.sources.flatMap { postV2 -> postV2.posts },
                                        result.next_page
                                    )
                                )
                            }

                            override fun onFailure(errMsg: String) {
                                serviceCallback.onFailure(errMsg)
                            }
                        }
                    )
                }, { err ->
                    handleApiCallError(application, err, serviceCallback)
                    Log.e(tag, "Error getting post status", err)
                })
        }

    }

    override fun getPostFeedTrending(
        start: Int?,
        count: Int,
        serviceCallback: ServiceCallback<PostFeedResults>,
        userInitiatedRefresh: Boolean
    ) {
        runIfUserIsAuthenticated {
            compositeDisposable += postApi.getFeedV3Trending(start, count, userInitiatedRefresh)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe({
                    handleResponseWithWrapper(
                        application,
                        it,
                        object : ServiceCallback<PostsResponseV2> {
                            override fun onSuccess(result: PostsResponseV2) {
                                serviceCallback.onSuccess(
                                    PostFeedResults(
                                        result.sources.mapIndexed { _, postV2 -> postV2.posts.first() },
                                        result.next_page
                                    )
                                )
                            }

                            override fun onFailure(errMsg: String) {
                                serviceCallback.onFailure(errMsg)
                            }
                        }
                    )
                }, { err ->
                    handleApiCallError(application, err, serviceCallback)
                    Log.e(tag, "Error getting post status", err)
                })
        }

    }

    override fun getUserPostFeed(
        userId: Int,
        lastPostId: Int?,
        serviceCallback: ServiceCallback<List<Post>>
    ) {

        runIfUserIsAuthenticated {

            postApi.getUserFeed(userId, lastPostId)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe({ response ->
                    handleResponseWithWrapper(
                        application,
                        response,
                        object : ServiceCallback<PostsPayload> {
                            override fun onSuccess(result: PostsPayload) {
                                result.posts?.let { posts ->
                                    serviceCallback.onSuccess(posts)
                                }
                            }

                            override fun onFailure(errMsg: String) {
                                serviceCallback.onFailure(errMsg)
                            }
                        }
                    )
                }, { err ->
                    handleApiCallError(application, err, serviceCallback)
                    Log.e(tag, "Error getting post status", err)
                })
        }

    }

    override fun getHashtagFeed(
        hashtag: String,
        nextPage: Int?,
        serviceCallback: ServiceCallback<UserMediaResponse>
    ) {
        runIfUserIsAuthenticated {

            postApi.getHashtagPosts(hashtag, nextPage)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe({ response ->
                    handleResponse(
                        application,
                        response,
                        object : ServiceCallback<UserMediaResponse> {
                            override fun onSuccess(result: UserMediaResponse) {
                                serviceCallback.onSuccess(result)
                            }

                            override fun onFailure(errMsg: String) {
                                serviceCallback.onFailure(errMsg)
                            }
                        }
                    )
                }, { err ->
                    handleApiCallError(application, err, serviceCallback)
                    Log.e(tag, "Error getting post status", err)
                })
        }

    }

    override fun getUserPhotoPostFeed(
        userId: Int,
        lastPostId: Int?,
        count: Int,
        serviceCallback: ServiceCallback<UserMediaResponse>
    ) {

        runIfUserIsAuthenticated {

            postApi.getUserPhotos(userId, lastPostId, count)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe({ response ->
                    handleResponse(
                        application,
                        response,
                        object : ServiceCallback<UserMediaResponse> {
                            override fun onSuccess(result: UserMediaResponse) {
                                serviceCallback.onSuccess(result)
                            }

                            override fun onFailure(errMsg: String) {
                                serviceCallback.onFailure(errMsg)
                            }
                        }
                    )
                }, { err ->
                    handleApiCallError(application, err, serviceCallback)
                    Log.e(tag, "Error getting post status", err)
                })
        }

    }

    override fun getUserVideoPostFeed(
        userId: Int,
        nextPage: Int?,
        count: Int,
        serviceCallback: ServiceCallback<UserMediaResponse>
    ) {

        runIfUserIsAuthenticated {

            postApi.getUserVideos(userId, nextPage, count)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe({ response ->
                    handleResponse(
                        application,
                        response,
                        object : ServiceCallback<UserMediaResponse> {
                            override fun onSuccess(result: UserMediaResponse) {
                                // For some reason can crash with NPE
                                val list = result.data.filter {
                                    it != null && it.groupId == null && !it.is_share
                                            && it.stream != null && it.stream.duration > 0
                                }
                                serviceCallback.onSuccess(UserMediaResponse(list, result.next_page))
                            }

                            override fun onFailure(errMsg: String) {
                                serviceCallback.onFailure(errMsg)
                            }
                        }
                    )
                }, { err ->
                    handleApiCallError(application, err, serviceCallback)
                    Log.e(tag, "Error getting post status", err)
                })
        }

    }

    override fun getDiscoveryFeed(
        nextPage: Int?,
        count: Int,
        serviceCallback: ServiceCallback<DiscoveryFeedResponse>
    ) {

        runIfUserIsAuthenticated {

            postApi.getDiscoveryFeed(nextPage, count)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe({ response ->
                    handleResponse(
                        application,
                        response,
                        object : ServiceCallback<DiscoveryFeedResponse> {
                            override fun onSuccess(result: DiscoveryFeedResponse) {
                                // For some reason can crash with NPE
                                /*val list = result.data.filter {
                                    it != null && it.groupId == null && !it.is_share
                                            && it.stream != null && it.stream.duration > 0
                                }*/
                                serviceCallback.onSuccess(result)
                            }

                            override fun onFailure(errMsg: String) {
                                serviceCallback.onFailure(errMsg)
                            }
                        }
                    )
                }, { err ->
                    handleApiCallError(application, err, serviceCallback)
                    Log.e(tag, "Error getting post status", err)
                })
        }

    }

    override fun getCommunityFeed(
        id: Int,
        page: Int?,
        serviceCallback: ServiceCallback<PostFeedResults>
    ) {
        runIfUserIsAuthenticated {
            compositeDisposable += communityApi.getCommunityFeed(id, page)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe({
                    handleResponse(
                        application,
                        it,
                        object : ServiceCallback<CommunityFeedResponse> {
                            override fun onSuccess(result: CommunityFeedResponse) {
                                serviceCallback.onSuccess(
                                    PostFeedResults(
                                        result.posts,
                                        result.meta.nexPage
                                    )
                                )
                            }

                            override fun onFailure(errMsg: String) {
                            serviceCallback.onFailure(errMsg)
                        }
                    })
                }, { err ->
                    handleApiCallError(application, err, serviceCallback)
                    Log.e(tag, "Error getting community feed", err)
                })
        }

    }

    override fun getPostById(postId: Long, serviceCallback: ServiceCallback<Post>) {
        compositeDisposable += postApi.getPost(postId.toInt())
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseWithWrapper(
                    application,
                    it,
                    serviceCallback
                )
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error getting post status", err)
            })
    }

    override fun savePostEdit(postId: Long, value: String, serviceCallback: ServiceCallback<Post>) {
        if (value.hashtags().size > PostApi.MAX_HASHTAG_COUNT) {
            serviceCallback.onFailure(application.getString(R.string.create_post_hashtag_error))
            return
        }

        compositeDisposable += postApi.editPostContent(postId, EditPostDTO(value, postId))
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({
                handleResponseWithWrapper(
                    application,
                    it,
                    object : ServiceCallback<EditPostResponse> {
                        override fun onSuccess(result: EditPostResponse) {
                            getPostById(postId, serviceCallback)
                        }

                        override fun onFailure(errMsg: String) {
                            handleApiCallError(application, Throwable(errMsg), serviceCallback)
                        }
                    }
                )
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
                Log.e(tag, "Error getting post status", err)
            })
    }

    override fun report(postId: Long, reasonId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += postApi.reportPost(ReportPostPayload(postId, reasonId))
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe({ response ->
                handleResponseResult(
                    application,
                    response,
                    serviceCallback
                )
            }, { err ->
                handleApiCallError(application, err, serviceCallback)
            })
    }

    override fun delete(postId: Long, serviceCallback: ServiceCallback<Boolean>) {

        compositeDisposable += postApi.deletePost(postId)
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe(
                { response ->
                    handleResponseResult(
                        application,
                        response,
                        serviceCallback
                    )
                },
                { err ->
                    handleApiCallError(application, err, serviceCallback)
                }
            )
    }

}
