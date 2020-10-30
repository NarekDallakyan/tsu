package social.tsu.android.ui.user_profile.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import social.tsu.android.R
import social.tsu.android.network.model.UserProfile
import social.tsu.android.ui.user_list.BaseUserViewHolder
import social.tsu.android.ui.user_list.UserViewHolderCallback


interface CurrentUserFriendViewHolderCallback :
    UserViewHolderCallback {
    fun onShowOptionsClick(userProfile: UserProfile)
}

class CurrentUserFriendViewHolder(
    itemView: View,
    private val callback: CurrentUserFriendViewHolderCallback?
) : BaseUserViewHolder(itemView, callback) {

    companion object {
        fun create(
            parent: ViewGroup,
            callback: CurrentUserFriendViewHolderCallback?
        ): CurrentUserFriendViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.current_user_friend_item,
                parent,
                false
            )
            return CurrentUserFriendViewHolder(view, callback)
        }
    }

    private val addFriendBtn: ImageButton = itemView.findViewById(R.id.user_item_more_btn)

    init {
        addFriendBtn.setOnClickListener {
            currentItem?.let { callback?.onShowOptionsClick(it) }
        }
    }

}