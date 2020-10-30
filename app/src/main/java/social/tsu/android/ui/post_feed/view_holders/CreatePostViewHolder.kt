package social.tsu.android.ui.post_feed.view_holders

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import social.tsu.android.R
import social.tsu.android.TsuApplication


open class CreatePostViewHolder(
    application: TsuApplication,
    private val callback: ViewHolderActions,
    itemView: View
) : PostViewHolder(application, null, itemView) {

    private val composeTextEdittext = itemView.findViewById<EditText>(R.id.composePost)

    override fun <T> bind(item: T) {
        val openCameraActivity = View.OnClickListener {
            callback.didTapOnCamera(composeTextEdittext.text.toString())
        }

        val openVideoCaptureFragment = View.OnClickListener {
            callback.didTapOnVideo(composeTextEdittext.text.toString())
        }

        itemView.findViewById<View>(R.id.btn_add_photo).setOnClickListener(openCameraActivity)

        itemView.findViewById<View>(R.id.btn_add_video).setOnClickListener(openVideoCaptureFragment)

        val postEdit = itemView.findViewById<View>(R.id.composePost) as EditText
        postEdit.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {

            }

            override fun afterTextChanged(arg0: Editable) {
                val value: String? = arg0.toString()
                if (value.isNullOrEmpty()) {
                    return
                }

                if (value.contains("@")) {
                    val split = value.split(" ")
                    for (item in split) {
                        if (item.equals("@", true) && item.length == 1) {
                            Log.e("first", "position : "+item)
                            callback.openMentionSearchFragment(
                                composeTextEdittext.text.toString(),
                                postEdit.selectionStart
                            )
                            break
                        } else {
                            val split2 = item.split("\n")
                            for (item2 in split2) {
                                if (item2.equals("@", true) && item2.length == 1) {
                                    Log.e("second", "position : "+value.toString().indexOf(item.plus(item2)))
                                    callback.openMentionSearchFragment(
                                        composeTextEdittext.text.toString(),
                                        postEdit.selectionStart
                                    )
                                    break
                                }
                            }
                        }
                    }
                }
            }
        })

        itemView.findViewById<View>(R.id.btn_post).setOnClickListener {
            callback.didTapOnPost(composeTextEdittext.text.toString())
            composeTextEdittext.setText("")
        }
    }

    interface ViewHolderActions {
        fun didTapOnVideo(composedText: String)
        fun didTapOnPost(text: String)
        fun didTapOnCamera(composedText: String)
        fun openMentionSearchFragment(composedText: String, position: Int = 0)
    }

}
