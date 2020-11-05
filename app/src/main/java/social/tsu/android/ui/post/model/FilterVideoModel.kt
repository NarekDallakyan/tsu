package social.tsu.android.ui.post.model

import android.graphics.Bitmap

class FilterVideoModel(
    var filterImage: Int,
    var filterObject: Any,
    var isSelected: Boolean = false,
    val bitmaps: Bitmap?
) {
    fun select(select: Boolean) {
        isSelected = select
    }
}