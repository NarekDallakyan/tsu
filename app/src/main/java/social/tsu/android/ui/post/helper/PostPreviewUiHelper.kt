package social.tsu.android.ui.post.helper

import android.content.Context
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
                R.drawable.ic_watermark_off, "", FontModel.ItemType.WATERMARK
            )
        )
        fontList.add(
            FontModel(
                R.drawable.ic_text_align_left, "", FontModel.ItemType.ALIGN
            )
        )
        fontList.add(
            FontModel(
                null, "Classic", FontModel.ItemType.FONT,
                Typeface.createFromAsset(context?.assets, "classic.ttf")
            )
        )
        fontList.add(
            FontModel(
                null, "Monospace", FontModel.ItemType.FONT,
                Typeface.createFromAsset(context?.assets, "robotoslab.ttf")
            )
        )
        fontList.add(
            FontModel(
                null, "Neon", FontModel.ItemType.FONT,
                Typeface.createFromAsset(context?.assets, "opensans.ttf")
            )
        )
        fontList.add(
            FontModel(
                null, "Handwriting", FontModel.ItemType.FONT,
                Typeface.createFromAsset(context?.assets, "pacifico.ttf")
            )
        )
        return fontList
    }

    /**
     *  All color list for preview screen text overlay
     */
    fun getColorList(): ArrayList<ColorModel> {

        val colorList = arrayListOf<ColorModel>()

        colorList.add(ColorModel(ColorModel.ColorEnum.White))
        colorList.add(ColorModel(ColorModel.ColorEnum.Black))
        colorList.add(ColorModel(ColorModel.ColorEnum.Yellow))
        colorList.add(ColorModel(ColorModel.ColorEnum.Pink))
        colorList.add(ColorModel(ColorModel.ColorEnum.Red))
        colorList.add(ColorModel(ColorModel.ColorEnum.Mint))
        colorList.add(ColorModel(ColorModel.ColorEnum.Green))
        colorList.add(ColorModel(ColorModel.ColorEnum.Blue))
        colorList.add(ColorModel(ColorModel.ColorEnum.Purple))
        return colorList
    }

    private fun overlayColors() {

    }
}