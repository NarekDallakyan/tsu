package social.tsu.android.ui.search

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import social.tsu.android.R
import social.tsu.android.helper.Constants
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.model.Group
import social.tsu.android.network.model.HashTag
import social.tsu.android.network.model.MentionUser
import social.tsu.android.network.model.SearchUser
import social.tsu.android.utils.hide
import social.tsu.android.utils.show


class SearchResultViewHolder(
    itemView: View,
    private val clickListener: OnItemClickListener<Any>
) : RecyclerView.ViewHolder(itemView) {

    private val username: TextView? = itemView.findViewById(R.id.username)
    private val fullname: TextView? = itemView.findViewById(R.id.fullname)
    private val userIcon: ImageView? = itemView.findViewById(R.id.user_icon)

    private val groupTitle: TextView? = itemView.findViewById(R.id.group_title)
    private val groupOwner: TextView? = itemView.findViewById(R.id.group_owner_name)
    private val groupIcon: ImageView? = itemView.findViewById(R.id.group_icon)

    private val hashTagText: TextView? = itemView.findViewById(R.id.hashtag_text)
    private val hastTagIcon: ImageView? = itemView.findViewById(R.id.hashtag_icon)
    private val ivVerify: AppCompatImageView? = itemView.findViewById(R.id.ivVerify)

    fun bind(data: Any) {
        when (data) {
            is MentionUser -> bindMention(data)
            is SearchUser -> bindUser(data)
            is Group -> bindGroup(data)
            is HashTag -> bindHashTag(data)
        }

        itemView.setOnClickListener {
            clickListener.onItemClicked(itemView, data)
        }
    }

    private fun bindUser(data: SearchUser) {
        username?.text = data.username
        fullname?.text = data.fullName

        if (userIcon != null) {
            Glide.with(itemView)
                .load(data.profilePictureUrl)
                .override(300)
                .error(R.drawable.user)
                .into(userIcon)
        }
        ivVerify?.visibility =
            if (Constants.isVerified(data.verifiedStatus)) View.VISIBLE else View.GONE
    }

    private fun bindMention(data: MentionUser) {
        username?.text = data.username
        fullname?.text = data.fullName

        if (userIcon != null) {
            Glide.with(itemView)
                .load(data.profilePictureUrl)
                .override(300)
                .error(R.drawable.user)
                .into(userIcon)
        }
    }

    private fun bindGroup(data: Group) {
        groupTitle?.text = data.name

        if (!data.ownerName.isNullOrEmpty()) {
            groupOwner?.show()
            groupOwner?.text = data.ownerName
        } else {
            groupOwner?.hide()
        }

        if (groupIcon != null) {
            if (data.pictureUrl == "/pictures/square/missing.png") {
                groupIcon.setImageResource(R.drawable.ic_group_placeholder)
            } else {
                Glide.with(itemView)
                    .load(formatGroupImageUrl(data))
                    .override(300)
                    .error(R.drawable.ic_group_placeholder)
                    .into(groupIcon)
            }
        }
    }

    private fun formatGroupImageUrl(data: Group): String {
        val source = data.pictureUrl.split("/groups").last()
        return "${HostProvider.imageHost}/groups${source}".replace("square", "cover")
    }

    private fun bindHashTag(data: HashTag) {
        hashTagText?.text = "#${data.text}"
    }

}