package social.tsu.android.ui.post_feed.view_holders

import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.adapters.viewholders.PostViewHolder.Companion.POST_IMAGE_THUMBNAIL_QUALITY
import social.tsu.android.helper.DateHelper
import social.tsu.android.helper.TSUTextTokenizingHelper
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.model.PendingPost
import social.tsu.android.service.ImageFetchHandler
import social.tsu.android.utils.hide
import social.tsu.android.utils.show

class PendingPostViewHolder(
    application: TsuApplication,
    private val callback: ViewHolderActions,
    itemView: View
) : PostViewHolder(application,null, itemView) {


    override fun <T> bind(item: T) {
        if (item == null) return

        val post = item as PendingPost
        post.user?.profilePictureUrl?.let { url ->
            callback.getProfilePicture(url, false) {
                it?.let {
                    userIcon?.setImageDrawable(it)
                } ?: run {
                    userIcon?.setImageResource(R.drawable.user)
                }
            }
        }

        post.user?.let {
            username?.text = post.user.fullname
        }

        var content = TSUTextTokenizingHelper.normalize(itemView.context, post.content)
        content =
            TSUTextTokenizingHelper.tokenize(itemView.context, content, callback::didTagUser, callback::didTapHashtag)
        postContent?.text = content
        postContent?.movementMethod = LinkMovementMethod.getInstance()

        itemView.findViewById<Button>(R.id.approveButton)
            .setOnClickListener { callback.didApprove(post) }
        itemView.findViewById<Button>(R.id.declineButton)
            .setOnClickListener { callback.didDecline(post) }

        if (postImage != null && post.pictureUrl.isNotEmpty()) {
            postImage.show()
            Glide.with(itemView.context)
                .load(formatUrl(post))
                .thumbnail(POST_IMAGE_THUMBNAIL_QUALITY)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(postImage)
        } else {
            postImage?.hide()
        }
        postDate?.text = DateHelper.prettyDate(itemView.context, post.createdAt.toLong() * 1000)
    }

    private fun formatUrl(post: PendingPost): String {
        // Handle relative paths from the API_HOST
        if (post.pictureUrl.startsWith("/")) {
            return "${HostProvider.imageHost}${post.pictureUrl}"
        }

        return post.pictureUrl
    }

    interface ViewHolderActions {

        fun getProfilePicture(
            key: String?,
            ignoringCache: Boolean,
            handler: ImageFetchHandler?
        )


        fun didApprove(post: PendingPost)
        fun didDecline(post: PendingPost)
        fun didTapHashtag(hashtag: String)
        fun didTagUser(tagUser:String)
    }
}