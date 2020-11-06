package social.tsu.android.ext

enum class FileType(val value: String) {
    VIDEO("Video"), PHOTO("Photo"), MEDIA("Media")
}

fun String.getFileType(): FileType? {

    try {

        val filePath = this
        val lastDotIndex = filePath.lastIndexOf(".")

        when (filePath.substring(lastDotIndex + 1)) {

            "png" -> {
                return FileType.PHOTO
            }

            "webp" -> {
                return FileType.PHOTO
            }

            "jpeg" -> {
                return FileType.PHOTO
            }

            "mp4" -> {
                return FileType.VIDEO
            }
            else -> {
                return FileType.MEDIA
            }

        }
    } catch (error: Exception) {
        return null
    }


}