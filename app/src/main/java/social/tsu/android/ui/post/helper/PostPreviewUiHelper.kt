package social.tsu.android.ui.post.helper

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import social.tsu.android.R
import social.tsu.android.ui.post.model.ColorModel
import social.tsu.android.ui.post.model.FontModel

object PostPreviewUiHelper {

    /**
     *  All font list for preview screen  text overlay
     */
    fun getFontList(context: Context?): ArrayList<FontModel> {

        val fontList = arrayListOf<FontModel>()
        fontList.add(
            FontModel(
                R.drawable.ic_watermark_off, FontModel.ItemType.WATERMARK
            )
        )
        fontList.add(
            FontModel(
                R.drawable.ic_text_align_left, FontModel.ItemType.ALIGN
            )
        )
        fontList.add(
            FontModel(
                R.drawable.ic_font_pic_classic_gray, FontModel.ItemType.FONT,
                Typeface.createFromAsset(context?.assets, "classic.ttf")
            )
        )
        fontList.add(
            FontModel(
                R.drawable.ic_font_pic_open_sans_gray, FontModel.ItemType.FONT,
                Typeface.createFromAsset(context?.assets, "opensans.ttf")
            )
        )
        fontList.add(
            FontModel(
                R.drawable.ic_font_pic_pacifico_gray, FontModel.ItemType.FONT,
                Typeface.createFromAsset(context?.assets, "pacifico.ttf")
            )
        )
        fontList.add(
            FontModel(
                R.drawable.ic_font_pic_roboto_lab_gray, FontModel.ItemType.FONT,
                Typeface.createFromAsset(context?.assets, "robotoslab.ttf")
            )
        )
        return fontList
    }

    /**
     *  All color list for preview screen text overlay
     */
    fun getColorList(): ArrayList<ColorModel> {

        val colorList = arrayListOf<ColorModel>()

        colorList.add(ColorModel(Color.parseColor("#FFFFFF")))
        colorList.add(ColorModel(Color.parseColor("#000000")))
        colorList.add(ColorModel(Color.parseColor("#FFB734")))
        colorList.add(ColorModel(Color.parseColor("#FF6B6B")))
        colorList.add(ColorModel(Color.parseColor("#D21010")))
        colorList.add(ColorModel(Color.parseColor("#4ECDC4")))
        colorList.add(ColorModel(Color.parseColor("#8AD22C")))
        colorList.add(ColorModel(Color.parseColor("#656CF4")))
        colorList.add(ColorModel(Color.parseColor("#8F4DD8")))
        return colorList
    }
}