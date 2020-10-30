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
import social.tsu.android.network.api.DiscoveryApi
import social.tsu.android.network.api.PostApi
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

abstract class DiscoveryGridService: DefaultService(){
    abstract var callback: DiscoveryGridServiceCallback?

    abstract fun getDiscoveryPosts(pageKey: Int?)
    abstract fun getDiscoveryCache(): LiveData<List<Post>>
}

interface DiscoveryGridServiceCallback {
    fun completedGetDiscoveryPosts(nextPage: Int?)
    fun completedError(message: String)
}

class DefaultDiscoveryGridService(private val application: TsuApplication, callback: DiscoveryGridServiceCallback): DiscoveryGridService() {

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var api: DiscoveryApi

    @Inject
    lateinit var postFeedDao: PostFeedDao

    override var callback: DiscoveryGridServiceCallback? = callback

    override val compositeDisposable = CompositeDisposable()

    override val tag: String
        get() = "DefaultHashtagGridService"

    private var isLoading = false

    init {
        application.appComponent.inject(this)
    }

    override fun getDiscoveryPosts(pageKey: Int?) {
        if (isLoading) return
        isLoading = true
        compositeDisposable += api.getDiscoveryFeed(pageKey)
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe({ response ->
                    handleResponse(application, response,
                        onSuccess = { result ->
                            val incoming = result.data
                            execute {
                                try {
                                    postFeedDao.savePosts(incoming, FeedSource.Type.DISCOVERY_FEED)
                                } catch (e:Exception){
                                    e.printStackTrace()
                                }
                            }
                            isLoading = false
                            callback?.completedGetDiscoveryPosts(result.nextPage?.popular)

                        },
                        onFailure = {
                            Log.e(tag, "error ${response.code()}")
                            callback?.completedGetDiscoveryPosts(null)
                            callback?.completedError(it)
                        }
                    )
                },
                { err ->
                    Log.e(tag, "error = ${err.message}")
                    callback?.completedError(err.message ?: "")
                    callback?.completedGetDiscoveryPosts(null)
                }
            )
    }

    override fun getDiscoveryCache(): LiveData<List<Post>> {
        return postFeedDao.getDiscoveryCache(FeedSource.Type.DISCOVERY_FEED)
    }
}