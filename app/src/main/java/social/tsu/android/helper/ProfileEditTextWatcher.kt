package social.tsu.android.helper

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText

interface ProfileEditTextWatcherCallback {
    fun didUpdateText(type: ProfileEditTextWatcherType, value: String)
    fun didEndEditing()
}

enum class ProfileEditTextWatcherType {
    bio,
    website,
    youtube,
    twitter,
    instagram,
    facebook
}

class ProfileEditTextWatcher(val type: ProfileEditTextWatcherType, val callback: ProfileEditTextWatcherCallback): TextWatcher, View.OnKeyListener {

    override fun afterTextChanged(s: Editable?) {
        callback.didUpdateText(type, s.toString())
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event?.action == KeyEvent.ACTION_UP) {
            (v as? EditText).let {
                callback.didUpdateText(type, it?.text.toString())
                callback.didEndEditing()
            }
            return true
        }
        return false
    }

}