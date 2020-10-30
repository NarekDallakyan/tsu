package social.tsu.android.helper

object Constants {
    //TODO: fix the verified account status below when including the verified accounts.
    const val USER_VERIFIED = 1
    const val USER_VERIFIED_2 = 2
    const val IMAGE_URI = "video_uri"
    const val VIDEO_URI = "image_uri"

    fun isVerified(verifyUser: Int?): Boolean {
        verifyUser?.let {
            return verifyUser == USER_VERIFIED || verifyUser == USER_VERIFIED_2
        } ?: kotlin.run {
            return false
        }
    }
}