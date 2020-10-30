package social.tsu.android.adapters

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.SimpleExoPlayer
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.ui.*
import social.tsu.android.ui.UserVideoHolder
import social.tsu.android.ui.model.FeedContent
import social.tsu.android.ui.model.PostContent
import social.tsu.android.ui.model.VideoPostContent
import social.tsu.android.ui.post_feed.view_holders.VideoPostHolder
import social.tsu.android.ui.recyclerview.SpannedGridLayoutManager
import social.tsu.android.utils.isWifiConnected
import kotlin.math.abs
import kotlin.math.min

private const val TYPE_POST_HASH_TAG = 0
private const val TYPE_POST_PHOTO = 1
private const val TYPE_POST_VIDEO = 2

private val tsuColors = intArrayOf(
    R.color.tsu_blue,
    R.color.tsu_green,
    R.color.tsu_yellow,
    R.color.tsu_red
)

open class DiscoveryGridAdapter(
    private val exoPlayer: SimpleExoPlayer,
    private val actionCallback: PostGridActionCallback?)
    : RecyclerView.Adapter<PostGridViewHolder>() {

    private lateinit var context: Context
    private val hashtagBackgroundMap = linkedMapOf<Int, Int>()

    val content = mutableListOf<FeedContent<Any>>()

//    private var layoutManager: LinearLayoutManager? = null
    var isLoading: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    open fun updatePosts(newPosts: List<Post>?) {
        newPosts?.let {
            content.clear()
            val elements = newPosts.map { PostContent(it) }
            content.addAll(elements)

            content.size
            hashtagBackgroundMap.clear()
            elements.forEachIndexed { index, post ->
                if (!post.getContent().has_stream && !post.getContent().has_picture) {
                    var nextIdx = (hashtagBackgroundMap.values.lastOrNull() ?: -1) + 1
                    if (nextIdx >= tsuColors.size) {
                        nextIdx = 0
                    }
                    hashtagBackgroundMap[index] = nextIdx
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostGridViewHolder {
        return when (viewType) {
            TYPE_POST_PHOTO -> UserPhotoHolder.create(parent, actionCallback)
            TYPE_POST_VIDEO -> UserDiscoveryVideoHolder.create(parent, exoPlayer, actionCallback)
            else -> HashtagResultViewHolder.create(parent, actionCallback)
        }
    }

    override fun getItemCount(): Int {
        return content.size
    }

    override fun onBindViewHolder(holder: PostGridViewHolder, position: Int) {
        content.getOrNull(position)?.let { postContent ->
            val post = postContent.getContent() as Post
            when(holder) {
                is UserDiscoveryVideoHolder -> {
                    if (post.id == this.activeVideoId) {
                        if (isAutoplayEnabled) {
                            isActiveVideoPaused = false
                            holder.startVideo()
                        }
                    } else {
                        Log.d("VideoHolder", "reset VideoPostContent ${post.id}")
                        holder.reset()
                    }
                }
                is HashtagResultViewHolder -> {
                    holder.setBackground(tsuColors[hashtagBackgroundMap[position] ?: 0])
                }
            }

            holder.bind(post)
        }
    }

    override fun getItemViewType(position: Int): Int {
        content.getOrNull(position)?.let { post ->
            if ((post as PostContent).getContent().has_stream && post.getContent().stream?.thumbnail != null) {
                return TYPE_POST_VIDEO
            }
            if (post.getContent().has_picture && post.getContent().picture_url != "") {
                return TYPE_POST_PHOTO
            }
        }
        return TYPE_POST_HASH_TAG
    }

    //Video routines
    private var layoutManager: SpannedGridLayoutManager? = null
    private var activeVideoPosition: Int = -1
    private var activeVideoId: Long = -1L
    private var isActiveVideoPaused: Boolean = false


    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> onScrollEnd()
                RecyclerView.SCROLL_STATE_DRAGGING -> onScrollStart()
            }
        }
    }

    fun onScrollStart() {
        pauseActiveVideo()
    }

    fun onScrollEnd() {
        determineViewToPlay()
    }

    fun playVideo(position: Int, postId: Long) {
        startPlaying(position, postId, true)
    }

    private fun startPlaying(position: Int, videoId: Long, autoPlay: Boolean) {
        Log.d("VideoHolder", "startPlaying $position id - $videoId")
        if (this.activeVideoId != videoId) {
            val oldId = activeVideoId

            val oldPosition = activeVideoPosition
            this.activeVideoPosition = position
            this.activeVideoId = videoId

            if (oldPosition >= 0) {
                Log.d("VideoHolder", "Stop video $oldPosition id - $oldId")
                notifyItemChanged(oldPosition)
            }
            if (position >= 0 && autoPlay) {
                Log.d("VideoHolder", "Run video $position id - $videoId")
                notifyItemChanged(position)
            }
        }
    }

    private fun getPlayableView(position: Int): View? {
        if (getItemViewType(position) != TYPE_POST_VIDEO) {
            return null
        }

        val itemView = layoutManager?.findViewByPosition(position)
        layoutManager?.spanSizeLookup?.let { spanLookup ->
            if(spanLookup.getSpanSize(position).width == 2){
                return itemView?.findViewWithTag("player_view")
            }
        }

        return null
    }

    private fun resumeActiveVideo() {
        if (activeVideoId >= 0 && isActiveVideoPaused && !exoPlayer.isPlaying) {
            isActiveVideoPaused = false
            exoPlayer.playWhenReady = true
            exoPlayer.playbackState
            Log.d("VideoHolder", "Video $activeVideoId resumed")
        }
    }

    private fun pauseActiveVideo() {
        if (exoPlayer.isPlaying) {
            isActiveVideoPaused = true
            exoPlayer.playWhenReady = false
            exoPlayer.playbackState
            Log.d("VideoHolder", "Video $activeVideoId paused")
        }
    }

    private val isAutoplayEnabled: Boolean
        get() {
            return context.isWifiConnected()
        }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context

        val layoutManager = recyclerView.layoutManager
        if (layoutManager is SpannedGridLayoutManager) {
            this.layoutManager = layoutManager
        }

        recyclerView.addOnScrollListener(onScrollListener)
    }

    private fun determineViewToPlay() {
        layoutManager?.let {
            val visiblePosition = it.firstVisiblePosition
            if (visiblePosition  < 0) return

            val lastVisible = it.lastVisiblePosition

            var mostVisiblePosition = -1
            var mostVisibleId = -1L

            val screenHeight = context.resources.displayMetrics.heightPixels
            val screenCenter = screenHeight / 2
            var lastDistance = Int.MAX_VALUE

            for (position in visiblePosition..lastVisible) {
                getPlayableView(position)?.let { view ->
                    val viewPosition = IntArray(2)
                    view.getLocationOnScreen(viewPosition)
                    val viewY = viewPosition[1]
                    val viewBotY = viewY + view.height
                    val visibleHeight = if (viewY < 0) {
                        if (viewBotY > screenHeight) screenHeight else viewBotY
                    } else if (viewBotY > screenHeight) {
                        screenHeight - viewY
                    } else {
                        viewBotY - viewY
                    }
                    if (visibleHeight > 0) {
                        val percentage = visibleHeight / view.height.toFloat()
                        val distance = min(
                            abs(screenCenter - viewY),
                            abs(screenCenter - viewBotY)
                        )
                        Log.d("VideoHolder", "visible $position visibleHeight: $visibleHeight, distance to screen center: $distance")
                        if (lastDistance > distance && percentage >= 0.33f) {
                            lastDistance = distance
                            mostVisiblePosition = position
                            content.getOrNull(position)?.let { item ->
                                mostVisibleId = (item as PostContent).getContent().id
                            }
                        }
                    }
                }
            }

            if (mostVisibleId >= 0) {
                if (activeVideoId != mostVisibleId) {
                    Log.d(
                        "VideoHolder",
                        "determineViewToPlay to play $mostVisibleId on $mostVisiblePosition"
                    )
                    startPlaying(mostVisiblePosition, mostVisibleId, true)
                } else {
                    Log.d(
                        "VideoHolder",
                        "determineViewToPlay resume $mostVisibleId on $mostVisiblePosition"
                    )
                    resumeActiveVideo()
                }
                return
            }

            // If any video isn't visible, pause it
            if (activeVideoPosition < visiblePosition || activeVideoPosition > lastVisible) {
                Log.d("VideoHolder", "determineViewToPlay stop video")
                startPlaying(-1, -1, false)
            }
        }
    }

    private val videoCallback = object : VideoPostHolder.Callback {
        override fun onStartPlaying(videoId: Long, position: Int) {
            Log.d("VideoHolder", "videoCallback $position id - $videoId")
            startPlaying(position, videoId, false)
        }
    }

}
