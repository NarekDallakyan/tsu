package social.tsu.android.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import jp.wasabeef.blurry.Blurry
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.api.HostProvider
import social.tsu.android.ui.model.FeedContent
import social.tsu.android.ui.model.PostContent
import social.tsu.android.ui.model.VideoPostContent

open class UserVideosAdapter(
    private val application: TsuApplication,
    private val actionCallback: UserVideosAdapterActionCallback?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var context: Context

    val content = mutableListOf<FeedContent<Any>>()
    var getSupportPostId: String = ""

    private var layoutManager: LinearLayoutManager? = null
    var isLoading: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    open fun updatePosts(newPosts: List<Post>?) {
        newPosts?.let {
            content.clear()
            val elements = newPosts.filter {
                it.has_stream && !it.is_share
            }.mapIndexed { index, post ->
                PostContent(post)
            }
            content.addAll(elements)
        }
        notifyDataSetChanged()
    }

    fun updatePost(post: Post) {
        val itemContent = if (post.has_stream) {
            VideoPostContent(post)
        } else {
            PostContent(post)
        }
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
        }
    }

    fun addPost(post: Post) {
        content.add(0, PostContent(post))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.user_video_item, parent, false)
        val holder = UserVideoHolder(application, actionCallback, view)
        holder.getSupportPostId = getSupportPostId
        return holder
    }

    override fun getItemCount(): Int {
        return content.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = content[position]
        if (holder is UserVideoHolder) {
            holder.bind((item as PostContent).getContent())
        }
        Log.d("PHOTOS", "onBind position = $position  size = ${content.size}")

    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context

        val layoutManager = recyclerView.layoutManager
        if (layoutManager is LinearLayoutManager) {
            this.layoutManager = layoutManager
        }
    }

}

class UserVideoHolder(
    val application: TsuApplication,
    val actionCallback: UserVideosAdapterActionCallback?,
    view: View
) : RecyclerView.ViewHolder(view) {

    lateinit var video: ImageView
    lateinit var photoContainer: ConstraintLayout
    var getSupportPostId: String = ""

    fun bind(post: Post) {
        video = itemView.findViewById(R.id.video)
        photoContainer = itemView.findViewById(R.id.video_container)
        post.stream?.thumbnail?.let {
            Glide.with(itemView.context).load(formatStreamUrl(it))
                .fallback(R.drawable.ic_photo_unavailable)
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
                                    .from(drawableToBitmap(resource)).into(video)
                            } else {
                                video.setImageBitmap(drawableToBitmap(resource))
                            }
                        }
                        return true
                    }
                })
                .into(video)
        }

        video.setOnClickListener {
            actionCallback?.onVideoClicked(post, adapterPosition)
        }
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

interface UserVideosAdapterActionCallback {
    fun onVideoClicked(post: Post, position: Int)
}
