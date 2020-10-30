package social.tsu.android.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import social.tsu.android.R
import social.tsu.android.utils.hide
import social.tsu.android.utils.show

class SettingsItemView : ConstraintLayout {

    private lateinit var arrowIcon: ImageView
    private lateinit var titleView: TextView
    private lateinit var valueView: TextView

    var isArrowVisible: Boolean
        set(value) {
            if (value) arrowIcon.show() else arrowIcon.hide()
        }
        get() = arrowIcon.isVisible

    var titleText: CharSequence?
        set(value) {
            titleView.text = value
        }
        get() = titleView.text

    var valueText: CharSequence?
        set(value) {
            valueView.text = value
        }
        get() = valueView.text

    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.user_settings_item, this, true)

        arrowIcon = findViewById(R.id.settings_item_arrow_icon)
        titleView = findViewById(R.id.settings_item_title)
        valueView = findViewById(R.id.settings_item_value)

        if (attrs != null) {
            val attrsArray = context.obtainStyledAttributes(attrs, R.styleable.SettingsItemView)
            isArrowVisible = attrsArray.getBoolean(R.styleable.SettingsItemView_enable_arrow, true)
            titleText = attrsArray.getString(R.styleable.SettingsItemView_item_title)
            valueText = attrsArray.getString(R.styleable.SettingsItemView_item_value)
            attrsArray.recycle()
        }
    }


}