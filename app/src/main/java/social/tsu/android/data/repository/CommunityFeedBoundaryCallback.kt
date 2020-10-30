package social.tsu.android.data.repository

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import social.tsu.android.data.local.dao.PostFeedDao
import social.tsu.android.data.local.entity.FeedSource
import social.tsu.android.data.local.entity.Post
import social.tsu.android.execute
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.post_feed.PostFeedService
import social.tsu.android.ui.model.Data

class CommunityFeedBoundaryCallback(
    private val communityId: Int,
    private val postFeedService: PostFeedService,
    private val postFeedDao: PostFeedDao,
    private val loadState: MutableLiveData<Data<Boolean>>,
    private val setRetry: (() -> Unit) -> Unit
) : PagedList.BoundaryCallback<Post>() {

    private var startAt: Int? = null

    private var isRequestInProgress = false

    private var lastStartAtId: Long = 0
    private var startAtId: Long = 0

    override fun onZeroItemsLoaded() {
        execute {
            lastStartAtId = 0
            val startAtPost = postFeedDao.getOldestCommunityPost(communityId, FeedSource.Type.COMMUNITY)
            startAtId = startAtPost?.id ?: 0
            requestAndSaveData()
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: Post) {
        execute {
            val startAtPost = postFeedDao.getOldestCommunityPost(communityId, FeedSource.Type.COMMUNITY)
            startAtId = startAtPost?.id ?: 0
            startAt = startAt ?: startAtPost?.timestamp
            requestAndSaveData()
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: Post) {}

    private fun requestAndSaveData() {

        if (isRequestInProgress ||
            AuthenticationHelper.currentUserId == null ||
            lastStartAtId == startAtId
        ) return

        loadState.postValue(Data.Loading())
        isRequestInProgress = true
        postFeedService.getCommunityFeed(communityId, startAt, object :
            ServiceCallback<PostFeedService.PostFeedResults> {
            override fun onSuccess(result: PostFeedService.PostFeedResults) {
                loadState.postValue(Data.Success(true))
                lastStartAtId = startAtId
                isRequestInProgress = false
                startAt = result.nextPage
                execute {
                    postFeedDao.savePosts(result.posts, FeedSource.Type.COMMUNITY)
                }
            }

            override fun onFailure(errMsg: String) {
                isRequestInProgress = false
                loadState.postValue(Data.Error(Throwable(errMsg)))
                setRetry(::requestAndSaveData)
            }
        })

    }

}