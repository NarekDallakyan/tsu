package social.tsu.android.ui.hashtag

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import social.tsu.android.R
import social.tsu.android.data.local.entity.Post
import social.tsu.android.network.model.diffutil.PostDiffCallback
import social.tsu.android.ui.*


private const val TYPE_POST_HASH_TAG = 0
private const val TYPE_POST_PHOTO = 1
private const val TYPE_POST_VIDEO = 2

private val tsuColors = intArrayOf(
    R.color.tsu_blue,
    R.color.tsu_green,
    R.color.tsu_yellow,
    R.color.tsu_red
)

open class HashtagResultsAdapter(
    private val actionCallback: PostGridActionCallback?
) : RecyclerView.Adapter<PostGridViewHolder>() {

    private val postList = arrayListOf<Post>()
    private val hashtagBackgroundMap = linkedMapOf<Int, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostGridViewHolder {
        return when (viewType) {
            TYPE_POST_PHOTO -> UserPhotoHolder.create(parent, actionCallback)
            TYPE_POST_VIDEO -> UserVideoHolder.create(parent, actionCallback)
            else -> HashtagResultViewHolder.create(parent, actionCallback)
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    override fun onBindViewHolder(holder: PostGridViewHolder, position: Int) {
        postList.getOrNull(position)?.let { post ->
            holder.bind(post)

            if (holder is HashtagResultViewHolder) {
                holder.setBackground(tsuColors[hashtagBackgroundMap[position] ?: 0])
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        postList.getOrNull(position)?.let { post ->
            if (post.has_stream && post.stream?.thumbnail != null) {
                return TYPE_POST_VIDEO
            }
            if (post.has_picture && post.picture_url != "") {
                return TYPE_POST_PHOTO
            }
        }
        return TYPE_POST_HASH_TAG
    }

    suspend fun submitList(list: List<Post>) = withContext(Dispatchers.IO) {
        val oldList = ArrayList(postList)
        val diffCallback = PostDiffCallback(oldList, list)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        hashtagBackgroundMap.clear()
        list.forEachIndexed { index, post ->
            if (!post.has_stream && !post.has_picture) {
                var nextIdx = (hashtagBackgroundMap.values.lastOrNull() ?: -1) + 1
                if (nextIdx >= tsuColors.size) {
                    nextIdx = 0
                }
                hashtagBackgroundMap[index] = nextIdx
            }
        }

        withContext(Dispatchers.Main) {
            if (isActive) {
                postList.clear()
                postList.addAll(list)
                diffResult.dispatchUpdatesTo(this@HashtagResultsAdapter)
            }
        }
    }

}