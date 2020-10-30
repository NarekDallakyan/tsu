package social.tsu.android.ui.new_post.likes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import social.tsu.android.R
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.UserFriendshipStatus
import social.tsu.android.network.model.UserProfile
import social.tsu.android.ui.user_list.BaseUserViewHolder
import social.tsu.android.ui.user_list.UserViewHolderCallback


interface LikeUserCallback : UserViewHolderCallback {
    fun didUserFollow(userProfile: UserProfile)
}

class LikeUserViewHolder(
    itemView: View,
    private val callback: LikeUserCallback?
) : BaseUserViewHolder(itemView, callback) {

    companion object {
        fun create(parent: ViewGroup, callback: LikeUserCallback?): LikeUserViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_user,
                parent, false
            )
            return LikeUserViewHolder(view, callback)
        }
    }

    private val followBtn: MaterialButton = itemView.findViewById(R.id.user_item_action_btn)

    init {
        followBtn.setOnClickListener {
            followBtn.isEnabled = false
            currentItem?.let { callback?.didUserFollow(it) }
        }
    }

    override fun bind(item: UserProfile) {
        super.bind(item)

        bindFollowStatus(item)
    }

    private fun bindFollowStatus(item: UserProfile) {
        when {
            item.id == AuthenticationHelper.currentUserId -> {
                setButtonAddable(false)
                followBtn.setText(R.string.user_is_you_btn)
            }
            item.userFriendshipStatus == UserFriendshipStatus.ACCEPTED -> {
                setButtonAddable(false)
                followBtn.setText(R.string.user_is_friend_btn)
            }
            item.isFollowing -> {
                setButtonAddable(false)
                followBtn.setText(R.string.user_is_following_btn)
            }
            else -> {
                setButtonAddable(true)
                followBtn.setText(R.string.btn_follow_txt)
            }
        }
    }

    private fun setButtonAddable(addable: Boolean) {
        val context = followBtn.context ?: return
        followBtn.isEnabled = addable
        followBtn.isClickable = addable
        if (addable) {
            followBtn.icon = context.getDrawable(R.drawable.ic_btn_add)
        } else {
            followBtn.icon = null
        }
    }

}