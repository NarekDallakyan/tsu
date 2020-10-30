package social.tsu.android.ui.post_feed.single_post


import social.tsu.android.data.repository.PostFeedRepository
import social.tsu.android.ui.post_feed.BaseFeedViewModel
import javax.inject.Inject

class SinglePostFeedViewModel @Inject constructor(private val postFeedRepo: PostFeedRepository) :
    BaseFeedViewModel(postFeedRepo) {

    fun retry() {
        postFeedRepo.getPostById(postId)
    }

    var postId: Long = 0
    val loadState by lazy {
        postFeedRepo.initialLoadState
    }
    val post by lazy {
        postFeedRepo.getPostById(postId)
    }

}