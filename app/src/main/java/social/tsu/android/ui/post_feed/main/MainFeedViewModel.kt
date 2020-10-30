package social.tsu.android.ui.post_feed.main


import social.tsu.android.data.local.entity.FeedSource
import social.tsu.android.data.repository.PostFeedRepository
import social.tsu.android.ui.post_feed.BaseFeedViewModel
import javax.inject.Inject

class MainFeedViewModel @Inject constructor(
    private val postFeedRepo: PostFeedRepository
) : BaseFeedViewModel(postFeedRepo) {

    val initialLoadState by lazy {
        postFeedRepo.initialLoadState
    }

    val userRefreshLoadState by lazy {
        postFeedRepo.userRefreshLoadState
    }

    fun getMainFeedChrono() = postFeedRepo.getMainFeedChrono()

    fun getMainFeedTrending(isInitial: Boolean = false) = postFeedRepo.getMainFeedTrending(isInitial)

    fun refreshPosts(userInitiatedRefresh: Boolean = false, isTrendingFeed: Boolean = false) {
        when (isTrendingFeed) {
            true -> postFeedRepo.refreshTrendingPosts(
                FeedSource.Type.ORDER,
                userInitiatedRefresh = userInitiatedRefresh
            )
            false -> postFeedRepo.refreshPosts(
                FeedSource.Type.MAIN,
                userInitiatedRefresh = userInitiatedRefresh
            )
        }
    }

    fun retry() = postFeedRepo.retry()

    fun createPost(string: String, isTrendingFeed: Boolean)
            = postFeedRepo.createPost(string, isTrendingFeed)

}