package social.tsu.android.data.repository

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import social.tsu.android.data.local.dao.PostFeedDao
import social.tsu.android.data.local.entity.FeedSource
import social.tsu.android.data.local.entity.Post
import social.tsu.android.execute
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.UserMediaResponse
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.post_feed.PostFeedService
import social.tsu.android.ui.model.Data

class UserPhotosFeedBoundaryCallback(
    private val userId: Int,
    private val postFeedService: PostFeedService,
    private val postFeedDao: PostFeedDao,
    private val initialSize: Int,
    private val count: Int,
    private val loadState: MutableLiveData<Data<Boolean>>,
    private val setRetry: (() -> Unit) -> Unit
) : PagedList.BoundaryCallback<Post>() {

    private var nextPageKey: Int? = 0

    private var isRequestInProgress = false

    override fun onZeroItemsLoaded() {
        execute {
            requestAndSaveData()
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: Post) {
        execute {
            requestAndSaveData()
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: Post) {}

    private fun requestAndSaveData() {
        if (isRequestInProgress || AuthenticationHelper.currentUserId == null || nextPageKey == null) return

        loadState.postValue(Data.Loading())
        isRequestInProgress = true
        val page = if (nextPageKey == 0) null else nextPageKey
        val count = if (page == null) initialSize else this.count
        postFeedService.getUserPhotoPostFeed(userId, page, count, object :
            ServiceCallback<UserMediaResponse> {
            override fun onSuccess(result: UserMediaResponse) {
                loadState.postValue(Data.Success(true))
                val finalResults = result.data.filter { !it.is_share }
                isRequestInProgress = false
                nextPageKey = result.next_page
                execute {
                    postFeedDao.savePosts(finalResults, FeedSource.Type.USER_PHOTOS)
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