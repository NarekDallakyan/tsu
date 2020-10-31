package social.tsu.android.ui.post.helper

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import social.tsu.android.R
import social.tsu.android.helper.DeviceUtils
import social.tsu.android.ui.post.view.PostTypesFragment
import social.tsu.android.ui.post.view.viewpager.GifPostFragment
import social.tsu.android.ui.post.view.viewpager.PhotoCameraPostFragment
import social.tsu.android.ui.post.view.viewpager.RecordVideoPostFragment

class LayoutChooseHelper {
    companion object {

        private val mHandle = Handler()
        private var fragments = ArrayList<Fragment>()

        private val photoRunnable: () -> Unit = {
            (fragments[0] as PhotoCameraPostFragment).resetCamera()
        }

        private val videoRunnable: () -> Unit = {
            (fragments[1] as RecordVideoPostFragment).resetCamera()
        }

        private val gifRunnable: () -> Unit = {
            (fragments[2] as GifPostFragment).resetCamera()
        }

        fun setChoose(
            layout: Int,
            languageLayout: ConstraintLayout?,
            photoLayout: ConstraintLayout?,
            wifiLayout: ConstraintLayout?
        ) {

            when (layout) {

                PostTypesFragment.LANGUAGE_CLICK -> {
                    languageLayout?.setBackgroundResource(R.drawable.ic_languages_white_end)
                    setUnChoose(
                        PostTypesFragment.PHOTO_CLICK,
                        languageLayout,
                        photoLayout,
                        wifiLayout
                    )
                    setUnChoose(
                        PostTypesFragment.WIFI_CLICK,
                        languageLayout,
                        photoLayout,
                        wifiLayout
                    )
                }

                PostTypesFragment.PHOTO_CLICK -> {
                    photoLayout?.setBackgroundResource(R.drawable.ic_photo_white)

                    setUnChoose(
                        PostTypesFragment.LANGUAGE_CLICK,
                        languageLayout,
                        photoLayout,
                        wifiLayout
                    )
                    setUnChoose(
                        PostTypesFragment.WIFI_CLICK,
                        languageLayout,
                        photoLayout,
                        wifiLayout
                    )
                }

                PostTypesFragment.WIFI_CLICK -> {
                    wifiLayout?.setBackgroundResource(R.drawable.ic_wifi_white_finish)
                    setUnChoose(
                        PostTypesFragment.LANGUAGE_CLICK,
                        languageLayout,
                        photoLayout,
                        wifiLayout
                    )
                    setUnChoose(
                        PostTypesFragment.PHOTO_CLICK,
                        languageLayout,
                        photoLayout,
                        wifiLayout
                    )
                }
            }
        }


        private fun setUnChoose(
            layout: Int,
            languageLayout: ConstraintLayout?,
            photoLayout: ConstraintLayout?,
            wifiLayout: ConstraintLayout?
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
            }
        }


        fun handleViewPagerChange(
            context: Context,
            position: Int,
            newPostPhotoText: TextView,
            newPostVideoText: TextView,
            newPostGifText: TextView,
            fragments: ArrayList<Fragment>
        ) {
            this.fragments = fragments

            when (position) {

                // Photo section
                0 -> {
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

                    mHandle.removeCallbacks(gifRunnable)
                    mHandle.removeCallbacks(photoRunnable)
                    mHandle.removeCallbacks(videoRunnable)
                    mHandle.postDelayed(photoRunnable, 200)
                }

                // Video section
                1 -> {
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

                    mHandle.removeCallbacks(gifRunnable)
                    mHandle.removeCallbacks(photoRunnable)
                    mHandle.removeCallbacks(videoRunnable)
                    mHandle.postDelayed(videoRunnable, 200)
                }

                // GIF section
                2 -> {
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

                    mHandle.removeCallbacks(gifRunnable)
                    mHandle.removeCallbacks(photoRunnable)
                    mHandle.removeCallbacks(videoRunnable)
                    mHandle.postDelayed(gifRunnable, 200)
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
}