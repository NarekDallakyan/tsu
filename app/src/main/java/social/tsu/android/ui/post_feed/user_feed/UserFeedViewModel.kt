package social.tsu.android.ui.post_feed.user_feed


import social.tsu.android.data.repository.PostFeedRepository
import social.tsu.android.ui.post_feed.BaseFeedViewModel
import javax.inject.Inject

class UserFeedViewModel @Inject constructor(
    private val postFeedRepo: PostFeedRepository
) : BaseFeedViewModel(postFeedRepo) {


    fun getUserPosts(userId: Int) = postFeedRepo.getUserPosts(userId)

    fun getUserPhotoPosts(userId: Int, includePosition: Int = 0) =
        postFeedRepo.getUserPhotoPosts(userId, includePosition)

    fun getUserVideoPosts(userId: Int, includePosition: Int = 0) =
        postFeedRepo.getUserVideoPosts(userId, includePosition)

    fun getDiscoveryFeed(userId: Int, includePosition: Int = 0) =
        postFeedRepo.getDiscoveryFeed(userId, includePosition)

    fun refreshUserPosts(userId: Int) = postFeedRepo.refreshUserPosts(userId)

    fun refreshUserPhotos(userId: Int) = postFeedRepo.refreshUserPhotoPosts(userId)

    fun refreshUserVideos(userId: Int) = postFeedRepo.refreshUserVideoPosts(userId)

    fun refreshDiscoveryFeed(userId: Int) = postFeedRepo.refreshDiscoveryFeedPosts(userId)

    fun retry() {
        postFeedRepo.retry()
    }

}