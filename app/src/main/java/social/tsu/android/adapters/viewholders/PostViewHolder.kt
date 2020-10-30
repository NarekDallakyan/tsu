package social.tsu.android.adapters.viewholders

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.ablanco.zoomy.Zoomy
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.post_video.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.DateHelper
import social.tsu.android.helper.TSUTextTokenizingHelper
import social.tsu.android.helper.*
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.model.User
import social.tsu.android.service.IS_TEXT_COPY_ENABLED
import social.tsu.android.service.SharedPrefManager
import social.tsu.android.service.UserProfileImageService
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.util.TextCopyUtils
import social.tsu.android.utils.applyTo
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import javax.inject.Inject


interface PostViewHolderCallback {
    fun didTapLikeOn(post: Post)
    fun didTapCommentOn(post: Post)
    fun didTapShowLikes(post: Post)
    fun didTapOnUser(userId: Long)
    fun didTapOnGroup(groupId: Int)
    fun didTapOnShare(post: Post)
    fun didTapMoreOptions(post: Post)
    fun didTapHashtag(hashtag: String)
    fun didTapTagUser(tagUser: String)
}

@SuppressLint("ClickableViewAccessibility")
open class PostViewHolder(
    private val application: TsuApplication,
    private val callback: PostViewHolderCallback?,
    itemView: View,
    private val showCommunityInTitle: Boolean = false
) : RecyclerView.ViewHolder(itemView), CoroutineScope by MainScope() {

    companion object {
        const val POST_IMAGE_THUMBNAIL_QUALITY = 0.4f
    }

    @Inject
    lateinit var userProfileImageService: UserProfileImageService

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    val currentPost: Post?
        get() = currPost

    protected var currPost: Post? = null

    private val likeButton: ImageButton? = itemView.findViewById(R.id.likeButton)

    private val shareButton: ImageButton? = itemView.findViewById(R.id.shareButton)
    private val shareProgress: ProgressBar? = itemView.findViewById(R.id.share_progress)
    private val commentButton: ImageButton? = itemView.findViewById(R.id.commentButton)

    private val moreIcon: ImageView? = itemView.findViewById(R.id.post_more_icon)

    protected val postImage: AppCompatImageView? = itemView.findViewById(R.id.post_item_image)
    protected val userIcon: ImageView? = itemView.findViewById(R.id.post_item_user_icon)
    protected val username: TextView? = itemView.findViewById(R.id.post_item_username)
    protected val postDate: TextView? = itemView.findViewById(R.id.post_item_date)
    protected val postContent: TextView? = itemView.findViewById(R.id.post_item_content)

    private val shareInfo: TextView? = itemView.findViewById(R.id.post_share_text)
    private val footerDivider: View? = itemView.findViewById(R.id.post_footer_divider)
    private val commentsCount: TextView? = itemView.findViewById(R.id.post_comments_count)
    private val likesCount: TextView? = itemView.findViewById(R.id.post_likes_count)
    private val sharesCount: TextView? = itemView.findViewById(R.id.post_shares_count)

    private val onClickListener = object : View.OnClickListener {

        override fun onClick(view: View?) {
            val post = this@PostViewHolder.currPost ?: return
            when (view?.id) {
                R.id.shareButton -> onShareClick(post)
                R.id.commentButton -> callback?.didTapCommentOn(post)
                R.id.post_comments_count -> callback?.didTapCommentOn(post)
                R.id.likeButton -> onLikeClick(post)
                R.id.post_likes_count -> callback?.didTapShowLikes(post)
                R.id.post_more_icon -> callback?.didTapMoreOptions(post)
                R.id.post_item_user_icon -> onUserClick(post)
                R.id.post_item_username -> onUserClick(post)
            }
        }
    }

    init {
        application.appComponent.viewHolderComponent().create().inject(this)

        onClickListener.applyTo(
            shareButton, commentButton, likeButton, moreIcon, userIcon, likesCount, commentsCount
        )
        shareInfo?.movementMethod = LinkMovementMethod.getInstance()
        username?.movementMethod = LinkMovementMethod.getInstance()
    }

    open fun reset() {
        postContent?.text = ""
        postDate?.text = ""
        footerDivider.hide()
        commentsCount.hide()
        likesCount.hide()
        sharesCount.hide()
    }

    open fun bind(post: Post) {
        if (this.currPost?.contentEquals(post) == true) return

        val oldPost = currPost
        this.currPost = post

        if (post.is_share) {
            updateForSharedPost(post)
        } else {
            shareInfo.hide()
            updateProfilePhoto(post.user)
            updateUserName(post.user, post)
        }
        updateLikeStatus(post)
        updateCommentStatus(post)
        updateShareStatus(post)
        postDate?.text = DateHelper.prettyDate(application, post.created_at)



        if (post.content.isNotBlank() && postContent != null) {
            postContent.show()
            postContent.text = tokenize(post.content)
            postContent.movementMethod = LinkMovementMethod.getInstance()
        } else {
            postContent.hide()
        }
        val supportPostIds = ArrayList<String>()
        sharedPrefManager.getSupportPostId()?.let { ids ->
            supportPostIds.addAll(ids.split(","))
        }
        if (post.privacy == Post.PRIVACY_EXCLUSIVE
            && post.user_id != AuthenticationHelper.currentUserId && supportPostIds.contains(
                post.id.toString()
            ).not()
        ) {
            shareButton?.visibility = View.INVISIBLE
            likeButton?.visibility = View.INVISIBLE
            commentButton?.visibility = View.INVISIBLE
            likesCount?.visibility = View.GONE
            commentsCount?.visibility = View.GONE
            footerDivider?.visibility = View.INVISIBLE
            sharesCount?.visibility = View.GONE
        } else {
            shareButton?.visibility = View.VISIBLE
            likeButton?.visibility = View.VISIBLE
            commentButton?.visibility = View.VISIBLE
        }
        if (post.privacy == Post.PRIVACY_EXCLUSIVE
            && post.user_id != AuthenticationHelper.currentUserId && supportPostIds.contains(
                post.id.toString()
            ).not()
        ) {
            postContent?.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            val radius: Float = postContent?.textSize!! / 3
            val filter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
            postContent.paint.maskFilter = filter
        } else {
            postContent?.paint?.maskFilter = null
        }
        if (post.has_picture && postImage != null) {
            postImage.show()
            if (oldPost?.picture_url != post.picture_url) {
                Glide.with(itemView.context)
                    .load(formatUrl(post))
                    .addListener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            post.let {
                                val supportPostIds = ArrayList<String>()
                                sharedPrefManager.getSupportPostId()?.let {
                                    supportPostIds.addAll(it.split(","))
                                }
                                resource?.let {
                                    if (post.privacy == Post.PRIVACY_EXCLUSIVE
                                        && post.user_id != AuthenticationHelper.currentUserId && supportPostIds.contains(
                                            post.id.toString()
                                        ).not()
                                    ) {
                                        Blurry.with(itemView.context).radius(25)
                                            .from(drawableToBitmap(resource)).into(postImage)
                                        itemView.tvSupport?.visibility = View.VISIBLE
                                        return true
                                    } else {
                                        itemView.tvSupport?.visibility = View.GONE
                                        return false

                                    }
                                }
                            }?: kotlin.run {
                                return false
                            }
                        }
                    })
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

    private fun onLikeClick(post: Post) {
        likeButton?.let {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
        }
        // To avoid double click
        likeButton?.isEnabled = false
        launch {
            callback?.didTapLikeOn(post)
            delay(300)
            likeButton?.isEnabled = true
        }
    }

    private fun onShareClick(post: Post) {
        // To avoid double click
        shareButton?.visibility = View.INVISIBLE
        shareProgress.show()
        launch {
            callback?.didTapOnShare(post)
            delay(300)
            shareButton.show()
            shareProgress.hide()
        }
    }

    private fun onUserClick(post: Post) {
        post.original_user?.let {
            if (it.id != null) callback?.didTapOnUser(it.id)
        } ?: run {
            if (post.user.id != null) callback?.didTapOnUser(post.user.id)
        }
    }

    private fun updateUserName(user: User, post: Post) {
        if (username == null) return

        val userFullName =
            if (Constants.isVerified(user.verifiedStatus)) user.fullName.plus(" ") else user.fullName
        val showedName = if (post.groupName != null && showCommunityInTitle) {
            "$userFullName ▸ ${post.groupName}"
        } else {
            userFullName
        }

        val text = if (post.action == 1 || post.action == 2) {
            application.getString(R.string.post_user_updated_profile, showedName)
        } else {
            showedName
        }
        val input = SpannableString(text)
        if (user.id != null) {
            TSUTextTokenizingHelper.clickable(application, input, userFullName) {
                callback?.didTapOnUser(user.id)
            }
        }
        if (post.groupId != null) {
            TSUTextTokenizingHelper.clickable(application, input, post.groupName) {
                callback?.didTapOnGroup(post.groupId)
            }
        }
        TSUTextTokenizingHelper.boldifySymbol(application, input, '▸')

        if (input.length == userFullName!!.length) {
            //  input = SpannableString(input.toString().plus(" "))
            if (Constants.isVerified(user.verifiedStatus)) {
                val imageSpan =
                    ImageSpan(application.applicationContext, R.drawable.ic_verified_small)
                input.setSpan(imageSpan, input.length - 1, input.length, 0)
            }
        } else {
            if (Constants.isVerified(user.verifiedStatus)) {
                val imageSpan =
                    ImageSpan(application.applicationContext, R.drawable.ic_verified_small)
                input.setSpan(imageSpan, userFullName.length, userFullName.length + 1, 0)
            }
        }

        username.text = input
        if (IS_TEXT_COPY_ENABLED) {
            postContent?.setTextIsSelectable(true)
            TextCopyUtils.textGestureDetectorCompat(itemView.context, postContent)
        }
    }

    private fun updateForSharedPost(post: Post) {
        post.original_user?.let {
            updateProfilePhoto(it)
            updateUserName(it, post)
        }

        if (shareInfo == null) return

        shareInfo.show()
        val text = application.getString(
            R.string.post_share_info, if (Constants.isVerified(post.user.verifiedStatus))
                post.user.fullName.plus(" ") else post.user.fullName
        )
        val input = SpannableString(text)
        TSUTextTokenizingHelper.clickable(application, input, post.user.fullName) {
            if (post.user.id != null) callback?.didTapOnUser(post.user.id)
        }
        if (Constants.isVerified(post.user.verifiedStatus)) {
            val imageSpan = ImageSpan(application.applicationContext, R.drawable.ic_verified_small)
            input.setSpan(imageSpan, post.user.fullName!!.length, post.user.fullName.length + 1, 0)
        }
        shareInfo.text = input
    }

    private fun updateLikeStatus(post: Post) {
        if (likeButton == null) return

        if (post.like_count == 0) {
            likeButton.setImageResource(R.drawable.ic_like)
            likeButton.imageTintList = application.getColorStateList(R.color.white)
        } else if (post.has_liked == true) {
            likeButton.setImageResource(R.drawable.ic_like_filled)
            likeButton.imageTintList = application.getColorStateList(R.color.tsu_primary)
        } else {
            likeButton.setImageResource(R.drawable.ic_like)
            likeButton.imageTintList = application.getColorStateList(R.color.white)
        }

        if (likesCount == null) return

        val value = post.like_count
        if (value <= 0) {
            likesCount.hide()
            if (commentsCount?.visibility != View.VISIBLE) {
                footerDivider.hide()
            }
            return
        }

        likesCount.show()
        footerDivider.show()
        val text = if (value == 1) {
            application.getString(R.string.like_count_one)
        } else {
            application.getString(R.string.like_count_many, value)
        }
        try {
            likesCount.text = boldify(text, value.toString())
        } catch (e: IndexOutOfBoundsException) {
            analyticsHelper.log("Problematic like text: $text. Value=$value")
            analyticsHelper.recordException(e)
        }
    }

    private fun updateShareStatus(post: Post) {
        if (shareButton == null) return

        val currentUserId = AuthenticationHelper.currentUserId
        if (post.privacy == Post.PRIVACY_EXCLUSIVE) {
            shareButton.isEnabled = false
        } else {
            shareButton.isEnabled = post.isSharable
                    && post.user.id != currentUserId?.toLong()
                    && post.original_user_id != currentUserId
        }
        shareButton.imageTintList = if (shareButton.isEnabled) {
            application.getColorStateList(R.color.white)
        } else if (post.has_shared == true && !post.is_share) {
            application.getColorStateList(R.color.shared_color)
        } else {
            application.getColorStateList(R.color.secondaryDarkGray)
        }


        if (sharesCount == null) return

        val value = post.share_count
        if (value <= 0) {
            sharesCount.hide()
            return
        }

        sharesCount.show()
        footerDivider.show()
        val text = if (value == 1) {
            application.getString(R.string.share_count_one)
        } else {
            application.getString(R.string.share_count_many, value)
        }
        try {
            sharesCount.text = boldify(text, value.toString())
        } catch (e: IndexOutOfBoundsException) {
            analyticsHelper.log("Problematic share text: $text. Value=$value")
            analyticsHelper.recordException(e)
        }


    }

    private fun updateCommentStatus(post: Post) {
        if (commentsCount == null) return

        val value = post.comment_count
        if (value <= 0) {
            commentsCount.hide()
            if (likesCount?.visibility != View.VISIBLE) {
                footerDivider.hide()
            }
            return
        }

        commentsCount.show()
        footerDivider.show()
        val text = if (value == 1) {
            application.getString(R.string.comment_count_one)
        } else {
            application.getString(R.string.comment_count_many, value)
        }
        commentsCount.text = boldify(text, value.toString())
    }

    private fun updateProfilePhoto(user: User) {
        if (userIcon == null) return

        userIcon.setImageResource(R.drawable.user)

        userProfileImageService.getProfilePicture(user.profilePictureUrl, false) {
            if (currentPost?.user?.profilePictureUrl == user.profilePictureUrl && it != null) {
                userIcon.setImageDrawable(it)
            }
        }
    }

    private fun formatUrl(post: Post): String {
        // Handle relative paths from the API_HOST
        if (post.picture_url.startsWith("/")) {
            return "${HostProvider.imageHost}${post.picture_url}"
        }

        return post.picture_url
    }

    fun formatStreamUrl(streamUrl: String): String {
        // Handle relative paths from the API_HOST
        if (streamUrl.startsWith("/")) {
            return "${HostProvider.videoHost}${streamUrl}"
        }

        if (streamUrl.startsWith("cdn")) {
            return "https://$streamUrl"
        }

        return streamUrl
    }

    private fun boldify(fullText: String?, text: String?): SpannableString? {
        if (fullText == null || text == null) return null
        val input = SpannableString(fullText)
        return TSUTextTokenizingHelper.boldify(application, text, input)
    }

    private fun tokenize(fullText: String?): SpannableString? {
        if (fullText == null) return null
        val input = SpannableString(fullText)
        return TSUTextTokenizingHelper.tokenize(application, input, {
            callback?.didTapTagUser(it)
        }, { hashtag ->
            callback?.didTapHashtag(hashtag)
        })
    }

    open fun drawableToBitmap(drawable: Drawable): Bitmap {
        var bitmap: Bitmap? = null
        if (drawable is BitmapDrawable) {
            val bitmapDrawable: BitmapDrawable = drawable as BitmapDrawable
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap()
            }
        }
        bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            ) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
        drawable.draw(canvas)
        return bitmap
    }
}