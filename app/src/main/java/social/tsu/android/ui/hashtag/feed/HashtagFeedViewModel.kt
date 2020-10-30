package social.tsu.android.ui.hashtag.feed

import social.tsu.android.data.repository.PostFeedRepository
import social.tsu.android.ui.post_feed.BaseFeedViewModel
import javax.inject.Inject

class HashtagFeedViewModel @Inject constructor(
    private val postFeedRepo: PostFeedRepository
) : BaseFeedViewModel(postFeedRepo) {

    fun getHashtagPosts(hashtag: String, includePosition: Int = 0) =
        postFeedRepo.getHashtagPosts(hashtag, includePosition)

    fun refreshHashtagPosts(hashtag: String) = postFeedRepo.refreshHashtagPosts(hashtag)

    fun retry() {
        postFeedRepo.retry()
    }

}