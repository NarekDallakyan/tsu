package social.tsu.android.ui.view

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText
import social.tsu.android.validation.Validator

class ValidationTextInputEditText: TextInputEditText {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var validator: Validator<String>? = null

    private val focusListeners: ArrayList<OnFocusChangeListener> = ArrayList()

    init{
        super.setOnFocusChangeListener { v, hasFocus ->
            for( listener in focusListeners) {
                listener.onFocusChange(v, hasFocus)
            }
        }
    }

    override fun setOnFocusChangeListener(l: OnFocusChangeListener?) {
        addOnFocusListener(l)
    }

    private fun addOnFocusListener(listener: OnFocusChangeListener?) {
        if(listener != null) {
            focusListeners.add(listener)
        } else {
            //clear all listeners as system probably clearing up views
            focusListeners.clear()
        }
    }

    private fun removeOnFocusListener(listener: OnFocusChangeListener?) {
        if(listener != null){
            focusListeners.remove(listener)
        }
    }

    /**
     * Just validate field if validator is available.
     * No error will be set to error field. Use if you need just to check if input is ok
     */
    fun validate(): Boolean {
        validator?.let{
            return it.validate(text.toString())
        }
        return true
    }

}