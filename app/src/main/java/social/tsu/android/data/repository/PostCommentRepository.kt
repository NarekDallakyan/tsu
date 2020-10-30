package social.tsu.android.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import social.tsu.android.data.local.dao.PostFeedDao
import social.tsu.android.execute
import social.tsu.android.network.model.Comment
import social.tsu.android.service.CommentActionsService
import social.tsu.android.service.ServiceCallback
import social.tsu.android.ui.model.Data
import javax.inject.Inject

class PostCommentRepository @Inject constructor(
    private val commentActionsService: CommentActionsService,
    private val postFeedDao: PostFeedDao
) {


    fun likeComment(comment: Comment): LiveData<Data<Boolean>> {
        val commentLoadState = MutableLiveData<Data<Boolean>>()

        commentLoadState.value = Data.Loading()
        commentActionsService.likeComment(comment.id, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                commentLoadState.value = Data.Success(result)
            }

            override fun onFailure(errMsg: String) {
                commentLoadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return commentLoadState
    }


    fun unlikeComment(comment: Comment): LiveData<Data<Boolean>> {
        val commentLoadState = MutableLiveData<Data<Boolean>>()

        commentLoadState.value = Data.Loading()
        commentActionsService.unlikeComment(comment.id, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                commentLoadState.value = Data.Success(result)
            }

            override fun onFailure(errMsg: String) {
                commentLoadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return commentLoadState
    }

    fun deleteComment(comment: Comment): LiveData<Data<Boolean>> {
        val commentLoadState = MutableLiveData<Data<Boolean>>()

        commentLoadState.value = Data.Loading()
        commentActionsService.deleteComment(comment.id, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                execute {
                    postFeedDao.decreaseCommentCount(comment.postId.toLong())
                    commentLoadState.postValue(Data.Success(result))
                }
            }

            override fun onFailure(errMsg: String) {
                commentLoadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return commentLoadState
    }
}