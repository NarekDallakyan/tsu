package social.tsu.android.ui.post_feed.edit_post

import androidx.lifecycle.ViewModel
import social.tsu.android.data.repository.PostFeedRepository
import javax.inject.Inject

class EditPostViewModel @Inject constructor(private val postFeedRepo: PostFeedRepository) :
    ViewModel() {

    var postId: Long = 0

    val post by lazy { postFeedRepo.getPost(postId) }

    fun savePostEdits(value: String) = postFeedRepo.savePostEdit(postId, value)

}