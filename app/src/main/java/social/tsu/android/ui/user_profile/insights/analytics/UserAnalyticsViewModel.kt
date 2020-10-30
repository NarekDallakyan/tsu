package social.tsu.android.ui.user_profile.insights.analytics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import social.tsu.android.R
import social.tsu.android.network.model.AnalyticsResponse
import social.tsu.android.service.UserAnalyticsService
import social.tsu.android.service.UserAnalyticsServiceCallback
import social.tsu.android.utils.SingleLiveEvent
import javax.inject.Inject

class UserAnalyticsViewModel @Inject constructor(private val userAnalyticsService: UserAnalyticsService) : ViewModel(), UserAnalyticsServiceCallback {

    var currentPeriodLabel = MutableLiveData<String>()
    var currentTimelineLabel = MutableLiveData<String>()
    var isLineChartVisible = MutableLiveData<Boolean>()

    private val mutableFiltersLiveData = MutableLiveData<List<UserAnalyticsFilterItem>>()
    val filtersLiveData: LiveData<List<UserAnalyticsFilterItem>> = mutableFiltersLiveData

    private val mutableAnalyticsLiveData = MutableLiveData<AnalyticsResponse>()
    val analyticsLiveData: LiveData<AnalyticsResponse> = mutableAnalyticsLiveData

    val errorLiveData = SingleLiveEvent<String>()

    init {
        userAnalyticsService.callback = this
    }

    fun loadAnalytics(date: String) {
        userAnalyticsService.loadUserAnalytics(date)
    }

    override fun didLoadUserAnalytics(data: AnalyticsResponse) {
        val filtersList = listOf(
            UserAnalyticsFilterItem(FilterType.VIEWS, R.drawable.ic_eye_open, "Views", false, data.viewCount),
            UserAnalyticsFilterItem(FilterType.LIKES, R.drawable.ic_like, "Likes", false, data.likeCount),
            UserAnalyticsFilterItem(FilterType.COMMENTS, R.drawable.ic_comment, "Comments", false, data.commentCount),
            UserAnalyticsFilterItem(FilterType.SHARES, R.drawable.ic_share, "Shares", false, data.shareCount))

        mutableFiltersLiveData.postValue(filtersList)
        mutableAnalyticsLiveData.postValue(data)
    }

    override fun didErrorWith(message: String) {
        errorLiveData.postValue(message)
    }
}