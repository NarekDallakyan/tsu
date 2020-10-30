package social.tsu.android.ui

import android.graphics.BitmapFactory
import android.view.View
import android.view.View.*
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.BindingConversion
import com.google.android.material.textfield.TextInputLayout
import social.tsu.android.BirthDate
import social.tsu.android.R
import social.tsu.android.ui.ContactRepository.Companion.fetchContactPhoto
import social.tsu.android.ui.view.ValidationTextInputEditText

@BindingAdapter("bind:contactImage")
fun bindContactImage(imageView: ImageView, contactId: Int) {
    val image = fetchContactPhoto(imageView.context.contentResolver, contactId.toLong())

    if (image == null) {
        imageView.setImageResource(R.drawable.user)

    } else {
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
        imageView.setImageBitmap(bitmap)
    }
}

@BindingAdapter("visibleOrGone")
fun View.setVisibleOrGone(show: Boolean) {
    visibility = if (show) VISIBLE else GONE
}


@BindingAdapter("visible")
fun View.setVisible(show: Boolean) {
    visibility = if (show) VISIBLE else INVISIBLE
}

@BindingAdapter("app:errorText")
fun setErrorMessage(view: TextInputLayout, errorMessage: Int) {
    if (errorMessage > 0) {
        view.isErrorEnabled = true
        view.error = view.resources.getString(errorMessage)
    } else {
        view.isErrorEnabled = false
        view.error = null
    }
}

@BindingAdapter("setText")
fun setError(textView: TextView, strOrResId: Any?) {
    if (strOrResId is Int) {
        textView.error = textView.context.getString((strOrResId as Int?)!!)
    } else {
        textView.error = strOrResId as String?
    }
}

@BindingAdapter( "android:text")
fun text(textView: ValidationTextInputEditText, date: BirthDate) {
    textView.setText(date.stringValue)
}

@BindingConversion
fun birthdayToString(date: BirthDate): String{
    return  date.stringValue
}
