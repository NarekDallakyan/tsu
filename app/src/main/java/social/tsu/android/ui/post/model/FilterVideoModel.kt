package social.tsu.android.ui.post.model

class FilterVideoModel(
    var filterImage: Int,
    var filterObject: Any,
    var isSelected: Boolean = false
) {
    fun select(select: Boolean) {
        isSelected = select
    }
}