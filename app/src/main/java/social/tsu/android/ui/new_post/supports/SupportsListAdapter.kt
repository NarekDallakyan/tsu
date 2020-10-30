package social.tsu.android.ui.new_post.supports

import android.view.ViewGroup
import social.tsu.android.ui.user_list.BaseUserListAdapter


class SupportsListAdapter(
    private val callback: SupportUserCallback
) : BaseUserListAdapter<SupportsUserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupportsUserViewHolder {
        return SupportsUserViewHolder.create(parent, callback)
    }

}