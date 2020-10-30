package social.tsu.android.adapters

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.ui.*
import social.tsu.android.ui.UserVideoHolder
import social.tsu.android.ui.model.FeedContent
import social.tsu.android.ui.model.PostContent
import social.tsu.android.ui.model.VideoPostContent

private const val TYPE_POST_HASH_TAG = 0
private const val TYPE_POST_PHOTO = 1
private const val TYPE_POST_VIDEO = 2

private val tsuColors = intArrayOf(
    R.color.tsu_blue,
    R.color.tsu_green,
    R.color.tsu_yellow,
    R.color.tsu_red
)

open class HashtagGridAdapter(private val actionCallback: PostGridActionCallback?)
    : RecyclerView.Adapter<PostGridViewHolder>() {

    private lateinit var context: Context
    private val hashtagBackgroundMap = linkedMapOf<Int, Int>()

    val content = mutableListOf<FeedContent<Any>>()

    private var layoutManager: LinearLayoutManager? = null
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
            TYPE_POST_VIDEO -> UserVideoHolder.create(parent, actionCallback)
            else -> HashtagResultViewHolder.create(parent, actionCallback)
        }
    }

    override fun getItemCount(): Int {
        return content.size
    }

    override fun onBindViewHolder(holder: PostGridViewHolder, position: Int) {
        content.getOrNull(position)?.let { post ->
            holder.bind((post as PostContent).getContent())

            if (holder is HashtagResultViewHolder) {
                holder.setBackground(tsuColors[hashtagBackgroundMap[position] ?: 0])
            }
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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context

        val layoutManager = recyclerView.layoutManager
        if (layoutManager is LinearLayoutManager) {
            this.layoutManager = layoutManager
        }
    }

}
