package social.tsu.android.adapters.viewholders

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.ablanco.zoomy.Zoomy
import com.bumptech.glide.Glide
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.service.OpenGraphService
import social.tsu.android.ui.MainActivity
import social.tsu.android.utils.hide
import social.tsu.android.utils.show


interface LinkPostViewHolderCallback : PostViewHolderCallback {
    fun didTapLink(link: String)
}

class LinkPostHolder(
    private val application: TsuApplication,
    private val callback: LinkPostViewHolderCallback?,
    private val openGraphService: OpenGraphService,
    itemView: View,
    showCommunityInTitle: Boolean
) : PostViewHolder(application, callback, itemView, showCommunityInTitle) {

    private val linkContainer: LinearLayout? = itemView.findViewById(R.id.post_link_container)
    private val linkHost: TextView? = itemView.findViewById(R.id.post_link_host)
    private val linkTitle: TextView? = itemView.findViewById(R.id.post_link_title)

    init {
        postContent?.setOnClickListener(::onClickLink)
        postImage?.setOnClickListener(::onClickLink)
        linkContainer?.setOnClickListener(::onClickLink)
    }

    override fun reset() {
        super.reset()
        linkContainer?.hide()
    }

    override fun bind(post: Post) {
        if (this.currPost?.contentEquals(post) == true) return

        super.bind(post)

        openGraphService.info(post) { preview ->
            if (preview != null) {
                linkContainer?.show()
                linkHost?.text = preview.host
                linkTitle?.text = preview.title ?: preview.host

                if (postImage != null) {
                    if (preview.imageUrl != null) {
                        postImage.show()
                        if(itemView.context.isValidGlideContext()) {
                            Glide.with(itemView)
                                .asBitmap()
                                .load(preview.imageUrl)
                                .into(postImage)
                            val builder: Zoomy.Builder = Zoomy.Builder(MainActivity.instance)
                                .target(postImage)
                                .interpolator(OvershootInterpolator())

                            builder.register()
                        }
                    } else {
                        postImage.hide()
                    }
                }
            } else {
                linkContainer?.hide()
            }
        }
    }

    private fun onClickLink(view: View) {
        currentPost?.getLinks()?.firstOrNull()?.let { link ->
            callback?.didTapLink(link)
        }
    }

    fun Context.isValidGlideContext() = this !is Activity || (!this.isDestroyed && !this.isFinishing)

}