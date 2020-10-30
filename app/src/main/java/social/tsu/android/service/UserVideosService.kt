package social.tsu.android.service

import android.util.Log
import androidx.lifecycle.LiveData
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.dao.PostFeedDao
import social.tsu.android.data.local.entity.FeedSource
import social.tsu.android.data.local.entity.Post
import social.tsu.android.execute
import social.tsu.android.network.api.PostApi
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

abstract class UserVideosService : DefaultService() {
    abstract var callback: UserVideosServiceCallback?

    abstract fun loadUserVideos(userId: Int, pageKey: Int?, totalVideo: Int = 50)
    abstract fun getUserVideos(userId: Int): LiveData<List<Post>>
}

interface UserVideosServiceCallback {
    fun completedGetUserVideos(nextPage: Int?)
    fun completedError(message: String)
}

class DefaultUserVideosService(
    private val application: TsuApplication,
    callback: UserVideosServiceCallback
) : UserVideosService() {


    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var api: PostApi

    @Inject
    lateinit var postFeedDao: PostFeedDao

    override var callback: UserVideosServiceCallback? = callback

    override val compositeDisposable = CompositeDisposable()

    override val tag: String
        get() = "DefaultUserVideosService"

    init {
        application.appComponent.inject(this)
    }

    private var isLoading = false

    override fun loadUserVideos(userId: Int, pageKey: Int?, totalVideo: Int) {
        if (isLoading) return
        isLoading = true
        compositeDisposable += api.getUserVideos(
            userId = userId,
            pageKey = pageKey,
            count = totalVideo
        )
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe(
                { response ->
                    handleResponse(
                        application,
                        response,
                        onSuccess = { result ->
                            val incoming = result.data

                            val list = incoming.filter {
                                it != null && it.groupId == null && !it.is_share
                                        && it.stream != null && it.stream.duration > 0
                            }
                            execute {
                                postFeedDao.savePosts(list, FeedSource.Type.USER_VIDEOS)
                            }
                            isLoading = false
                            callback?.completedGetUserVideos(result.next_page)
                        },
                        onFailure = {
                            Log.e(tag, "error ${response.code()}")
                            callback?.completedGetUserVideos(null)
                        }
                    )
                },
                { err ->
                    Log.e(tag, "error = ${err.message}")
                    callback?.completedGetUserVideos(null)
                }
            )
    }

    override fun getUserVideos(userId: Int): LiveData<List<Post>> {
        return postFeedDao.getUserVideoPostsLiveData(userId)
    }

}