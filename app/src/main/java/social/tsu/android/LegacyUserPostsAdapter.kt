package social.tsu.android

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.SimpleExoPlayer
import social.tsu.android.adapters.viewholders.*
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.ads.GoogleAdFetcher
import social.tsu.android.network.model.PendingPost
import social.tsu.android.service.DefaultOpenGraphService
import social.tsu.android.ui.model.*
import social.tsu.android.utils.isWifiConnected
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


interface UserPostsAdapterActionCallback : CreatePostViewHolderCallback,
    LinkPostViewHolderCallback, CreatePostMiniViewHolderCallback, PendingPostViewHolderCallback


class UserVideosFeedPostsAdapter(
    application: TsuApplication,
    exoPlayer: SimpleExoPlayer,
    actionCallback: UserPostsAdapterActionCallback?,
    listener: OnBottomReachedListener?
) : LegacyUserPostsAdapter(application, exoPlayer, actionCallback, listener) {
    override fun updatePosts(newPosts: List<Post>?) {
        val filteredPosts = newPosts?.filter {
            it.has_stream && !it.is_share
        }
        if (filteredPosts != null) {
            content.clear()
            val elements = transformList(filteredPosts)
            content.addAll(elements)
        }
        notifyDataSetChanged()
    }
}

class UserPhotosFeedPostsAdapter(
    application: TsuApplication,
    exoPlayer: SimpleExoPlayer,
    actionCallback: UserPostsAdapterActionCallback?,
    listener: OnBottomReachedListener?
) : LegacyUserPostsAdapter(application, exoPlayer, actionCallback, listener) {
    override fun updatePosts(newPosts: List<Post>?) {
        val filteredPosts = newPosts?.filter {
            it.has_picture && !it.is_share
        }
        if (filteredPosts != null) {
            content.clear()
            val elements = transformList(filteredPosts)
            content.addAll(elements)
        }
        notifyDataSetChanged()
    }
}

