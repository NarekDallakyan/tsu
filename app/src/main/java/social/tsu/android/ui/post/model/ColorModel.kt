package social.tsu.android.ui.post.model

class ColorModel(
    var color: ColorEnum,
    var isSelected: Boolean = false
) {

    enum class ColorEnum(var value: String) {
        White("#FFFFFF"),
        Black("#000000"),
        Yellow("#FFB734"),
        Pink("#FF6B6B"),
        Red("#D21010"),
        Mint("#4ECDC4"),
        Green("#8AD22C"),
        Blue("#656CF4"),
        Purple("#8F4DD8")
    }
}