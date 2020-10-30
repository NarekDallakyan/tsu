package social.tsu.android.ui.user_profile.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import social.tsu.android.R
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.UserProfile
import social.tsu.android.ui.user_list.BaseUserViewHolder
import social.tsu.android.ui.user_list.UserViewHolderCallback


interface UserFollowerViewHolderCallback : UserViewHolderCallback {
    fun onFollowClick(userProfile: UserProfile)
}

class UserFollowerViewHolder(itemView: View, callback: UserFollowerViewHolderCallback?) :
    BaseUserViewHolder(itemView, callback) {


    companion object {
        fun create(
            parent: ViewGroup,
            callback: UserFollowerViewHolderCallback?
        ): UserFollowerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_user,
                parent,
                false
            )
            return UserFollowerViewHolder(view, callback)
        }
    }

    private val addFollowerBtn: MaterialButton = itemView.findViewById(R.id.user_item_action_btn)

    init {
        addFollowerBtn.setOnClickListener {
            currentItem?.let { callback?.onFollowClick(it) }
        }
    }

    override fun bind(item: UserProfile) {
        super.bind(item)
        bindFollowStatus(item)
    }

    private fun bindFollowStatus(item: UserProfile) {
        val context = addFollowerBtn.context ?: return

        when {
            item.id == AuthenticationHelper.currentUserId -> {
                setButtonAddable(false)
                addFollowerBtn.icon = null
                addFollowerBtn.setText(R.string.user_is_you_btn)
            }
            item.isFollowing -> {
                setButtonAddable(false)
                addFollowerBtn.icon = null
                addFollowerBtn.setText(R.string.btn_following_txt)
            }
            else -> {
                setButtonAddable(true)
                addFollowerBtn.icon = context.getDrawable(R.drawable.ic_btn_add)
                addFollowerBtn.setText(R.string.btn_follow_txt)
            }
        }
    }

    private fun setButtonAddable(addable: Boolean) {
        addFollowerBtn.isEnabled = addable
        addFollowerBtn.isClickable = addable
    }

}