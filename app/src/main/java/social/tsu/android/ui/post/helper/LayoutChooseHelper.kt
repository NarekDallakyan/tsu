package social.tsu.android.ui.post.helper

import androidx.constraintlayout.widget.ConstraintLayout
import social.tsu.android.R
import social.tsu.android.ui.post.view.PostTypesFragment

class LayoutChooseHelper {
    companion object {

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


    }
}