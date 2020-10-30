package social.tsu.android.ui.search

import android.util.Log
import androidx.lifecycle.LiveData
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.service.*

interface DiscoveryGridViewModelCallback {
    fun didUpdateDiscoveryPosts(nextPage: Int?)
    fun didErrorWith(message: String)
}

abstract class DiscoveryGridViewModel {
    var nextPage: Int? = 0
    abstract fun getDiscoveryPosts(pageKey: Int?)
    abstract fun getDiscoveryCache(): LiveData<List<Post>>
}

class DefaultDiscoveryGridViewModel(private val application: TsuApplication, private val callback: DiscoveryGridViewModelCallback): DiscoveryGridViewModel(), DiscoveryGridServiceCallback {

    private val discoveryGridService: DiscoveryGridService by lazy {
        DefaultDiscoveryGridService(application, this)
    }

    override fun getDiscoveryPosts(pageKey: Int?){
        discoveryGridService.getDiscoveryPosts(pageKey)
    }

    override fun getDiscoveryCache(): LiveData<List<Post>> {
        return discoveryGridService.getDiscoveryCache()
    }

    override fun completedGetDiscoveryPosts(nextPage: Int?) {
        Log.d("DiscoveryGridViewModel" , "NextPage: $nextPage")
        this.nextPage = nextPage
        callback.didUpdateDiscoveryPosts(nextPage)
    }

    override fun completedError(message: String) {
        callback.didErrorWith(message)
    }
}
