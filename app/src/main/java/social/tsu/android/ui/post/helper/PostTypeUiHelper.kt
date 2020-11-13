package social.tsu.android.ui.post.helper

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import kotlinx.android.synthetic.main.fragment_post_types.view.*
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.DeviceUtils
import social.tsu.android.ui.post.view.PostTypesFragment


object PostTypeUiHelper {

    fun setChoose(
        layout: Int,
        rootView: View?
    ) {
        if (rootView == null) return

        val languageLayout = rootView.languageLayout_id
        val photoLayout = rootView.photoLayout_id
        val gifLayout = rootView.gifLayout_id
        val wifiLayout = rootView.wifiLayout_id

        when (layout) {

            PostTypesFragment.GIF_CLICK -> {
                val unwrappedDrawable =
                    AppCompatResources.getDrawable(TsuApplication.mContext, R.drawable.ic_gif_icon)
                unwrappedDrawable?.let {
                    val wrappedDrawable: Drawable = DrawableCompat.wrap(it)
                    DrawableCompat.setTint(wrappedDrawable, Color.WHITE)
                    gifLayout?.background = wrappedDrawable
                }
                setUnChoose(
                    PostTypesFragment.PHOTO_CLICK,
                    languageLayout,
                    photoLayout,
                    wifiLayout,
                    gifLayout
                )
                setUnChoose(
                    PostTypesFragment.WIFI_CLICK,
                    languageLayout,
                    photoLayout,
                    wifiLayout,
                    gifLayout
                )

                setUnChoose(
                    PostTypesFragment.LANGUAGE_CLICK,
                    languageLayout,
                    photoLayout,
                    wifiLayout,
                    gifLayout
                )
            }
            PostTypesFragment.LANGUAGE_CLICK -> {
                languageLayout?.setBackgroundResource(R.drawable.ic_languages_white_end)
                setUnChoose(
                    PostTypesFragment.PHOTO_CLICK,
                    languageLayout,
                    photoLayout,
                    wifiLayout,
                    gifLayout
                )
                setUnChoose(
                    PostTypesFragment.WIFI_CLICK,
                    languageLayout,
                    photoLayout,
                    wifiLayout,
                    gifLayout
                )

                setUnChoose(
                    PostTypesFragment.GIF_CLICK,
                    languageLayout,
                    photoLayout,
                    wifiLayout,
                    gifLayout
                )
            }

            PostTypesFragment.PHOTO_CLICK -> {
                photoLayout?.setBackgroundResource(R.drawable.ic_photo_white)

                setUnChoose(
                    PostTypesFragment.LANGUAGE_CLICK,
                    languageLayout,
                    photoLayout,
                    wifiLayout,
                    gifLayout
                )
                setUnChoose(
                    PostTypesFragment.WIFI_CLICK,
                    languageLayout,
                    photoLayout,
                    wifiLayout,
                    gifLayout
                )
                setUnChoose(
                    PostTypesFragment.GIF_CLICK,
                    languageLayout,
                    photoLayout,
                    wifiLayout,
                    gifLayout
                )
            }

            PostTypesFragment.WIFI_CLICK -> {
                wifiLayout?.setBackgroundResource(R.drawable.ic_wifi_white_finish)
                setUnChoose(
                    PostTypesFragment.LANGUAGE_CLICK,
                    languageLayout,
                    photoLayout,
                    wifiLayout,
                    gifLayout
                )
                setUnChoose(
                    PostTypesFragment.PHOTO_CLICK,
                    languageLayout,
                    photoLayout,
                    wifiLayout,
                    gifLayout
                )
                setUnChoose(
                    PostTypesFragment.GIF_CLICK,
                    languageLayout,
                    photoLayout,
                    wifiLayout,
                    gifLayout
                )
            }
            }
        }


