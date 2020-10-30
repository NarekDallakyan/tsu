package social.tsu.android.ui.post_feed

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.paging.AsyncPagedListDiffer
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.*
import com.google.android.exoplayer2.SimpleExoPlayer
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.ads.GoogleAdFetcher
import social.tsu.android.service.DefaultOpenGraphService
import social.tsu.android.ui.LoadStateViewHolder
import social.tsu.android.ui.model.*
import social.tsu.android.ui.post_feed.view_holders.*
import social.tsu.android.ui.util.BaseViewHolder
import social.tsu.android.ui.util.RetryCallback
import social.tsu.android.utils.isWifiConnected
import kotlin.math.abs
import kotlin.math.min


class UserPostsAdapter(
    private val application: TsuApplication,
    private val exoPlayer: SimpleExoPlayer,
    private val actionCallback: ViewHolderActions,
    private val retryCallback: RetryCallback,
    var addPostComposeView: Boolean = true,
    private val useMiniComposePost: Boolean = false,
    private val showCommunityInTitle: Boolean = true,
    private val nativeAdFetcher: GoogleAdFetcher? = null,
    private val dividerColor: Int = 0,
    var isPublicSupportAdLoaded: Boolean = false,
    var isExclusiveSupportAdLoaded: Boolean = false
) : PagedListAdapter<Post, BaseViewHolder>(POST_DIFF_CALLBACK) {

    private var loadState: Data<Boolean>? = null

    var onItemRemove: (() -> Unit)? = null

    private var composedMessage: String = ""

    companion object {

        const val AD_STRIDE = 3
        private val POST_DIFF_CALLBACK = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem.contentEquals(newItem)
            }
        }
    }

    enum class ViewTypes {
        POST_CONTENT,
        POST_LINK,
        POST_VIDEO,
        POST_CREATE,
        POST_PENDING,
        POST_CREATE_MINI,
        VIEW_LOADING
    }

    private lateinit var context: Context

    private val openGraphService = DefaultOpenGraphService()

    private var layoutManager: LinearLayoutManager? = null
    private var activeVideoPosition: Int = -1
    private var activeVideoId: Long = -1L
    private var isActiveVideoPaused: Boolean = false

    private fun postToContent(post: Post): FeedContent<Post> {
        return if (post.has_stream) {
            VideoPostContent(post)
        } else if (!post.has_picture && (post.has_link || post.getLinks().isNotEmpty())) {
            LinkPostContent(post)
        } else {
            PostContent(post)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        when (viewType) {
            ViewTypes.POST_CREATE.ordinal -> {
                val view = inflater.inflate(R.layout.compose_post, parent, false)
                if (!composedMessage.isNullOrBlank()) {
                    view.findViewById<EditText>(R.id.composePost).append(composedMessage)
                    val imm = context.applicationContext.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)                }
                return CreatePostViewHolder(
                    application,
                    actionCallback,
                    view
                )
            }
            ViewTypes.POST_CREATE_MINI.ordinal -> {
                val view = inflater.inflate(R.layout.compose_post_mini, parent, false)
                return CreatePostMiniViewHolder(
                    application,
                    actionCallback,
                    view
                )
            }
            ViewTypes.VIEW_LOADING.ordinal -> {
                return LoadStateViewHolder.create(parent, retryCallback)
            }
            ViewTypes.POST_VIDEO.ordinal -> {
                val view = inflater.inflate(R.layout.post_video, parent, false)
                val holder = VideoPostHolder(
                    application,
                    exoPlayer,
                    videoCallback,
                    actionCallback,
                    view,
                    showCommunityInTitle,
                    dividerColor
                )
                return holder
            }
            ViewTypes.POST_LINK.ordinal -> {
                val view = inflater.inflate(R.layout.post_link, parent, false)
                return LinkPostHolder(
                    application,
                    actionCallback,
                    openGraphService,
                    view,
                    showCommunityInTitle,
                    dividerColor
                )
            }
            ViewTypes.POST_PENDING.ordinal -> {
                val view = inflater.inflate(R.layout.post_pending, parent, false)
                return PendingPostViewHolder(application,actionCallback, view)
            }
            else -> {
                val view = inflater.inflate(R.layout.post_text, parent, false)
                val holder = PostViewHolder(
                    application,
                    actionCallback,
                    view,
                    showCommunityInTitle,
                    dividerColor
                )
                return holder
            }
        }
    }

    override fun getItemViewType(position: Int): Int {

        if (hasExtraRow() && position == itemCount - 1) {
            return ViewTypes.VIEW_LOADING.ordinal
        }

        if (addPostComposeView && position == 0) {
            return if (useMiniComposePost) ViewTypes.POST_CREATE_MINI.ordinal else ViewTypes.POST_CREATE.ordinal
        }
        getItem(position)?.let {
            return when (postToContent(it)) {
                is VideoPostContent -> ViewTypes.POST_VIDEO.ordinal
                is LinkPostContent -> ViewTypes.POST_LINK.ordinal
                else -> ViewTypes.POST_CONTENT.ordinal
            }
        } ?: kotlin.run {
            return ViewTypes.POST_CONTENT.ordinal
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ViewTypes.POST_CREATE.ordinal -> {
                holder.bind(null)
                return
            }
            ViewTypes.POST_CREATE_MINI.ordinal -> {
                holder.bind(null)
                return
            }
            ViewTypes.VIEW_LOADING.ordinal -> {
                holder.bind(loadState)
                return
            }
        }
        getItem(position)?.let {
            val item = postToContent(it)
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
                                if (video.privacy == Post.PRIVACY_EXCLUSIVE) {
                                    val supportPostIds = ArrayList<String>()
                                    holder.sharedPrefManager.getSupportPostId()?.let {
                                        supportPostIds.addAll(it.split(","))
                                    }
                                    if (video.isCurrentUserCreator || supportPostIds.contains(video.id.toString())) {
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
            if (position == 0) {
                actionCallback.refreshPosts()
            }

            if (holder is PostViewHolder) {
                if (item.getContent().privacy == 2) {
                    holder.bindSupportBtnAvailability(isExclusiveSupportAdLoaded)
                } else {
                    holder.bindSupportBtnAvailability(isPublicSupportAdLoaded)
                }
            }
        }
    }

    fun playVideo(position: Int, postId: Long) {
        startPlaying(position, postId, true)
    }

    private fun hasExtraRow(): Boolean {
        return loadState != null && loadState !is Data.Success
    }

    fun setLoadState(newLoadState: Data<Boolean>) {
        if (!currentList.isNullOrEmpty() && newLoadState == loadState) {
            val previousState = this.loadState
            val hadExtraRow = hasExtraRow()
            this.loadState = newLoadState

            val hasExtraRow = hasExtraRow()

            if (hadExtraRow != hasExtraRow) {
                if (hadExtraRow) {
                    notifyItemRemoved(itemCount)
                } else {
                    notifyItemInserted(itemCount)
                }
            } else if (hasExtraRow && previousState != newLoadState) {
                notifyItemChanged(itemCount - 1)
            }
        }
    }

    override fun getItemCount(): Int = differ.itemCount
        .plus(/*create post*/composePostViewCount())
        .plus(/*loading view holder*/if (hasExtraRow()) 1 else 0)

    private fun getPostPosition(position: Int): Int {
        return position
            .minus(/*create post holder*/(composePostViewCount())
                .plus(/*loading view holder*/if (hasExtraRow()) 1 else 0)
            )
    }

    fun getAdapterPosition(position: Int): Int {
        return position
            .plus(/*create post holder*/(composePostViewCount())
                .plus(/*loading view holder*/if (hasExtraRow()) 1 else 0)
            )
    }

    private fun composePostViewCount() = if (addPostComposeView) 1 else 0

    private val adapterCallback = AdapterListUpdateCallback(this)

    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            adapterCallback.onInserted(getAdapterPosition(position), count)
        }

        override fun onRemoved(position: Int, count: Int) {
            adapterCallback.onRemoved(getAdapterPosition(position), count)
            onItemRemove?.invoke()
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            adapterCallback.onMoved(
                getAdapterPosition(fromPosition),
                getAdapterPosition(toPosition)
            )
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            adapterCallback.onChanged(getAdapterPosition(position), count, payload)
        }
    }

    private val differ = AsyncPagedListDiffer(
        listUpdateCallback,
        AsyncDifferConfig.Builder(POST_DIFF_CALLBACK).build()
    )

    override fun getItem(position: Int): Post? {
        return differ.getItem(getPostPosition(position))
    }

    override fun submitList(pagedList: PagedList<Post>?) {
        differ.submitList(pagedList)
    }

    override fun submitList(pagedList: PagedList<Post>?, commitCallback: Runnable?) {
        differ.submitList(pagedList, commitCallback)
    }

    override fun getCurrentList(): PagedList<Post>? {
        return differ.currentList
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
                            getItem(position)?.let { post ->
                                postToContent(post).getContent().let { item ->
                                    mostVisibleId = item.id
                                }
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

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)
        val adapterPosition = holder.adapterPosition
        if (adapterPosition > 0 && adapterPosition % AD_STRIDE == 0) {
            nativeAdFetcher?.onAdRecycled(adapterPosition)
        }
    }

    fun getAdContent(position: Int): AdContent? {
        return nativeAdFetcher?.getAd(position)
    }

    fun onDestroy() {
        nativeAdFetcher?.onDestroy()
    }

    private val videoCallback = object : VideoPostHolder.Callback {
        override fun onStartPlaying(videoId: Long, position: Int) {
            Log.d("VideoHolder", "videoCallback $position id - $videoId")
            startPlaying(position, videoId, false)
        }
    }

    fun updateComposetext(compositeText: String) {
        composedMessage = compositeText
    }

    interface ViewHolderActions : PostViewHolder.ViewHolderActions,
        VideoPostHolder.ViewHolderActions,
        PendingPostViewHolder.ViewHolderActions,
        CreatePostViewHolder.ViewHolderActions,
        CreatePostMiniViewHolder.ViewHolderActions,
        LinkPostHolder.ViewHolderActions {

        fun refreshPosts()

    }
}
