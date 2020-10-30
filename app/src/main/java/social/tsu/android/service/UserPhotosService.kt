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
import social.tsu.android.network.api.UserApi
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

abstract class UserPhotosService: DefaultService(){
    abstract var callback: UserPhotosServiceCallback?

    abstract fun getUserPhotos(userId: Int, pageKey: Int?,totalImage : Int =10)
    abstract fun getUserPhotos(userId: Int): LiveData<List<Post>>
}

interface UserPhotosServiceCallback {
    fun completedGetUserPhotos(nextPage: Int?)
    fun completedError(message: String)
}

class DefaultUserPhotosService(private val application: TsuApplication, callback: UserPhotosServiceCallback): UserPhotosService() {


    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var api: UserApi

    @Inject
    lateinit var postFeedDao: PostFeedDao

    private val profileImageService: DefaultUserProfileImageService by lazy {
        DefaultUserProfileImageService(application)
    }

    override var callback: UserPhotosServiceCallback? = callback

    override val compositeDisposable = CompositeDisposable()

    override val tag: String
        get() = "DefaultUserPhotosService"

    private var isLoading = false

    init {
        application.appComponent.inject(this)
    }

    override fun getUserPhotos(userId: Int, pageKey: Int?, totalImage : Int) {
        if (isLoading) return
        isLoading = true
        compositeDisposable += api.getUserPhotos(userId, pageKey,count = totalImage)
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe(
                { response ->
                    handleResponse(
                        application,
                        response,
                        onSuccess = { result ->
                            val incoming = result.data
                            execute {
                                try {
                                    postFeedDao.savePosts(
                                        incoming,
                                        FeedSource.Type.USER_PHOTOS
                                    )
                                }catch (e:Exception){
                                    e.printStackTrace()
                                }
                            }
                            isLoading = false
                            callback?.completedGetUserPhotos(result.next_page)

                        },
                        onFailure = {
                            Log.e(tag, "error ${response.code()}")
                            callback?.completedGetUserPhotos(null)
                            callback?.completedError(it)
                        }
                    )
                },
                { err ->
                    Log.e(tag, "error = ${err.message}")
                    callback?.completedError(err.message ?: "")
                    callback?.completedGetUserPhotos(null)
                }
            )
    }

    override fun getUserPhotos(userId: Int): LiveData<List<Post>> {
        return postFeedDao.getUserVideoPostsLiveData(userId, FeedSource.Type.USER_PHOTOS)
    }
}