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

abstract class HashtagGridService: DefaultService(){
    abstract var callback: HashtagGridServiceCallback?

    abstract fun getPostsByHashtag(hashtag: String, pageKey: Int?)
    abstract fun getCacheByHashtag(hashtag: String): LiveData<List<Post>>
}

interface HashtagGridServiceCallback {
    fun completedGetPostsByHashtag(nextPage: Int?)
    fun completedError(message: String)
}

class DefaultHashtagGridService(private val application: TsuApplication, callback: HashtagGridServiceCallback): HashtagGridService() {

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var api: PostApi

    @Inject
    lateinit var postFeedDao: PostFeedDao

    override var callback: HashtagGridServiceCallback? = callback

    override val compositeDisposable = CompositeDisposable()

    override val tag: String
        get() = "DefaultHashtagGridService"

    private var isLoading = false

    init {
        application.appComponent.inject(this)
    }

    override fun getPostsByHashtag(hashtag: String, pageKey: Int?) {
        if (isLoading) return
        isLoading = true
        compositeDisposable += api.getHashtagPosts(hashtag, pageKey)
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe({ response ->
                    handleResponse(application, response,
                        onSuccess = { result ->
                            val incoming = result.data
                            execute {
                                try {
                                    postFeedDao.savePosts(incoming, FeedSource.Type.HASHTAG)
                                } catch (e:Exception){
                                    e.printStackTrace()
                                }
                            }
                            isLoading = false
                            callback?.completedGetPostsByHashtag(result.next_page)

                        },
                        onFailure = {
                            Log.e(tag, "error ${response.code()}")
                            callback?.completedGetPostsByHashtag(null)
                            callback?.completedError(it)
                        }
                    )
                },
                { err ->
                    Log.e(tag, "error = ${err.message}")
                    callback?.completedError(err.message ?: "")
                    callback?.completedGetPostsByHashtag(null)
                }
            )
    }

    override fun getCacheByHashtag(hashtag: String): LiveData<List<Post>> {
        return postFeedDao.getCacheByHashtag(hashtag, FeedSource.Type.HASHTAG)
    }
}