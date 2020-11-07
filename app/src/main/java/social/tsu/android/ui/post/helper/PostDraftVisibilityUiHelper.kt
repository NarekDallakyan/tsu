package social.tsu.android.ui.post.helper

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.ext.hide
import social.tsu.android.ext.show

object PostDraftVisibilityUiHelper {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "PostDraftVisibility",
        masterKeyAlias,
        TsuApplication.mContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     *  Saving Visibility option to Shared Preferences
     */
    private fun saveVisibility(position: Int) {

        sharedPreferences.edit().putInt("postDraftVisibility", position).apply()
    }

    /**
     *  Get Visibility option from Shared Preferences
     */
    private fun getVisibility(): Int {

        return sharedPreferences.getInt("postDraftVisibility", 0)
    }

    /**
     *  Handling choose visibility option
     */
    fun handleVisibilityOptionChoose(rootView: View?) {

        val choosesOption = getVisibility()

        val publicLayout: ConstraintLayout? = rootView?.findViewById(R.id.publicLayout)
        val privateLayout: ConstraintLayout? = rootView?.findViewById(R.id.privateLayout)
        val exclusiveLayout: ConstraintLayout? = rootView?.findViewById(R.id.exclusiveLayout)

        val publicChoose: ImageView? = publicLayout?.findViewById(R.id.publicChoose)
        val privateChoose: ImageView? = privateLayout?.findViewById(R.id.privateChoose)
        val exclusiveChoose: ImageView? = exclusiveLayout?.findViewById(R.id.exclusiveChoose)

        publicLayout?.setOnClickListener {
            // public clicked

            publicChoose?.show(animate = true)
            privateChoose?.hide(animate = true)
            exclusiveChoose?.hide(animate = true)
            // store chooses option to shared preferences
            saveVisibility(0)
        }

        privateLayout?.setOnClickListener {
            // private clicked

            publicChoose?.hide(animate = true)
            privateChoose?.show(animate = true)
            exclusiveChoose?.hide(animate = true)
            // store chooses option to shared preferences
            saveVisibility(1)
        }

        exclusiveLayout?.setOnClickListener {
            // exclusive clicked

            publicChoose?.hide(animate = true)
            privateChoose?.hide(animate = true)
            exclusiveChoose?.show(animate = true)
            // store chooses option to shared preferences
            saveVisibility(2)
        }

        when (choosesOption) {

            0 -> {
                publicChoose?.show(animate = true)
                privateChoose?.hide(animate = true)
                exclusiveChoose?.hide(animate = true)
            }

            1 -> {
                publicChoose?.hide(animate = true)
                privateChoose?.show(animate = true)
                exclusiveChoose?.hide(animate = true)
            }

            2 -> {
                publicChoose?.hide(animate = true)
                privateChoose?.hide(animate = true)
                exclusiveChoose?.show(animate = true)
            }
        }
    }

    fun getChoosesOption(): Int {

        return getVisibility()
    }

    fun showChoosesOption(rootView: View?) {

        val choosesText: TextView? = rootView?.findViewById(R.id.visibilityChoosesText)

        when (getVisibility()) {

            0 -> {
                choosesText?.text = "Public"
            }

            1 -> {
                choosesText?.text = "Private"
            }

            2 -> {
                choosesText?.text = "Exclusive"
            }
        }
    }
}