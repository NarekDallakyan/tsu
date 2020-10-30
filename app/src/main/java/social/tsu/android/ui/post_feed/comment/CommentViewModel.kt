package social.tsu.android.ui.post_feed.comment

import androidx.lifecycle.ViewModel
import social.tsu.android.data.repository.PostCommentRepository
import social.tsu.android.data.repository.PostFeedRepository
import social.tsu.android.network.model.Comment
import javax.inject.Inject

class CommentViewModel @Inject constructor(
    private val postCommentRepo: PostCommentRepository,
    private val postFeedRepo: PostFeedRepository
) : ViewModel() {

    var postId: Long = 0
    val loadState by lazy {
        postFeedRepo.initialLoadState
    }
    val post by lazy {
        postFeedRepo.getPost(postId)
    }

    fun likeComment(comment: Comment) = postCommentRepo.likeComment(comment)

    fun unlikeComment(comment: Comment) = postCommentRepo.unlikeComment(comment)

    fun deleteComment(comment: Comment) = postCommentRepo.deleteComment(comment)

}