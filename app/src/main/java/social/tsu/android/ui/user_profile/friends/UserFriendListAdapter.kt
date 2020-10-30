package social.tsu.android.ui.user_profile.friends

import android.view.ViewGroup
import social.tsu.android.ui.user_list.BaseUserListAdapter
import social.tsu.android.ui.user_list.BaseUserViewHolder


interface UserListCallback : UserFriendViewHolderCallback,
    CurrentUserFriendViewHolderCallback, UserFollowerViewHolderCallback

enum class UserFriendListType {
    FRIEND_LIST, FOLLOWER_LIST, FOLLOWING_LIST
}

class UserFriendListAdapter(
    private val callback: UserListCallback
) : BaseUserListAdapter<BaseUserViewHolder>() {

    var isCurrentUser: Boolean = false
    var type = UserFriendListType.FRIEND_LIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseUserViewHolder {
        return when {
            type == UserFriendListType.FOLLOWER_LIST -> {
                UserFollowerViewHolder.create(parent, callback)
            }
            type == UserFriendListType.FOLLOWING_LIST && !isCurrentUser -> {
                UserFollowerViewHolder.create(parent, callback)
            }
            type == UserFriendListType.FRIEND_LIST && !isCurrentUser -> {
                UserFriendViewHolder.create(parent, callback)
            }
            else -> {
                CurrentUserFriendViewHolder.create(parent, callback)
            }
        }
    }

}