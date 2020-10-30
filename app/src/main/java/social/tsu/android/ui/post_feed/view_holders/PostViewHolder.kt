package social.tsu.android.ui.post_feed.view_holders

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
import androidx.constraintlayout.widget.Guideline
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.ablanco.zoomy.Zoomy
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.button.MaterialButton
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.post_item_header.view.*
import kotlinx.android.synthetic.main.post_video.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.Constants
import social.tsu.android.helper.DateHelper
import social.tsu.android.helper.TSUTextTokenizingHelper
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.model.User
import social.tsu.android.service.IS_TEXT_COPY_ENABLED
import social.tsu.android.service.ImageFetchHandler
import social.tsu.android.service.SharedPrefManager
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.model.AdContent
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.post_feed.UserPostsAdapter
import social.tsu.android.ui.util.BaseViewHolder
import social.tsu.android.ui.util.TextCopyUtils
import social.tsu.android.utils.applyTo
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import javax.inject.Inject


@SuppressLint("ClickableViewAccessibility")
open class PostViewHolder(
    private val application: TsuApplication,
    private val callback: ViewHolderActions?,
    itemView: View,
    private val showCommunityInTitle: Boolean = false,
    dividerColor: Int = 0
) : BaseViewHolder(itemView), CoroutineScope by MainScope() {

    private val context = itemView.context

    private val adViewHolder = AdViewHolder(itemView)
    private val adLayout = itemView.ad_header

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    var currentPost: Post? = null
        protected set

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
    private val postDivider: View? = itemView.findViewById(R.id.post_divider)
    private val pictureProgress: ProgressBar? = itemView.findViewById(R.id.picture_progress)
    val supportButton: MaterialButton? = itemView.findViewById(R.id.supportButton)

    private val likesCount: TextView? = itemView.findViewById(R.id.post_likes_count)
    private val commentsCount: TextView? = itemView.findViewById(R.id.post_comments_count)
    private val sharesCount: TextView? = itemView.findViewById(R.id.post_shares_count)
    private val supportsCount: TextView? = itemView.findViewById(R.id.post_supports_count)
    private val commentsCountGuideline: Guideline? = itemView.findViewById(R.id.post_comments_count_guideline)
    private val sharesCountGuideline: Guideline? = itemView.findViewById(R.id.post_shares_count_guideline)


    private val onClickListener = object : View.OnClickListener {

        override fun onClick(view: View?) {
            val post = this@PostViewHolder.currentPost ?: return
            when (view?.id) {
                R.id.shareButton -> onShareClick(post)
                R.id.commentButton -> callback?.didTapCommentOn(post)
                R.id.post_comments_count -> callback?.didTapCommentOn(post)
                R.id.likeButton -> onLikeClick(post)
                R.id.post_likes_count -> callback?.didTapShowLikes(post)
                R.id.post_supports_count -> callback?.didTapShowSupports(post)
                R.id.post_more_icon -> callback?.didTapMoreOptions(post)
                R.id.post_item_user_icon -> onUserClick(post)
                R.id.post_item_username -> onUserClick(post)
                R.id.supportButton -> {
                    callback?.didTapSupportButton(post)
                    currentPost = null
                }
            }
        }
    }

    init {
        application.appComponent.viewHolderComponent().create().inject(this)
        onClickListener.applyTo(
            shareButton,
            commentButton,
            likeButton,
            moreIcon,
            userIcon,
            likesCount,
            commentsCount,
            supportsCount,
            supportButton
        )
        shareInfo?.movementMethod = LinkMovementMethod.getInstance()
        username?.movementMethod = LinkMovementMethod.getInstance()
        if (dividerColor != 0) {
            postDivider?.setBackgroundColor(dividerColor)
        }
    }

    open fun reset() {
        postContent?.text = ""
        postDate?.text = ""
        footerDivider.hide()
        commentsCount.hide()
        likesCount.hide()
        sharesCount.hide()
    }


    override fun <T> bind(item: T) {
        if (item == null) return

        val post = item as Post
        if (this.currentPost?.contentEquals(post) == true) return

        pictureProgress?.visibility = View.GONE

        if (this.currentPost?.id == post.id) {
            //do nothing: view holder being updated
        } else if (adapterPosition > 0 && adapterPosition % UserPostsAdapter.AD_STRIDE == 0) {
            val adContent = callback?.getAdContent(adapterPosition)
            if (adContent != null) {
                adLayout?.show()
                adViewHolder.bind(adContent)
            } else {
                adLayout.hide()
            }
        } else {
            adLayout?.hide()
        }

        val oldPost = currentPost
        this.currentPost = post

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
        updateSupportsStatus(post)
        postDate?.text = DateHelper.prettyDate(context, post.created_at)
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
            footerDivider?.visibility = View.INVISIBLE
            likesCount?.visibility = View.GONE
            commentsCount?.visibility = View.GONE
            sharesCount?.visibility = View.GONE
            supportsCount?.visibility = View.GONE
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
                pictureProgress?.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(formatUrl(post)).addListener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            pictureProgress?.visibility = View.GONE
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
                                sharedPrefManager.getSupportPostId()?.let { ids ->
                                    supportPostIds.addAll(ids.split(","))
                                }
                                resource?.let {
                                    if (post.privacy == Post.PRIVACY_EXCLUSIVE
                                        && post.user_id != AuthenticationHelper.currentUserId && supportPostIds.contains(
                                            post.id.toString()
                                        ).not()
                                    ) {
                                        Blurry.with(context).radius(25)
                                            .from(drawableToBitmap(resource)).into(postImage)
                                        pictureProgress?.visibility = View.GONE
                                        itemView.tvSupport?.visibility = View.VISIBLE
                                        return true
                                    } else {
                                        pictureProgress?.visibility = View.GONE
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
            delay(500)
            likeButton?.isEnabled = true
        }
    }

    private fun onShareClick(post: Post) {
        // To avoid double click
        shareButton?.visibility = View.INVISIBLE
        shareProgress.show()
        callback?.didTapOnShare(post)?.observe(callback.lifecycleOwner, Observer {
            when (it) {
                is Data.Loading -> {
                    shareProgress.show()
                    shareButton?.visibility = View.INVISIBLE
                }
                else -> {
                    shareButton.show()
                    shareProgress.hide()
                }
            }
        })
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
            context.getString(R.string.post_user_updated_profile, showedName)
        } else {
            showedName
        }
        val input = SpannableString(text)
        if (user.id != null) {
            TSUTextTokenizingHelper.clickable(itemView.context, input, userFullName) {
                callback?.didTapOnUser(user.id)
            }
        }
        if (post.groupId != null) {
            TSUTextTokenizingHelper.clickable(itemView.context, input, post.groupName) {
                callback?.didTapOnGroup(post.groupId)
            }
        }
        TSUTextTokenizingHelper.boldifySymbol(context, input, '▸')

        if (input.length == userFullName!!.length){
          //  input = SpannableString(input.toString().plus(" "))
            if (Constants.isVerified(user.verifiedStatus)) {
                val imageSpan = ImageSpan(application.applicationContext, R.drawable.ic_verified_extra_small)
                input.setSpan(imageSpan, input.length - 1, input.length, 0)
            }
        }else{
            if (Constants.isVerified(user.verifiedStatus)) {
                val imageSpan = ImageSpan(application.applicationContext, R.drawable.ic_verified_extra_small)
                input.setSpan(imageSpan, userFullName.length, userFullName.length + 1, 0)
            }
        }

        username.text = input
        if (IS_TEXT_COPY_ENABLED) {
            postContent?.setTextIsSelectable(true)
            TextCopyUtils.textGestureDetectorCompat(context, postContent)
        }

    }
    
    private fun updateSupportsStatus(post: Post) {
        if (supportsCount == null) return
        val currentUserId = AuthenticationHelper.currentUserId
        if ((post.user_id == currentUserId && !post.is_share) || post.original_user_id == currentUserId) {
            commentsCountGuideline?.setGuidelinePercent(0.35f)
            sharesCountGuideline?.setGuidelinePercent(0.75f)

            val value = post.supporters_count
            if (value > 0) {
                supportsCount.show()
                footerDivider.show()
                supportsCount.text = if (value == 1) {
                    boldify(context.getString(R.string.support_count_one), "1")
                } else {
                    boldify(
                        context.getString(R.string.support_count_many, value),
                        value.toString()
                    )
                }
            } else {
                supportsCount.hide()
            }
        } else {
            supportsCount.hide()
            supportsCount.text = null
            commentsCountGuideline?.setGuidelinePercent(0.5f)
            sharesCountGuideline?.setGuidelinePercent(1f)
        }
    }

    private fun updateForSharedPost(post: Post) {
        post.original_user?.let {
            updateProfilePhoto(it)
            updateUserName(it, post)
        }

        if (shareInfo == null) return

        shareInfo.show()
        val text = context.getString(R.string.post_share_info, if (Constants.isVerified(post.user.verifiedStatus))
            post.user.fullName.plus(" ") else post.user.fullName)
        val input = SpannableString(text)
        TSUTextTokenizingHelper.clickable(itemView.context, input, post.user.fullName) {
            if (post.user.id != null) callback?.didTapOnUser(post.user.id)
        }
        if (Constants.isVerified(post.user.verifiedStatus)) {
            val imageSpan = ImageSpan(application.applicationContext, R.drawable.ic_verified_extra_small)
            input.setSpan(imageSpan, post.user.fullName!!.length, post.user.fullName.length + 1, 0)
        }
        shareInfo.text = input
    }

    fun bindSupportBtnAvailability(isInterstitialAdLoaded: Boolean) {
        val post = this.currentPost
        val currentUserId = AuthenticationHelper.currentUserId
        if (post?.user_id == currentUserId || post?.original_user_id == currentUserId) {
            supportButton?.hide()
            return
        }

        if (isInterstitialAdLoaded) {
            supportButton?.show()
        } else {
            supportButton?.hide()
        }
    }

    private fun updateLikeStatus(post: Post) {
        if (likeButton == null) return

        if (post.like_count == 0) {
            likeButton.setImageResource(R.drawable.ic_like)
            likeButton.imageTintList = context.getColorStateList(R.color.white)
        } else if (post.has_liked == true) {
            likeButton.setImageResource(R.drawable.ic_like_filled)
            likeButton.imageTintList = context.getColorStateList(R.color.tsu_primary)
        } else {
            likeButton.setImageResource(R.drawable.ic_like)
            likeButton.imageTintList = context.getColorStateList(R.color.white)
        }

        if (likesCount == null) return

        val value = post.like_count
        if (value <= 0) {
            likesCount.hide()
            if (commentsCount?.visibility == View.GONE) {
                footerDivider.hide()
            }
            return
        }

        likesCount.show()
        footerDivider.show()
        val text = if (value == 1) {
            context.getString(R.string.like_count_one)
        } else {
            context.getString(R.string.like_count_many, value)
        }
        likesCount.text = boldify(text, value.toString())
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
            context.getColorStateList(R.color.white)
        } else {
            context.getColorStateList(R.color.secondaryDarkGray)
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
            context.getString(R.string.share_count_one)
        } else {
            context.getString(R.string.share_count_many, value)
        }
        sharesCount.text = boldify(text, value.toString())
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
            context.getString(R.string.comment_count_one)
        } else {
            context.getString(R.string.comment_count_many, value)
        }
        commentsCount.text = boldify(text, value.toString())
    }

    private fun updateProfilePhoto(user: User) {
        if (userIcon == null) return

        userIcon.setImageResource(R.drawable.user)

        callback?.getProfilePicture(user.profilePictureUrl, false) {
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
        return TSUTextTokenizingHelper.boldify(context, text, input)
    }

    private fun tokenize(fullText: String?): SpannableString? {
        if (fullText == null) return null
        val input = SpannableString(fullText)
        return TSUTextTokenizingHelper.tokenize(context, input, {
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

    interface ViewHolderActions {
        val lifecycleOwner: LifecycleOwner
        fun didTapLikeOn(post: Post)
        fun didTapCommentOn(post: Post)
        fun didTapShowLikes(post: Post)
        fun didTapShowSupports(post: Post)
        fun didTapOnUser(userId: Long)
        fun didTapOnGroup(groupId: Int)
        fun didTapOnShare(post: Post): LiveData<Data<Boolean>>?
        fun didTapMoreOptions(post: Post)
        fun didTapHashtag(hashtag: String)
        fun getProfilePicture(
            key: String?,
            ignoringCache: Boolean,
            handler: ImageFetchHandler?
        )
        fun didTapTagUser(userTag:String)
        fun getAdContent(adapterPosition: Int): AdContent?
        fun didTapSupportButton(post: Post)
    }
}