open class LegacyUserPostsAdapter(
    private val application: TsuApplication,
    private val exoPlayer: SimpleExoPlayer,
    private val actionCallback: UserPostsAdapterActionCallback?,
    listener: OnBottomReachedListener?,
    private val nativeAdFetcher: GoogleAdFetcher? = null,
    var addPostComposeView: Boolean = true,
    private val useMiniComposePost: Boolean = false,
    private val showCommunityInTitle: Boolean = true
) : RecyclerView.Adapter<PostViewHolder>() {
    companion object {
        const val AD_STRIDE = 3
    }

    enum class ViewTypes {
        POST_CONTENT,
        POST_ADVIEW,
        POST_LINK,
        POST_CREATE,
        POST_CREATE_MINI,
        POST_VIDEO,
        POST_PENDING,
        VIEW_LOADING
    }

    var enableOldBottomLogic = false
    private lateinit var context: Context

    val content = mutableListOf<FeedContent<Any>>()
    var onBottomReachedListener: OnBottomReachedListener? = listener

    private val openGraphService = DefaultOpenGraphService()

    private var layoutManager: LinearLayoutManager? = null
    private var activeVideoPosition: Int = -1
    private var activeVideoId: Long = -1L
    private var isActiveVideoPaused: Boolean = false
    var isLoading: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    open fun updatePosts(newPosts: List<Post>?) {
        if (newPosts != null) {
            content.clear()
            val numberOfAds = newPosts.size / AD_STRIDE
            val elements = transformList(newPosts)
            if (addPostComposeView) {
                if (useMiniComposePost) {
                    content.add(0, CreatePostMiniContent())
                } else {
                    content.add(0, CreatePostContent())
                }
            }
            content.addAll(elements)

            Log.i("ADS", "posts = ${newPosts.size} ads = ${numberOfAds} ")

            val adOffset = (newPosts.size / max(1, numberOfAds)) + 1
            var index = 0
            for (idx in 0..numberOfAds) {
                index += adOffset
                val adIdx = index - 1
                if (adIdx < newPosts.size + numberOfAds) {
                    Log.d("ADS", "inserting ad at $adIdx")
                    content.add(adIdx, AdContent())
                }
            }

        }
        notifyDataSetChanged()
    }

    fun updatePendingPosts(posts: List<PendingPost>) {
        content.clear()
        content.addAll(posts.map { PendingPostContent(it) })
        notifyDataSetChanged()
    }

    fun updatePost(post: Post) {
        val itemContent = postToContent(post)
        val idx = content.indexOfFirst {
            val content = it.getContent()
            if (content is Post) {
                return@indexOfFirst content.originalPostId == post.originalPostId
            }
            false
        }
        if (idx >= 0) {
            content.removeAt(idx)
            content.add(idx, itemContent)
            notifyItemChanged(idx)
        } else {
            notifyDataSetChanged()
        }
    }

    protected fun transformList(postsList: List<Post>): List<FeedContent<Post>> {
        return postsList.map { post ->
            postToContent(post)
        }
    }

    private fun postToContent(post: Post): FeedContent<Post> {
        return if (post.has_stream) {
            VideoPostContent(post)
        } else if (!post.has_picture && (post.has_link || post.getLinks().isNotEmpty())) {
            LinkPostContent(post)
        } else {
            PostContent(post)
        }
    }

    fun addPost(post: Post) {
        if (post.is_share) return
        content.add(0, PostContent(post))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        when (viewType) {
            ViewTypes.POST_ADVIEW.ordinal -> {
                val view = inflater.inflate(R.layout.sample_ad_screen, parent, false)
                val adViewHolder = AdViewHolder(view, application, parent)
                return adViewHolder
            }
            ViewTypes.POST_CREATE.ordinal -> {
                val view = inflater.inflate(R.layout.compose_post, parent, false)
                return CreatePostViewHolder(application, actionCallback, view)
            }
            ViewTypes.POST_VIDEO.ordinal -> {
                val view = inflater.inflate(R.layout.post_video, parent, false)
                return VideoPostHolder(
                    application,
                    exoPlayer,
                    videoCallback,
                    actionCallback,
                    view,
                    showCommunityInTitle
                )
            }
            ViewTypes.VIEW_LOADING.ordinal -> {
                //loadmore spinner
                val view = inflater.inflate(R.layout.post_loading, parent, false)
                return LoadmoreViewHolder(view, application, parent)
            }
            ViewTypes.POST_LINK.ordinal -> {
                val view = inflater.inflate(R.layout.post_link, parent, false)
                return LinkPostHolder(
                    application,
                    actionCallback,
                    openGraphService,
                    view,
                    showCommunityInTitle
                )
            }
            ViewTypes.POST_CREATE_MINI.ordinal -> {
                val view = inflater.inflate(R.layout.compose_post_mini, parent, false)
                return CreatePostMiniViewHolder(application, actionCallback, view)
            }
            ViewTypes.POST_PENDING.ordinal -> {
                val view = inflater.inflate(R.layout.post_pending, parent, false)
                return PendingPostViewHolder(application, actionCallback, view)
            }
            else -> {
                val view = inflater.inflate(R.layout.post_text, parent, false)
                return PostViewHolder(application, actionCallback, view, showCommunityInTitle)
            }
        }
    }

    override fun getItemCount(): Int {
        return if (isLoading) content.size + 1 else content.size
    }

    override fun getItemViewType(position: Int): Int {
        if (isLoading && position == itemCount - 1) {
            return ViewTypes.VIEW_LOADING.ordinal
        }
        return when (content[position]) {
            is AdContent -> ViewTypes.POST_ADVIEW.ordinal
            is VideoPostContent -> ViewTypes.POST_VIDEO.ordinal
            is CreatePostContent -> ViewTypes.POST_CREATE.ordinal
            is CreatePostMiniContent -> ViewTypes.POST_CREATE_MINI.ordinal
            is LinkPostContent -> ViewTypes.POST_LINK.ordinal
            is PendingPostContent -> ViewTypes.POST_PENDING.ordinal
            else -> ViewTypes.POST_CONTENT.ordinal
        }
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        if (isLoading && position == itemCount - 1) {
            Log.d("FEED", "loadmore view - no need to bind")
            return
        }

        val item = content[position]

        Log.d("FEED", "onBind position = $position  size = ${content.size}")

        //Bottom reach event logic moved to RecyleView onScrollChangedListener
        //temporary switch to handle userfeed loadmore
        if (enableOldBottomLogic) {
            if (position >= content.size - 2) {
                Log.d("FEED", "last one???")
                val lastPost = content.findLast {
                    it is PostContent
                }?.let {
                    (it as PostContent).getContent()
                }
                if (position != 0) {
                    onBottomReachedListener?.onBottomReached(lastPost)
                }
            }
        }

        if (holder is AdViewHolder) {
            nativeAdFetcher?.let {
                val ad = nativeAdFetcher.getAd(position)
                content[position] = ad
                holder.bind(ad)
                return
            }
            val adContent = item as AdContent
            holder.bind(adContent)
            return
        }

        if (holder is CreatePostViewHolder) {
            holder.bind()
            return
        }

        if (holder is CreatePostMiniViewHolder) {
            holder.bind()
            return
        }

        if (holder is PendingPostViewHolder) {
            holder.bind((content[position] as PendingPostContent).getContent())
        }

        when (item) {
            is LinkPostContent -> holder.bind(item.getContent())
            is PostContent -> {
                holder.bind(item.getContent())
            }
            is VideoPostContent -> {
                val video = item.getContent()
                holder.bind(video)
                if (holder is VideoPostHolder) {
                    if (video.id == this.activeVideoId) {
                        if (isAutoplayEnabled) {
                            if (item.getContent().privacy == Post.PRIVACY_EXCLUSIVE) {
                                val supportPostIds = ArrayList<String>()
                                holder.sharedPrefManager.getSupportPostId()?.let {
                                    supportPostIds.addAll(it.split(","))
                                }
                                if (supportPostIds.contains(item.getContent().id.toString())) {
                                    isActiveVideoPaused = false
                                    holder.startVideo()
                                }
                            } else {
                                isActiveVideoPaused = false
                                holder.startVideo()
                            }
                        }
                    } else {
                        Log.d("VideoHolder", "reset VideoPostContent ${video.id}")
                        holder.reset()
                    }
                }
            }
        }
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        if (holder is AdViewHolder) {
            nativeAdFetcher?.onAdRecycled(holder.adapterPosition)
        }
    }

    fun removePost(post: Post) {
        removePost(post.id)
    }

    fun removePost(postId: Long) {
        val idx = content.indexOfFirst {
            val itemContent = it.getContent()
            if (itemContent is Post) {
                return@indexOfFirst itemContent.originalPostId == postId.toInt()
            }
            false
        }
        if (idx >= 0) {
            content.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context

        val layoutManager = recyclerView.layoutManager
        if (layoutManager is LinearLayoutManager) {
            this.layoutManager = layoutManager
        }

        recyclerView.addOnScrollListener(onScrollListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        openGraphService.onDestroy()

        exoPlayer.stop(true)
        exoPlayer.release()

        recyclerView.removeOnScrollListener(onScrollListener)

        this.layoutManager = null
    }

    fun onScrollStart() {
        pauseActiveVideo()
    }

    fun onScrollEnd() {
        determineViewToPlay()
    }

    private fun determineViewToPlay() {
        layoutManager?.let {
            val visiblePosition = it.findFirstVisibleItemPosition()
            if (visiblePosition < 0) return

            val lastVisible = it.findLastVisibleItemPosition()

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
//                    Log.d("VideoHolder", "visible $position visibleHeight: $visibleHeight, distance to screen center: $distance")
                        if (lastDistance > distance && percentage >= 0.33f) {
                            lastDistance = distance
                            mostVisiblePosition = position
                            content.getOrNull(position)?.getContent()?.let { item ->
                                mostVisibleId = if (item is Post) item.id else -1
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
        if (getItemViewType(position) != ViewTypes.POST_VIDEO.ordinal) {
            return null
        }

        val itemView = layoutManager?.findViewByPosition(position)
        return itemView?.findViewWithTag("player_view")
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

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> onScrollEnd()
                RecyclerView.SCROLL_STATE_DRAGGING -> onScrollStart()
            }
        }
    }

    private val videoCallback = object : VideoPostHolder.Callback {
        override fun onStartPlaying(videoId: Long, position: Int) {
            Log.d("VideoHolder", "videoCallback $position id - $videoId")
            startPlaying(position, videoId, false)
        }
    }

    fun getPositionById(id: Long): Int {
        for (i in 0 until content.size) {
            val item = content[i]
            if (item is PostContent) {
                if (item.getContent().id == id) {
                    return i
                }
            }
        }
        return 0
    }

    fun getVideoPositionById(id: Long): Int {
        for (i in 0 until content.size) {
            val item = content[i]
            if (item is VideoPostContent) {
                if (item.getContent().id == id) {
                    return i
                }
            }
        }
        return 0
    }

    fun removePost(post: PendingPost) {
        val idx = content.indexOfFirst {
            val itemContent = it.getContent()
            if (itemContent is PendingPost) {
                return@indexOfFirst itemContent.id == post.id
            }
            false
        }
        if (idx >= 0) {
            content.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }
}
