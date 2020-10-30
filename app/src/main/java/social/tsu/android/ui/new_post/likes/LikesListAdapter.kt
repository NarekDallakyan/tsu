package social.tsu.android.ui.new_post.likes

import android.view.ViewGroup
import social.tsu.android.ui.user_list.BaseUserListAdapter


class LikesListAdapter(
    private val userCallback: LikeUserCallback
) : BaseUserListAdapter<LikeUserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LikeUserViewHolder {
        return LikeUserViewHolder.create(parent, userCallback)
    }

}