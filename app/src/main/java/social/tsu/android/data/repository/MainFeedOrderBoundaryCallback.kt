package social.tsu.android.data.repository

import android.util.Log
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

class MainFeedOrderBoundaryCallback(
    private val postFeedService: PostFeedService,
    private val postFeedDao: PostFeedDao,
    private val loadState: MutableLiveData<Data<Boolean>>,
    private val setRetry: (() -> Unit) -> Unit,
    private var isInitial: Boolean
) : PagedList.BoundaryCallback<Post>() {

    private val TAG = MainFeedOrderBoundaryCallback::class.java.simpleName

    private var startAt: Int? = null
    private var lastStartAt: Int? = -1
    private var isNextPageSame = false

    private var isRequestInProgress = false

    override fun onZeroItemsLoaded() {
        Log.d(TAG, "onZeroItemsLoaded()")
        execute {
            lastStartAt = -1
            isNextPageSame = false
            if (postFeedDao.getOldestPost(FeedSource.Type.HASHTAG) == null)
                startAt = null

            requestAndSaveData()
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: Post) {
        Log.d(TAG, "onItemAtEndLoaded()")
        execute {
            requestAndSaveData()
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: Post) {}

    private fun requestAndSaveData() {

        if (isRequestInProgress || AuthenticationHelper.currentUserId == null || lastStartAt == 0 || lastStartAt == null || isNextPageSame) {
            Log.d(TAG, "RETURN REASON isRequestInProgress:$isRequestInProgress, lastStartAt 0/null:$lastStartAt, isNextPageSame$isNextPageSame")
            return
        }

        loadState.postValue(Data.Loading())
        isRequestInProgress = true
        postFeedService.getPostFeedTrending(startAt, PostFeedRepository.PAGE_SIZE, object :
            ServiceCallback<PostFeedService.PostFeedResults> {
            override fun onSuccess(result: PostFeedService.PostFeedResults) {
                loadState.postValue(Data.Success(true))

                if (lastStartAt == result.nextPage)
                    isNextPageSame = true

                isRequestInProgress = false
                startAt = result.nextPage
                lastStartAt = result.nextPage
                isInitial = false

                execute {
                    postFeedDao.savePosts(result.posts, FeedSource.Type.ORDER, true)
                }
            }

            override fun onFailure(errMsg: String) {
                isRequestInProgress = false
                loadState.postValue(Data.Error(Throwable(errMsg)))
                setRetry(::requestAndSaveData)
            }
        }, isInitial)

    }

}