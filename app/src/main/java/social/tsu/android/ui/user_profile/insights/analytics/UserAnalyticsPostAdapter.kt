package social.tsu.android.ui.user_profile.insights.analytics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_user_analytics_post.view.*
import social.tsu.android.R
import social.tsu.android.data.local.entity.Post
import social.tsu.android.network.api.HostProvider
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class UserAnalyticsPostAdapter(val itemsList: MutableList<Post> = mutableListOf(), val clickListener: (Long) -> Unit) : RecyclerView.Adapter<UserAnalyticsPostAdapter.UserAnalyticsPostItemViewHolder>(){

    override fun getItemCount() = itemsList.size
    var filterType: FilterType = FilterType.VIEWS

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserAnalyticsPostItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_analytics_post, parent, false)
        return UserAnalyticsPostItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserAnalyticsPostItemViewHolder, position: Int) {
        holder.bind(itemsList[position])
    }

    fun submitList(newItemsList: List<Post>) {
        itemsList.clear()
        itemsList.addAll(newItemsList)
        notifyDataSetChanged()
    }

    inner class UserAnalyticsPostItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val postDate = view.findViewById<TextView>(R.id.post_date)
        private val postMessage = view.findViewById<TextView>(R.id.post_message)
        private val postImage = view.findViewById<ImageView>(R.id.post_image)
        private val viewsCounter = view.findViewById<TextView>(R.id.views_counter)
        private val likesCounter = view.findViewById<TextView>(R.id.likes_counter)
        private val commentsCounter = view.findViewById<TextView>(R.id.comments_counter)
        private val sharesCounter = view.findViewById<TextView>(R.id.shares_counter)

        fun bind(item: Post) {
            postDate.text = formatDate(item.created_at)
            postMessage.text = item.content

            when {
                item.has_picture -> {
                    postImage.visibility = View.VISIBLE
                    Glide.with(view)
                        .load(item.picture_url)
                        .error(R.drawable.ic_photo_unavailable)
                        .centerCrop()
                        .override(300, 300)
                        .into(postImage)
                }
                item.has_stream -> {
                    postImage.visibility = View.VISIBLE
                    val thumbnailUrl = item.stream?.thumbnail ?: ""
                    if (thumbnailUrl.isNotEmpty())
                        Glide.with(view)
                            .load(formatStreamUrl(thumbnailUrl))
                            .error(R.drawable.ic_videocam)
                            .centerCrop()
                            .override(300, 300)
                            .into(postImage)
                    else
                        Glide.with(view)
                            .load(R.drawable.ic_videocam)
                            .centerCrop()
                            .override(300, 300)
                            .into(postImage)
                }
                else -> {
                    postImage.visibility = View.GONE
                }
            }

            viewsCounter.text = formatNumber(item.view_count)
            likesCounter.text = formatNumber(item.like_count)
            commentsCounter.text = formatNumber(item.comment_count)
            sharesCounter.text = formatNumber(item.share_count)
            resetIconsColor()
            resetCountersColor()
            when (filterType) {
                FilterType.VIEWS -> {
                    view.views_icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.tsu_yellow)
                    viewsCounter.setTextColor(ContextCompat.getColorStateList(view.context, R.color.analytics_post_item_counter_active))
                }
                FilterType.LIKES -> {
                    view.likes_icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.tsu_red)
                    likesCounter.setTextColor(ContextCompat.getColorStateList(view.context, R.color.analytics_post_item_counter_active))
                }
                FilterType.COMMENTS -> {
                    view.comments_icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.tsu_blue)
                    commentsCounter.setTextColor(ContextCompat.getColorStateList(view.context, R.color.analytics_post_item_counter_active))
                }
                FilterType.SHARES -> {
                    view.shares_icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.tsu_cine)
                    sharesCounter.setTextColor(ContextCompat.getColorStateList(view.context, R.color.analytics_post_item_counter_active))
                }
            }

            view.setOnClickListener {
                clickListener(item.id)
            }
        }

        private fun resetCountersColor() {
            viewsCounter.setTextColor(ContextCompat.getColorStateList(view.context, R.color.analytics_post_item_counter))
            likesCounter.setTextColor(ContextCompat.getColorStateList(view.context, R.color.analytics_post_item_counter))
            commentsCounter.setTextColor(ContextCompat.getColorStateList(view.context, R.color.analytics_post_item_counter))
            sharesCounter.setTextColor(ContextCompat.getColorStateList(view.context, R.color.analytics_post_item_counter))
        }

        private fun formatDate(createdAt: Date): String {
            val formatter = SimpleDateFormat("MM/dd/yyyy - HH:mm a")
            return formatter.format(createdAt)
        }

        private fun resetIconsColor(){
            view.views_icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.analytics_post_item_icon_disabled)
            view.likes_icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.analytics_post_item_icon_disabled)
            view.comments_icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.analytics_post_item_icon_disabled)
            view.shares_icon.imageTintList = ContextCompat.getColorStateList(view.context, R.color.analytics_post_item_icon_disabled)
        }

        private fun formatNumber(value: Int): String {
            return NumberFormat.getIntegerInstance().format(value)
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

    }
}