package social.tsu.android.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.paypal.android.sdk.payments.PayPalConfiguration
import com.paypal.android.sdk.payments.PayPalOAuthScopes
import com.paypal.android.sdk.payments.PayPalProfileSharingActivity
import com.paypal.android.sdk.payments.PayPalService
import social.tsu.android.R
import social.tsu.android.ui.MainActivity

fun Fragment.exitAppOnBackPressed() {
    var shouldExitApp = false

    onBackPressed(true) {
        if (shouldExitApp) {
            activity?.finishAffinity()
        } else {
            shouldExitApp = true
            Toast.makeText(
                context!!,
                getString(R.string.initial_back_pressed_message),
                Toast.LENGTH_LONG
            )
                .show()
            Handler().postDelayed({
                shouldExitApp = false
            }, 5000)
        }
    }
}

fun Fragment.onBackPressed(overrideDefault: Boolean, block: () -> Unit) {

    view?.isFocusableInTouchMode = true
    view?.requestFocus()
    view?.setOnKeyListener(object : View.OnKeyListener {
        override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                block.invoke()
                return overrideDefault
            }
            return false
        }
    })
}


fun Fragment.findNavControllerOrNull() = try {
    findNavController()
} catch (e: Exception) {
    null
}

fun Fragment.findParentNavController(): NavController =
    requireActivity().findNavController(R.id.nav_host_fragment)

val Fragment.supportActionBar: ActionBar?
    get() {
        val activity = this.activity
        if (activity is AppCompatActivity) {
            return activity.supportActionBar
        }
        return null
    }

fun Fragment.dismissKeyboard() {
    val inputMethodManager =
        context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

    inputMethodManager?.hideSoftInputFromWindow(view?.windowToken, 0)
}

/**
 * Creates intent and launches chooser with provided requestCode and type
 */
fun Fragment.pickMediaFromGallery(requestCode: Int, type: String) {
    try {
        val libraryIntent = context?.getPickIntent(type) ?: return

        if (libraryIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(libraryIntent, requestCode)
        }
    } catch (e: ActivityNotFoundException) {
        Log.e(this.javaClass.simpleName, "pickImageFromGallery", e)
    }
}

fun Fragment.snack(message: String) {
    view?.let {
        try {
            it.getSnackBar(message).show()
        } catch (ex: IllegalArgumentException) {
            //snack called after fragment was detached
            Log.e("FragmentExt", "Called snack() with invalid parent")
        }
    }
}

fun Fragment.snack(@StringRes message: Int) {
    view?.let {
        try {
            it.getSnackBar(it.context.getString(message)).show()
        } catch (ex: IllegalArgumentException) {
            //snack called after fragment was detached
            Log.e("FragmentExt", "Called snack() with invalid parent")
        }
    }
}

fun Fragment.updateLoginStatus() {
    val activity = this.activity
    if (activity is MainActivity) {
        activity.checkLoggedIn()
    }
}

fun Fragment.updateProfileIcon(drawable: Drawable?) {
    val activity = this.activity
    if (activity is MainActivity) {
        activity.updateProfilePhoto(drawable)
    }
}

fun Fragment.bindUserInfo(fullName: String?, username: String?, profilePictureUrl: String?, verifyStatus : Int?) {
    val activity = this.activity
    if (activity is MainActivity) {
        activity.bindUserInfo(fullName, username, profilePictureUrl,verifyStatus)
    }
}

fun Fragment.createPayPalIntent(
    payPalConfiguration: PayPalConfiguration,
    oauthScopes: PayPalOAuthScopes
): Intent {
    val intent = Intent(requireActivity(), PayPalProfileSharingActivity::class.java)
    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, payPalConfiguration)
    intent.putExtra(PayPalProfileSharingActivity.EXTRA_REQUESTED_SCOPES, oauthScopes)
    return intent
}

fun Fragment.setScreenOrientation(value: Int) {
    requireActivity().requestedOrientation = value
}