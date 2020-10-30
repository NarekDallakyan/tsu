package social.tsu.android.adapters

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.ui.PostGridActionCallback
import social.tsu.android.ui.UserPhotoHolder
import social.tsu.android.ui.model.FeedContent
import social.tsu.android.ui.model.PostContent
import social.tsu.android.ui.model.VideoPostContent

open class UserPhotosAdapter(
    private val application: TsuApplication,
    private val actionCallback: PostGridActionCallback?
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
                it.has_picture && !it.is_share
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
        val holder = UserPhotoHolder.create(parent, actionCallback)
        holder.getSupportPostId = getSupportPostId
        return holder
    }

    override fun getItemCount(): Int {
        return content.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = content[position]
        if (holder is UserPhotoHolder) {
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
