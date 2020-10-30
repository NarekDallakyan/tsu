package social.tsu.android.adapters.viewholders

import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.DateHelper
import social.tsu.android.helper.TSUTextTokenizingHelper
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.model.PendingPost
import social.tsu.android.utils.hide
import social.tsu.android.utils.show

interface PendingPostViewHolderCallback {
    fun didApprove(post: PendingPost)
    fun didDecline(post: PendingPost)
    fun didTapHashtag(hashtag: String)
    fun didTapTagUser(tagUser :String)
    fun onUserClick(id: Int?)
}

open class PendingPostViewHolder(
    private val application: TsuApplication,
    private val callback: PendingPostViewHolderCallback?,
    itemView: View
) : PostViewHolder(application, null, itemView) {


    fun bind(post: PendingPost) {

        post.user?.profilePictureUrl?.let { url ->
            userProfileImageService.getProfilePicture(url, false) {
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
        userIcon?.setOnClickListener {
            callback?.onUserClick(post.user?.id)
        }

        username?.setOnClickListener {
            callback?.onUserClick(post.user?.id)
        }

        var content = TSUTextTokenizingHelper.normalize(application, post.content)
        content = TSUTextTokenizingHelper.tokenize(application, content, {
            callback?.didTapTagUser(it)
        }, { hashtag ->
            callback?.didTapHashtag(hashtag)
        })
        postContent?.text = content
        postContent?.movementMethod = LinkMovementMethod.getInstance()

        itemView.findViewById<ImageView>(R.id.post_more_icon)?.visibility = View.GONE

        itemView.findViewById<Button>(R.id.approveButton)
            .setOnClickListener { callback?.didApprove(post) }
        itemView.findViewById<Button>(R.id.declineButton)
            .setOnClickListener { callback?.didDecline(post) }

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
        postDate?.text = DateHelper.prettyDate(application, post.createdAt.toLong() * 1000)
    }

    private fun formatUrl(post: PendingPost): String {
        // Handle relative paths from the API_HOST
        if (post.pictureUrl.startsWith("/")) {
            return "${HostProvider.imageHost}${post.pictureUrl}"
        }

        return post.pictureUrl
    }
}