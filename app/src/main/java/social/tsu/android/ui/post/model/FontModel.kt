package social.tsu.android.ui.post.model

import android.graphics.Typeface
import androidx.annotation.DrawableRes

class FontModel(
    @DrawableRes var iconResource: Int,
    var itemType: ItemType,
    var font: Typeface? = null,
    var watermark: Boolean = false,
    var align: Int = 0,
    var isSelected: Boolean = false
) {
    enum class ItemType {
        FONT, WATERMARK, ALIGN
    }
}