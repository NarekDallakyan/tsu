package social.tsu.android.ui.user_profile.insights.family_tree

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import social.tsu.android.R
import social.tsu.android.network.model.UserProfile


interface UserFamilyTreeUserCallback {
    fun onUserClick(userProfile: UserProfile)
}

class UserFamilyTreeViewHolder(
    itemView: View,
    private val callback: UserFamilyTreeUserCallback
) : RecyclerView.ViewHolder(itemView) {

    companion object {

        fun create(
            parent: ViewGroup,
            callback: UserFamilyTreeUserCallback
        ): UserFamilyTreeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_family_tree_user, parent, false)
            return UserFamilyTreeViewHolder(
                view,
                callback
            )
        }

    }

    private val userIcon: CircleImageView? = itemView.findViewById(R.id.user_item_photo)
    private val userName: TextView? = itemView.findViewById(R.id.user_item_name)

    private lateinit var itemUser: UserProfile

    init {
        itemView.setOnClickListener {
            itemUser.let { user -> callback.onUserClick(itemUser) }
        }
    }

    fun bind(member: UserProfile) {
        this.itemUser = member
        userName?.text = member.fullName

        if (userIcon != null) {
            Glide.with(itemView)
                .load(member.profilePictureUrl)
                .error(R.drawable.user)
                .override(300)
                .into(userIcon)
        }
    }
}