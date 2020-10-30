package social.tsu.android.ui.user_profile.friends

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


interface UserFriendViewHolderCallback :
    UserViewHolderCallback {
    fun onAddFriendClick(userProfile: UserProfile)
}

class UserFriendViewHolder(
    itemView: View,
    private val callback: UserFriendViewHolderCallback?
) : BaseUserViewHolder(itemView, callback) {

    companion object {
        fun create(
            parent: ViewGroup,
            callback: UserFriendViewHolderCallback?
        ): UserFriendViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_user,
                parent,
                false
            )
            return UserFriendViewHolder(view, callback)
        }
    }

    private val addFriendBtn: MaterialButton = itemView.findViewById(R.id.user_item_action_btn)

    init {
        addFriendBtn.setOnClickListener {
            currentItem?.let {
                addFriendBtn.isEnabled = false
                callback?.onAddFriendClick(it)
            }
        }
    }

    override fun bind(item: UserProfile) {
        super.bind(item)
        bindFriendStatus(item)
    }

    private fun bindFriendStatus(item: UserProfile) {
        val context = addFriendBtn.context ?: return
        when  {
            item.id == AuthenticationHelper.currentUserId -> {
                setButtonAddable(false)
                addFriendBtn.icon = null
                addFriendBtn.setText(R.string.user_is_you_btn)
            }
            item.userFriendshipStatus == UserFriendshipStatus.REQUESTED -> {
                setButtonAddable(false)
                addFriendBtn.icon = null
                addFriendBtn.setText(R.string.requested)
            }
            item.userFriendshipStatus == UserFriendshipStatus.PENDING -> {
                setButtonAddable(false)
                addFriendBtn.icon = context.getDrawable(R.drawable.ic_pending)
                addFriendBtn.setText(R.string.pending)
            }
            item.userFriendshipStatus == UserFriendshipStatus.ACCEPTED -> {
                setButtonAddable(false)
                addFriendBtn.icon = null
                addFriendBtn.setText(R.string.user_is_friend_btn)
            }
            else -> {
                setButtonAddable(true)
                addFriendBtn.icon = context.getDrawable(R.drawable.ic_btn_add)
                addFriendBtn.setText(R.string.user_friend_list_add)
            }
        }
    }

    private fun setButtonAddable(addable: Boolean) {
        addFriendBtn.isEnabled = addable
        addFriendBtn.isClickable = addable
    }

}