        private fun setUnChoose(
            layout: Int,
            languageLayout: ConstraintLayout?,
            photoLayout: ConstraintLayout?,
            wifiLayout: ConstraintLayout?,
            gifLayout: ConstraintLayout?
        ) {
            when (layout) {
                PostTypesFragment.LANGUAGE_CLICK -> {
                    languageLayout?.setBackgroundResource(R.drawable.ic_languages_gray)
                }

                PostTypesFragment.PHOTO_CLICK -> {
                    photoLayout?.setBackgroundResource(R.drawable.ic_photogray)
                }

                PostTypesFragment.WIFI_CLICK -> {
                    wifiLayout?.setBackgroundResource(R.drawable.ic_wifi_gray_ending)
                }

                PostTypesFragment.GIF_CLICK -> {
                    gifLayout?.setBackgroundResource(R.drawable.ic_gif_icon)
                }
            }
        }

        fun handleViewPagerChange(
            context: Context,
            position: Int,
            rootView: View?
        ) {
            if (rootView == null) return

            // Find post type fragment sub views
            val newPostPhotoText = rootView.newPostPhotoText
            val newPostVideoText = rootView.newPostVideoText
            val newPostGifText = rootView.newPostGifText
            val captureBtn = rootView.snap_icon_id


            when (position) {

                // Photo section
                0 -> {

                    captureBtn.setImageResource(R.drawable.photo_capture_image)

                    // Change text styles
                    newPostPhotoText.setTypeface(newPostPhotoText.typeface, Typeface.BOLD)
                    newPostVideoText.setTypeface(newPostVideoText.typeface, Typeface.NORMAL)
                    newPostGifText.setTypeface(newPostGifText.typeface, Typeface.NORMAL)
                    // Changes text sizes
                    newPostPhotoText.textSize = DeviceUtils.pixelsToSp(context, 50f)
                    newPostVideoText.textSize = DeviceUtils.pixelsToSp(context, 40f)
                    newPostGifText.textSize = DeviceUtils.pixelsToSp(context, 40f)
                    // Change text colors
                    newPostPhotoText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.next_green
                        )
                    )
                    newPostVideoText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.ib_fr_white
                        )
                    )
                    newPostGifText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.ib_fr_white
                        )
                    )
                }

                // Video section
                1 -> {


                    captureBtn.setImageResource(R.drawable.record_video_not_start)

                    // Change text styles
                    newPostPhotoText.setTypeface(newPostPhotoText.typeface, Typeface.NORMAL)
                    newPostVideoText.setTypeface(newPostVideoText.typeface, Typeface.BOLD)
                    newPostGifText.setTypeface(newPostGifText.typeface, Typeface.NORMAL)
                    // Changes text sizes
                    newPostPhotoText.textSize = DeviceUtils.pixelsToSp(context, 40f)
                    newPostVideoText.textSize = DeviceUtils.pixelsToSp(context, 50f)
                    newPostGifText.textSize = DeviceUtils.pixelsToSp(context, 40f)
                    // Change text colors
                    newPostPhotoText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.ib_fr_white
                        )
                    )
                    newPostVideoText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.next_green
                        )
                    )
                    newPostGifText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.ib_fr_white
                        )
                    )
                }

                // GIF section
                2 -> {

                    captureBtn.setImageResource(R.drawable.record_video_not_start)

                    // Change text styles
                    newPostPhotoText.setTypeface(newPostPhotoText.typeface, Typeface.NORMAL)
                    newPostVideoText.setTypeface(newPostVideoText.typeface, Typeface.NORMAL)
                    newPostGifText.setTypeface(newPostGifText.typeface, Typeface.BOLD)
                    // Changes text sizes
                    newPostPhotoText.textSize = DeviceUtils.pixelsToSp(context, 40f)
                    newPostVideoText.textSize = DeviceUtils.pixelsToSp(context, 40f)
                    newPostGifText.textSize = DeviceUtils.pixelsToSp(context, 50f)
                    // Change text colors
                    newPostPhotoText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.ib_fr_white
                        )
                    )
                    newPostVideoText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.ib_fr_white
                        )
                    )
                    newPostGifText.setTextColor(ContextCompat.getColor(context, R.color.next_green))
                }
            }
        }

        fun changeLayoutAlpha(layout: ConstraintLayout) {
            when (layout.alpha) {
                1F -> {
                    layout.alpha = 0.7F
                }
                0.7F -> {
                    layout.alpha = 1F
                }
            }
        }
}