package social.tsu.android.ui.post_feed.view_holders

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import social.tsu.android.R
import social.tsu.android.data.local.entity.Post
import social.tsu.android.service.OpenGraphService
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import android.view.animation.OvershootInterpolator
import com.ablanco.zoomy.Zoomy
import social.tsu.android.TsuApplication
import social.tsu.android.ui.MainActivity



class LinkPostHolder(
    application: TsuApplication,
    private val callback: ViewHolderActions,
    private val openGraphService: OpenGraphService,
    itemView: View,
    showCommunityInTitle: Boolean,
    dividerColor: Int = 0
) : PostViewHolder(application, callback, itemView, showCommunityInTitle, dividerColor) {

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


    override fun <T> bind(item: T) {
        val post = item as Post
        if (this.currentPost?.contentEquals(post) == true) return

        super.bind(item)

        openGraphService.info(post) { preview ->
            if (preview != null) {
                linkContainer?.show()
                linkHost?.text = preview.host
                linkTitle?.text = preview.title ?: preview.host

                if (postImage != null) {
                    if (preview.imageUrl != null) {
                        postImage.show()
                        if (itemView.context.isValidGlideContext()) {
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
            callback.didTapLink(link)
        }
    }

    private fun Context.isValidGlideContext() =
        this !is Activity || (!this.isDestroyed && !this.isFinishing)

    interface ViewHolderActions : PostViewHolder.ViewHolderActions {
        fun didTapLink(link: String)
    }
}