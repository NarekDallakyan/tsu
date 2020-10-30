package social.tsu.android.data.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import social.tsu.android.data.local.dao.PostFeedDao
import social.tsu.android.data.local.entity.FeedSource
import social.tsu.android.data.local.entity.Post
import social.tsu.android.execute
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.DiscoveryFeedResponse
import social.tsu.android.network.model.UserMediaResponse
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.post_feed.PostFeedService
import social.tsu.android.ui.model.Data

class DiscoveryFeedBoundaryCallback(
    private val userId: Int,
    private val postFeedService: PostFeedService,
    private val postFeedDao: PostFeedDao,
    private val initialSize: Int,
    private val count: Int,
    private val loadState: MutableLiveData<Data<Boolean>>,
    private val setRetry: (() -> Unit) -> Unit
) : PagedList.BoundaryCallback<Post>() {

    val TAG: String = "DiscoFeedBoundaryCb"
    private var nextPageKey: Int? = 0

    private var isRequestInProgress = false

    override fun onZeroItemsLoaded() {
        execute {
            requestAndSaveData()
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: Post) {
       // Log.d(TAG, "onItemAtEndLoaded: $itemAtEnd")
        execute {
            requestAndSaveData()
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: Post) {
        //Log.d(TAG, "onItemAtFrontLoaded: $itemAtFront")
    }

    private fun requestAndSaveData() {
        Log.d(TAG, "requestAndSaveData: requestInProgress=$isRequestInProgress, nextPage=$nextPageKey")
        if (isRequestInProgress || AuthenticationHelper.currentUserId == null || nextPageKey == null) return

        loadState.postValue(Data.Loading())
        isRequestInProgress = true
        val page = if (nextPageKey == 0) null else nextPageKey
        val count = if (page == null) initialSize else this.count
        postFeedService.getDiscoveryFeed(page, count, object : ServiceCallback<DiscoveryFeedResponse> {
            override fun onSuccess(result: DiscoveryFeedResponse) {
                loadState.postValue(Data.Success(true))
                isRequestInProgress = false
                nextPageKey = result.nextPage?.popular
                Log.d(TAG, "getDiscoveryFeed::onSuccess: nextPage=$nextPageKey, nextPageObj:${result.nextPage}, itemsSize: ${result.data.size}")
                execute {
                    postFeedDao.savePosts(result.data, FeedSource.Type.DISCOVERY_FEED)
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