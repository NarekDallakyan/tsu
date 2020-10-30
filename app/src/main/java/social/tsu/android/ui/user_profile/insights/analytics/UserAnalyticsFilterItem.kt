package social.tsu.android.ui.user_profile.insights.analytics

data class UserAnalyticsFilterItem(
    val filterType: FilterType,
    val icon: Int,
    val label: String,
    var isSelected: Boolean,
    val counterValue: Int
)