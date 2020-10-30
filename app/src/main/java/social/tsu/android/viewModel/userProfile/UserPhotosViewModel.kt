package social.tsu.android.viewModel.userProfile

import android.util.Log
import androidx.lifecycle.LiveData
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.service.DefaultUserPhotosService
import social.tsu.android.service.UserPhotosService
import social.tsu.android.service.UserPhotosServiceCallback

interface UserPhotosViewModelCallback {
    fun didUpdatePhotos(nextPage: Int?)
    fun didErrorWith(message: String)
}

abstract class UserPhotosViewModel{
    var nextPage: Int? = 0
    abstract fun getUserPhotos(userId: Int, pageKey: Int?)
    abstract fun photosForUser(userId: Int): LiveData<List<Post>>
}

class DefaultUserPhotosViewModel(private val application: TsuApplication, private val callback: UserPhotosViewModelCallback): UserPhotosViewModel(),  UserPhotosServiceCallback {

    private val photosService: UserPhotosService by lazy {
        DefaultUserPhotosService(application, this)
    }

    override fun getUserPhotos(userId: Int, pageKey: Int?){
        photosService.getUserPhotos(userId, pageKey)
    }

    override fun photosForUser(userId: Int): LiveData<List<Post>> {
        return photosService.getUserPhotos(userId)
    }

    override fun completedGetUserPhotos(nextPage: Int?) {
        Log.d("UserPhotosViewModel" , "NextPage: $nextPage")
        this.nextPage = nextPage
        callback.didUpdatePhotos(nextPage)
    }

    override fun completedError(message: String) {
        callback.didErrorWith(message)
    }
}
