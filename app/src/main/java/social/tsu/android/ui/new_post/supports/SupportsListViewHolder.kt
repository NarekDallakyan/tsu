package social.tsu.android.ui.new_post.supports

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


interface SupportUserCallback : UserViewHolderCallback {
    fun didUserSupport(userProfile: UserProfile) {}
}

class SupportsUserViewHolder(
    itemView: View,
    private val callback: SupportUserCallback?
) : BaseUserViewHolder(itemView, callback) {

    companion object {
        fun create(parent: ViewGroup, callback: SupportUserCallback): SupportsUserViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_support_user,
                parent, false
            )
            return SupportsUserViewHolder(view, callback)
        }
    }

    private val supportBtn: MaterialButton = itemView.findViewById(R.id.user_item_support_btn)

    init {
        supportBtn.setOnClickListener {
            supportBtn.isEnabled = false
            currentItem?.let { callback?.didUserSupport(it) }
        }
    }

    override fun bind(item: UserProfile) {
        super.bind(item)

    }

}