package social.tsu.android.adapters.viewholders

import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.post_video.view.*
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.service.SharedPrefManager
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import javax.inject.Inject

class VideoPostHolder(
    application: TsuApplication,
    private val player: SimpleExoPlayer,
    private val videoCallback: Callback,
    callback: PostViewHolderCallback?,
    itemView: View,
    showCommunityInTitle: Boolean
) : PostViewHolder(application, callback, itemView, showCommunityInTitle) {

    interface Callback {
        fun onStartPlaying(videoId: Long, position: Int)
    }

    private val playerView = itemView.findViewById<PlayerView>(R.id.player_view)
    private val volumeToggle = playerView.findViewById<ImageButton>(R.id.control_mute)
    private val playerShutter: ImageView? = playerView.findViewById(R.id.exo_shutter)
    private val aspectRatioLayout: AspectRatioFrameLayout? = playerView.findViewById(
        R.id.exo_content_frame
    )

    private val dataSourceFactory = DefaultDataSourceFactory(
        itemView.context,
        Util.getUserAgent(itemView.context, "tsu-android")
    )

    private val playbackListener = object : Player.EventListener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playerView?.let {
                it.keepScreenOn = isPlaying
            }
        }
    }

    private val playerStartBtn = itemView.findViewById<View>(R.id.player_start_btn)
    private val playerThumbnail = itemView.findViewById<ImageView>(R.id.player_thumbnail)

    init {
        playerThumbnail.setOnClickListener(::playCurrentSource)
        playerStartBtn.setOnClickListener(::playCurrentSource)
        playerView.setOnClickListener(::togglePlaying)
        volumeToggle.setOnClickListener(::toggleVolume)
    }

    override fun reset() {
        Log.d("VideoHolder", "reset $adapterPosition id ${currPost?.id}")
        player.removeListener(playbackListener)
        playerStartBtn.show()
        playerThumbnail.show()
        playerShutter?.setImageDrawable(null)
        playerView.player = null
    }

    override fun bind(post: Post) {
        val oldPost = this.currPost

        super.bind(post)

        val stream = post.stream ?: run {
            reset()
            return
        }
        if (oldPost?.stream?.url == stream.url) return

        stream.sizes.lastOrNull()?.let { size ->
            aspectRatioLayout?.setAspectRatio(size.width.toFloat() / size.height.toFloat())
        }

        Glide.with(playerThumbnail)
            .load(formatStreamUrl(stream.thumbnail))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    playerShutter?.setImageDrawable(null)
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
                                    .from(drawableToBitmap(resource)).into(playerShutter)
                                Blurry.with(itemView.context).radius(25)
                                    .from(drawableToBitmap(resource)).into(playerThumbnail)
                                itemView.tvSupport?.visibility = View.VISIBLE
                                playerThumbnail.isEnabled = false
                                playerStartBtn.isEnabled = false
                            } else {
                                playerShutter?.setImageBitmap(drawableToBitmap(resource))
                                playerThumbnail?.setImageBitmap(drawableToBitmap(resource))
                                itemView.tvSupport?.visibility = View.GONE
                                playerThumbnail.isEnabled = true
                                playerStartBtn.isEnabled = true

                            }
                        }
                    }
                    return true
                }

            })
            .into(playerThumbnail)
    }

    private fun toggleVolume(view: View) {
        player.volume = if (player.volume == 0.0f) {
            volumeToggle.setImageResource(R.drawable.ic_mute_off)
            1.0f
        } else {
            volumeToggle.setImageResource(R.drawable.ic_mute_on)
            0.0f
        }
    }

    private fun togglePlaying(view: View) {
        player.playWhenReady = !player.isPlaying
        player.playbackState
    }

    fun startVideo() {
        playerShutter?.setImageDrawable(playerThumbnail.drawable)
        playerStartBtn.hide()
        playerThumbnail.hide()
        if (currPost?.has_stream == true) {
            currPost?.stream?.let { stream ->
                val videoUrl = Uri.parse(formatStreamUrl(stream.url))
                if (playerView.player == null || player.currentTag != videoUrl) {
                    player.playWhenReady = false
                    player.stop(true)

                    playerView.player = player

                    stream.sizes.lastOrNull()?.let { size ->
                        aspectRatioLayout?.setAspectRatio(size.width.toFloat() / size.height.toFloat())
                    }

                    Log.d("VideoHolder", "Start video stream on $adapterPosition of post ${currPost?.id}")
                    val videoSource = HlsMediaSource.Factory(dataSourceFactory)
                        .setTag(videoUrl)
                        .setAllowChunklessPreparation(true)
                        .createMediaSource(videoUrl)

                    volumeToggle.setImageResource(R.drawable.ic_mute_on)

                    player.volume = 0f
                    player.prepare(videoSource)
                    player.addListener(playbackListener)
                    player.playWhenReady = true
                }
            }
        }
    }

    private fun playCurrentSource(view: View) {
        videoCallback.onStartPlaying(currPost?.id ?: -1, adapterPosition)
        startVideo()
    }

}
