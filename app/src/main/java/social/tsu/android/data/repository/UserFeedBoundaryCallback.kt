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

class UserFeedBoundaryCallback(
    private val userId: Int,
    private val postFeedService: PostFeedService,
    private val postFeedDao: PostFeedDao,
    private val loadState: MutableLiveData<Data<Boolean>>,
    private val setRetry: (() -> Unit) -> Unit
) : PagedList.BoundaryCallback<Post>() {

    private var startAt: Int? = null
    private var startAtId: Long = 0
    private var lastStartAtId: Long = 0

    private var isRequestInProgress = false

    override fun onZeroItemsLoaded() {
        execute {
            lastStartAtId = 0
            val startAtPost = postFeedDao.getOldestPost(FeedSource.Type.USER)
            startAt = startAtPost?.id?.toInt()
            startAtId = startAtPost?.id ?: 0
            requestAndSaveData()
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: Post) {
        execute {
            val startAtPost = postFeedDao.getOldestPost(FeedSource.Type.USER)
            startAt = startAtPost?.id?.toInt()
            startAtId = startAtPost?.id ?: 0
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
        postFeedService.getUserPostFeed(userId, startAt, object :
            ServiceCallback<List<Post>> {
            override fun onSuccess(result: List<Post>) {
                loadState.postValue(Data.Success(true))
                val finalResults = result.filter { !it.is_share }
                isRequestInProgress = false
                lastStartAtId = startAtId
                execute {
                    postFeedDao.savePosts(finalResults, FeedSource.Type.USER)
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