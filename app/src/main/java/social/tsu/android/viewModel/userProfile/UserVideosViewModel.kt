package social.tsu.android.viewModel.userProfile

import android.util.Log
import androidx.lifecycle.LiveData
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.service.DefaultUserVideosService
import social.tsu.android.service.UserVideosService
import social.tsu.android.service.UserVideosServiceCallback

interface UserVideosViewModelCallback {
    fun didUpdateVideos(nextPage: Int?)
    fun didErrorWith(message: String)
}

abstract class UserVideosViewModel {
    var nextPage: Int? = 0
    abstract fun loadUserVideos(userId: Int, pageKey: Int?)
    abstract fun videosForUser(userId: Int): LiveData<List<Post>>
}

class DefaultUserVideosViewModel(
    private val application: TsuApplication,
    private val callback: UserVideosViewModelCallback
) : UserVideosViewModel(), UserVideosServiceCallback {

    // TODO: Replace with PostFeedRepository
    private val videosService: UserVideosService by lazy {
        DefaultUserVideosService(application, this)
    }

    override fun loadUserVideos(userId: Int, pageKey: Int?) {
        videosService.loadUserVideos(userId, pageKey)
    }

    override fun videosForUser(userId: Int): LiveData<List<Post>> {
        return videosService.getUserVideos(userId)
    }

    override fun completedGetUserVideos(nextPage: Int?) {
        Log.d("UserVideosViewModel", "NextPage: $nextPage")
        this.nextPage = nextPage
        callback.didUpdateVideos(nextPage)
    }

    override fun completedError(message: String) {
        callback.didErrorWith(message)
    }
}
