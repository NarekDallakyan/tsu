package social.tsu.android.adapters.viewholders

import android.view.View
import android.widget.EditText
import social.tsu.android.R
import social.tsu.android.TsuApplication

interface CreatePostViewHolderCallback {
    fun didTapOnCamera(composedText:String)
    fun didTapOnVideo(composedText:String)
    fun didTapOnPost()
}

open class CreatePostViewHolder(
    application: TsuApplication,
    private val callback: CreatePostViewHolderCallback?,
    itemView: View
) : PostViewHolder(application, null, itemView) {

    private val composeTextEdittext = itemView.findViewById<EditText>(R.id.composePost)

    fun bind() {
        val openCameraActivity = View.OnClickListener {
            callback?.didTapOnCamera(composeTextEdittext.text.toString())
        }

        val openVideoCaptureFragment = View.OnClickListener {
            callback?.didTapOnVideo(composeTextEdittext.text.toString())
        }

        itemView.findViewById<View>(R.id.btn_add_photo).setOnClickListener(openCameraActivity)

        itemView.findViewById<View>(R.id.btn_add_video).setOnClickListener(openVideoCaptureFragment)

        itemView.findViewById<View>(R.id.btn_post).setOnClickListener {
            callback?.didTapOnPost()
        }
    }

}
