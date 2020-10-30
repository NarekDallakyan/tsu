package social.tsu.android.ui.user_profile.insights.family_tree

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.R
import social.tsu.android.ui.setVisibleOrGone


class UserFamilyTreeLoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        fun create(parent: ViewGroup): UserFamilyTreeLoadingViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.load_state_item, parent, false)
            return UserFamilyTreeLoadingViewHolder(
                view
            )
        }
    }

    fun bind() {
        itemView.findViewById<View>(R.id.loading_progress_bar).setVisibleOrGone(true)
    }
}