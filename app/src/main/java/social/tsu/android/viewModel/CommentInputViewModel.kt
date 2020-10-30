package social.tsu.android.viewModel

class CommentInputViewModel {

    var numberOfDefaultEmojis: Int = 12

    fun emoji(index: Int): String {
        when(index) {
            0 -> return "❤️"
            1 -> return "🔥"
            2 -> return "💩"
            3 -> return "😂"
            4 -> return "😕️"
            5 -> return "😘"
            6 -> return "😎"
            7 -> return "😡"
            8 -> return "😢"
            9 -> return "👍"
            10 -> return "👏"
            11 -> return "🙌"
        }
        return ""
    }

}