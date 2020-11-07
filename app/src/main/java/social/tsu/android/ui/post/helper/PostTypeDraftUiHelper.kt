package social.tsu.android.ui.post.helper

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Switch
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.android.synthetic.main.draft_post.view.*
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.ext.hide
import social.tsu.android.ext.show

object PostTypeDraftUiHelper {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "PostDraftVisibility",
        masterKeyAlias,
        TsuApplication.mContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     *  Handling description layout visibility
     */
    fun handleDescriptionUi(rootView: View?) {

        if (rootView == null) return

        val descriptionClose: RelativeLayout = rootView.descriptionCloseLayout
        val descriptionEditText: EditText = rootView.descriptionEditText

        descriptionEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                val fullText = p0?.toString() ?: ""
                if (fullText.isEmpty()) {
                    descriptionClose.hide(animate = true)
                } else {
                    descriptionClose.show(animate = true)
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })

        if (descriptionEditText.text.isEmpty()) {
            descriptionClose.hide(animate = true)
        } else {
            descriptionClose.show(animate = true)
        }

        // Listen close layout clicked
        descriptionClose.setOnClickListener {

            descriptionEditText.text.clear()
        }
    }

    /**
     *  Saving Save to device option to Shared Preferences
     */
    private fun saveSaveToDevice(position: Int) {

        sharedPreferences.edit().putInt("saveToDevice", position).apply()
    }

    /**
     *  Get Save to device option from Shared Preferences
     */
    private fun getSaveToDevice(): Int {

        return sharedPreferences.getInt("saveToDevice", 1)
    }

    /**
     *  Handling save to device visibility
     */
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun handleSaveToDeviceUi(rootView: View?) {

        if (rootView == null) return

        val saveToDeviceOption = getSaveToDevice()

        val saveToDeviceSwitcher: Switch = rootView.findViewById(R.id.saveToDeviceSwitcher)

        // listen save to device switch changing
        saveToDeviceSwitcher.setOnCheckedChangeListener { compoundButton, isChecked ->

            if (isChecked) {
                saveSaveToDevice(1)
            } else {
                saveSaveToDevice(0)
            }
        }

        saveToDeviceSwitcher.isChecked = saveToDeviceOption != 0
    }
}