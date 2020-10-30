package social.tsu.android.ui.hashtag

import android.util.Log
import androidx.lifecycle.LiveData
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.service.*

interface HashtagGridViewModelCallback {
    fun didUpdateHashtags(nextPage: Int?)
    fun didErrorWith(message: String)
}

abstract class HashtagGridViewModel {
    var nextPage: Int? = 0
    abstract fun getPostsByHashtag(hashtag: String, pageKey: Int?)
    abstract fun getCacheByHashtag(hashtag: String): LiveData<List<Post>>
}

class DefaultHashtagGridViewModel(private val application: TsuApplication, private val callback: HashtagGridViewModelCallback): HashtagGridViewModel(), HashtagGridServiceCallback {

    private val hashtagGridService: HashtagGridService by lazy {
        DefaultHashtagGridService(application, this)
    }

    override fun getPostsByHashtag(hashtag: String, pageKey: Int?){
        hashtagGridService.getPostsByHashtag(hashtag, pageKey)
    }

    override fun getCacheByHashtag(hashtag: String): LiveData<List<Post>> {
        return hashtagGridService.getCacheByHashtag(hashtag)
    }

    override fun completedGetPostsByHashtag(nextPage: Int?) {
        Log.d("HashtagGridViewModel" , "NextPage: $nextPage")
        this.nextPage = nextPage
        callback.didUpdateHashtags(nextPage)
    }

    override fun completedError(message: String) {
        callback.didErrorWith(message)
    }
}
