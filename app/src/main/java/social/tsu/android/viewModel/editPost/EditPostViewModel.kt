package social.tsu.android.viewModel.editPost

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.PostsCache
import social.tsu.android.network.api.PostApi
import social.tsu.android.network.model.EditPostDTO
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.getNetworkCallErrorMessage
import social.tsu.android.service.handleResponse
import javax.inject.Inject

interface EditPostViewModelCallback {
    fun completedPostEdit()
    fun failedToEditPost(message: String)
}

abstract class EditPostViewModel {
    abstract var post: Post?
    abstract fun savePostEdits(value: String)
}

class DefaultEditPostViewModel(private val application: TsuApplication, private val postId: Long, private val callback: EditPostViewModelCallback?): EditPostViewModel() {

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var postApi: PostApi

    private val compositeDisposable = CompositeDisposable()

    private val cache: PostsCache = PostsCache

    override var post: Post? = null
        get() = cache[postId]

    init {
        application.appComponent.inject(this)
    }

    override fun savePostEdits(value: String) {
        val dto = EditPostDTO(value, postId)
        compositeDisposable += postApi.editPostContent(postId, dto)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = {
                        updatePost()
                    },
                    onFailure = {
                        callback?.failedToEditPost(it)
                    }
                )
            }, { err ->
                Log.e("EditPostViewModel", "error = ${err.message}")
                callback?.failedToEditPost(err.getNetworkCallErrorMessage(application))
            })
    }

    private fun updatePost() {
        compositeDisposable += postApi.getPost(postId.toInt())
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe(
                { response ->
                    handleResponse(
                        application,
                        response,
                        onSuccess = {
                            val incoming = it.data
                            updateWithNewPost(incoming)
                        },
                        onFailure = {
                            callback?.failedToEditPost(it)
                        }
                    )
                },
                { err ->
                    callback?.failedToEditPost(err.getNetworkCallErrorMessage(application))
                }
            )
    }

    private  fun updateWithNewPost(post: Post?) {
        val post = post?: return
        cache[post.id] = post
        callback?.completedPostEdit()
    }

}