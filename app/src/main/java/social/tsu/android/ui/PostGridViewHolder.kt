package social.tsu.android.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import jp.wasabeef.blurry.Blurry
import social.tsu.android.R
import social.tsu.android.adapters.formatStreamUrl
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.utils.hide
import social.tsu.android.utils.show


interface PostGridActionCallback {
    fun onPostClicked(post: Post, position: Int)
}

abstract class PostGridViewHolder(
    itemView: View,
    protected val actionCallback: PostGridActionCallback?
) : RecyclerView.ViewHolder(itemView) {

    protected var item: Post? = null
    var getSupportPostId: String = ""

    init {
        itemView.setOnClickListener {
            item?.let {
                actionCallback?.onPostClicked(it, adapterPosition)
            }
        }
    }

    fun bind(post: Post) {
        item = post
        bindItem(post)
    }

    protected abstract fun bindItem(post: Post)

}

class UserPhotoHolder(
    itemView: View,
    actionCallback: PostGridActionCallback?
) : PostGridViewHolder(itemView, actionCallback) {

    companion object {
        fun create(
            parent: ViewGroup,
            actionCallback: PostGridActionCallback?
        ): UserPhotoHolder {
            val context = parent.context
            val view = LayoutInflater.from(context).inflate(
                R.layout.user_photo_item,
                parent, false
            )
            return UserPhotoHolder(view, actionCallback)
        }
    }

    val photo: ImageView = itemView.findViewById(R.id.photo)
    val photoContainer: ConstraintLayout = itemView.findViewById(R.id.photo_container)

    override fun bindItem(post: Post) {
        Glide.with(itemView.context)
            .load(post.picture_url)
            .fallback(R.drawable.ic_photo_unavailable)
            .thumbnail(0.25f)
            .addListener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    val supportPostIds = ArrayList<String>()
                    supportPostIds.addAll(getSupportPostId.split(","))
                    resource?.let {
                        if (post.privacy == Post.PRIVACY_EXCLUSIVE
                            && post.user_id != AuthenticationHelper.currentUserId && supportPostIds.contains(
                                post.id.toString()
                            ).not()
                        ) {
                            Blurry.with(itemView.context).radius(25)
                                .from(drawableToBitmap(resource)).into(photo)
                        } else {
                            photo.setImageBitmap(drawableToBitmap(resource))
                        }
                    }
                    return true
                }
            })
            .into(photo)
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
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

class UserVideoHolder(
    itemView: View,
    actionCallback: PostGridActionCallback?
) : PostGridViewHolder(itemView, actionCallback) {

    companion object {
        fun create(
            parent: ViewGroup,
            actionCallback: PostGridActionCallback?
        ): UserVideoHolder {
            val context = parent.context
            val view = LayoutInflater.from(context).inflate(
                R.layout.user_photo_item,
                parent, false
            )
            return UserVideoHolder(view, actionCallback)
        }
    }

    val photo: ImageView = itemView.findViewById(R.id.photo)
    val photoBackIcon: ImageView = itemView.findViewById(R.id.photo_back_icon)
    val photoContainer: ConstraintLayout = itemView.findViewById(R.id.photo_container)

    init {
        photoBackIcon.setImageResource(R.drawable.ic_videocam)
    }

    override fun bindItem(post: Post) {
        post.stream?.thumbnail?.let { thumbnail ->
            Glide.with(itemView.context)
                .load(formatStreamUrl(thumbnail))
                .fallback(R.drawable.ic_photo_unavailable)
                .thumbnail(0.25f)
                .into(photo)
        }
    }
}

class UserDiscoveryVideoHolder(
    itemView: View,
    val exoPlayer: SimpleExoPlayer,
    actionCallback: PostGridActionCallback?
) : PostGridViewHolder(itemView, actionCallback) {

    companion object {
        fun create(
            parent: ViewGroup,
            exoPlayer: SimpleExoPlayer,
            actionCallback: PostGridActionCallback?
        ): UserDiscoveryVideoHolder {
            val context = parent.context
            val view = LayoutInflater.from(context).inflate(
                R.layout.discovery_video_item,
                parent, false
            )
            return UserDiscoveryVideoHolder(view, exoPlayer, actionCallback)
        }
    }

    private val playerView = itemView.findViewById<PlayerView>(R.id.player_view)
    val photo: ImageView = itemView.findViewById(R.id.photo)
    val photoBackIcon: ImageView = itemView.findViewById(R.id.photo_back_icon)
    val photoContainer: ConstraintLayout = itemView.findViewById(R.id.photo_container)
    private val volumeToggle = playerView.findViewById<ImageButton>(R.id.control_mute)
    private val playerShutter: ImageView? = playerView.findViewById(R.id.exo_shutter)
    private val aspectRatioLayout: AspectRatioFrameLayout? = playerView.findViewById(
        R.id.exo_content_frame
    )
    private val playerStartBtn = itemView.findViewById<View>(R.id.player_start_btn)

    private var heldPost: Post? = null

    init {
        photoBackIcon.setImageResource(R.drawable.ic_videocam)
    }

    private val dataSourceFactory = DefaultDataSourceFactory(
        itemView.context,
        Util.getUserAgent(itemView.context, "tsu-android")
    )

    fun reset() {
        Log.d("VideoHolder", "reset $adapterPosition id ${heldPost?.id}")
        playerView.player = null
    }

    override fun bindItem(post: Post) {
        post.stream?.thumbnail?.let { thumbnail ->
            Glide.with(itemView.context)
                .load(formatStreamUrl(thumbnail))
                .fallback(R.drawable.ic_photo_unavailable)
                .into(photo)
        }

        heldPost = post
    }

    fun startVideo() {
        heldPost?.stream?.let { stream ->
            val videoUrl = Uri.parse(formatStreamUrl(stream.url))
            if (playerView.player == null || exoPlayer.currentTag != videoUrl) {
                exoPlayer.playWhenReady = false
                exoPlayer.stop(true)

                playerView.player = exoPlayer

                volumeToggle.hide()
                playerShutter.hide()

                stream.sizes.lastOrNull()?.let { size ->
                    aspectRatioLayout?.setAspectRatio(size.width.toFloat() / size.height.toFloat())
                }

                Log.d(
                    "VideoHolder",
                    "Start video stream on $adapterPosition of post ${heldPost?.id}"
                )
                val videoSource = HlsMediaSource.Factory(dataSourceFactory)
                    .setTag(videoUrl)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(videoUrl)

                //volumeToggle.setImageResource(R.drawable.ic_mute_on)

                exoPlayer.volume = 0f
                exoPlayer.prepare(videoSource)
                exoPlayer.playWhenReady = true
            }
        }
    }
}

class HashtagResultViewHolder(
    itemView: View,
    actionCallback: PostGridActionCallback?
) : PostGridViewHolder(itemView, actionCallback) {

    companion object {

        fun create(
            parent: ViewGroup,
            actionCallback: PostGridActionCallback?
        ): HashtagResultViewHolder {
            val context = parent.context
            val view = LayoutInflater.from(context).inflate(
                R.layout.text_post_grid_item,
                parent, false
            )
            return HashtagResultViewHolder(view, actionCallback)
        }
    }

    private val text: TextView = itemView.findViewById(R.id.hashtag_preview)

    override fun bindItem(post: Post) {
        text.text = post.content
    }

    fun setBackground(@ColorRes colorRes: Int) {
        itemView.setBackgroundResource(colorRes)
    }

}