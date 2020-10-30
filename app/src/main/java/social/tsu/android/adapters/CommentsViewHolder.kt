package social.tsu.android.adapters

import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.comment_item_layout.view.*
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.Constants
import social.tsu.android.helper.DateHelper
import social.tsu.android.helper.TSUTextTokenizingHelper
import social.tsu.android.network.model.Comment
import social.tsu.android.network.model.User
import social.tsu.android.service.IS_TEXT_COPY_ENABLED
import social.tsu.android.service.UserProfileImageService
import social.tsu.android.ui.util.TextCopyUtils
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import java.util.*
import javax.inject.Inject


class CommentsViewHolder(
    private val application: TsuApplication,
    private val actions: CommentsAdapter.ViewHolderActions,
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    @Inject
    lateinit var imageService: UserProfileImageService

    private val commentLikeButton = itemView.comment_like_button
    private val commentLikeIcon = itemView.comment_like_icon
    private val commentLikeCount = itemView.comment_like_number
    private val commentDelete = itemView.comment_delete
    private val commentUserIcon = itemView.comment_user_icon
    private val commentTextView = itemView.comment_text_view

    init {
        application.appComponent.viewHolderComponent().create().inject(this)
    }

    fun updateWithPost(post: Post) {
        val user = post.original_user ?: post.user
        updateUserProfilePhoto(user)
        updateComment(post.content, user)
        updateTimestamp(post.created_at)
        commentLikeButton.hide()
        commentDelete.hide()
    }

    fun updateWithComment(comment: Comment) {
        commentLikeButton.show()
        if (actions.enableDeleteComment(comment)) {
            commentDelete.show()
        } else {
            commentDelete.hide()
        }
        updateUserProfilePhoto(comment.user)
        updateComment(comment.body, comment.user)
        updateTimestamp(Date(comment.timestamp.toLong() * 1000L))
        updateLikeStatus(comment)
        commentLikeButton.setOnClickListener {
            if (comment.hasLiked) {
                actions.unlikeComment(comment)
            } else actions.likeComment(comment)
        }
        commentDelete.setOnClickListener {
            actions.deleteComment(comment)
        }
    }

    private fun updateLikeStatus(comment: Comment) {
        if (comment.likeCount == 0) {
            commentLikeCount?.visibility = View.INVISIBLE
            commentLikeIcon?.setImageResource(R.drawable.ic_like)
            commentLikeIcon?.imageTintList =
                ContextCompat.getColorStateList(itemView.context, R.color.primaryButton)
        } else {
            commentLikeCount?.show()
            commentLikeCount?.text = comment.likeCount.toString()
            if (comment.hasLiked) {
                commentLikeIcon?.setImageResource(R.drawable.ic_like_filled)
                commentLikeIcon?.imageTintList =
                    ContextCompat.getColorStateList(itemView.context, R.color.tsu_primary)
            } else {
                commentLikeIcon?.setImageResource(R.drawable.ic_like)
                commentLikeIcon?.imageTintList =
                    ContextCompat.getColorStateList(itemView.context, R.color.primaryButton)
            }
        }
    }

    private fun updateUserProfilePhoto(user: User) {
        imageService.getProfilePicture(user.profilePictureUrl, false) { image ->
            image?.let {
                itemView.findViewById<CircleImageView>(R.id.comment_user_icon)
                    ?.setImageDrawable(image)
            } ?: run {
                itemView.findViewById<CircleImageView>(R.id.comment_user_icon)
                    ?.setImageResource(R.drawable.user)
            }
        }
        commentUserIcon?.setOnClickListener {
            actions.onUserTap(user)
        }

    }

    private fun updateComment(value: String, user: User) {
        val userName =
            if (Constants.isVerified(user.verifiedStatus)) user.fullName.plus(" ") else user.fullName
        var content = TSUTextTokenizingHelper.normalize(application, "$userName $value")
        content = TSUTextTokenizingHelper.boldify(application, user.fullName ?: "", content)
        content = TSUTextTokenizingHelper.tokenize(
            application,
            content,
            actions::onTagUserTap,
            actions::onHashtagTap
        )
        TSUTextTokenizingHelper.clickable(
            application,
            content,
            user.fullName,
            TSUTextTokenizingHelper.TsuClickableTextStyle.BOLD
        ) {
            actions.onUserTap(user)
        }

        if (Constants.isVerified(user.verifiedStatus)) {
            val imageSpan = ImageSpan(application.applicationContext, R.drawable.ic_verified_small)
            content.setSpan(imageSpan, user.fullName!!.length, user.fullName.length + 1, 0)
        }


        commentTextView?.text = content
        commentTextView?.movementMethod = LinkMovementMethod.getInstance()
        if (IS_TEXT_COPY_ENABLED) {
            itemView.findViewById<TextView>(R.id.comment_text_view).setTextIsSelectable(true)
            itemView.findViewById<TextView>(R.id.comment_time_view).setTextIsSelectable(true)
            TextCopyUtils.textGestureDetectorCompat(
                itemView.context,
                itemView.findViewById(R.id.comment_time_view)
            )
        }
    }

    private fun updateTimestamp(value: Date) {
        itemView.findViewById<TextView>(R.id.comment_time_view)?.text =
            DateHelper.prettyDate(itemView.context, value)
    }

}

class CommentsLoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)