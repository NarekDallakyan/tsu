package social.tsu.android.ui.post.model

import android.graphics.Typeface
import androidx.annotation.DrawableRes

class FontModel(
    @DrawableRes var iconResource: Int,
    var needBorder: Boolean = true,
    var font: Typeface
)