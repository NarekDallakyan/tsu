package social.tsu.android.service

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.network.api.LikeApi
import social.tsu.android.network.model.LikeResponse
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

interface LikeServiceCallback: DefaultServiceCallback {
    fun didCompletePostLike(post: Post, response: LikeResponse)
    fun didCompletePostUnlike(post: Post, response: LikeResponse)
}

abstract class LikeService: DefaultService() {
    abstract fun like(post: Post)
    abstract fun unlike(post: Post)
}

class DefaultLikeService(
    private val application: TsuApplication,
    private val callback: LikeServiceCallback?
): LikeService() {

    @Inject
    lateinit var likeApi: LikeApi

    @Inject
    lateinit var schedulers: RxSchedulers

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override val tag: String
        get() = "DefaultLikeService"

    init {
        application.appComponent.inject(this)
    }

    override fun like(post: Post) {
        compositeDisposable += likeApi.likeUserPost(post.id)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe({ response ->
                    handleResponse(
                        application,
                        response,
                        onSuccess = { result ->
                            val resultPost = post.copy(
                                has_liked = true,
                                like_count = result.likeCount
                            )
                            callback?.didCompletePostLike(resultPost, result)
                        },
                        onFailure = { errMsg ->
                            Log.e(tag, "Error getting response data from body $response")
                            callback?.didErrorWith(errMsg)
                        }
                    )
                }, { err ->
                    callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                    Log.e(tag, "Error liking post", err)
                })
    }

    override fun unlike(post: Post) {
        compositeDisposable += likeApi.unlikeUserPost(post.id)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe({ response ->
                    handleResponse(
                        application,
                        response,
                        onSuccess = { result ->
                            val resultPost = post.copy(
                                has_liked = false,
                                like_count = result.likeCount
                            )
                            callback?.didCompletePostUnlike(resultPost, result)
                        },
                        onFailure = { errMsg ->
                            Log.e(tag, "Error getting response data from body $response")
                            callback?.didErrorWith(errMsg)
                        }
                    )
                }, { err ->
                    callback?.didErrorWith(err.getNetworkCallErrorMessage(application))
                    Log.e(tag, "Error unliking post", err)
                })
    }


}