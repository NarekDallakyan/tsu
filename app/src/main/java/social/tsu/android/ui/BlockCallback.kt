package social.tsu.android.ui

interface BlockCallback {

    fun onBlocked(userId: Int)
    fun onUnblocked(userId: Int)
}