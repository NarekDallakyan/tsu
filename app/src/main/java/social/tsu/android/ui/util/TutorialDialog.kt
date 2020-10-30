package social.tsu.android.ui.util

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.service.SharedPrefManager
import javax.inject.Inject

class TutorialDialog private constructor(
    private val titleText: String,
    private val imageDrawable: Int,
    private val key: String
) : DialogFragment() {

    data class Builder(
        private var titleText: String = "",
        private var tutorialKey: String = "",
        private var imageDrawable: Int = 0
    ) {
        fun title(text: String) = apply { this.titleText = text }
        fun image(drawable: Int) = apply { this.imageDrawable = drawable }
        fun key(key: String) = apply { this.tutorialKey = key }
        fun build() = TutorialDialog(titleText = titleText, imageDrawable = imageDrawable, key = tutorialKey)
    }

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        if (sharedPrefManager.isTutorialCompleted(key)) {
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.dialog_tutorial, container, false)
        val tutorialText = view.findViewById<TextView>(R.id.tutorialText)
        val tutorialImage = view.findViewById<ImageView>(R.id.tutorialImage)
        val closeButton = view.findViewById<ImageView>(R.id.closeButton)
        tutorialText.text = titleText
        tutorialImage.setImageResource(imageDrawable)
        closeButton.setOnClickListener {
            sharedPrefManager.setTutorialCompleted(key)
            dismiss()
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
}