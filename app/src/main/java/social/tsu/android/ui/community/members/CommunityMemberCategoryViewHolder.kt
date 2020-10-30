package social.tsu.android.ui.community.members

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.member_category_title.view.*
import social.tsu.android.R


class CommunityMemberCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        fun create(parent: ViewGroup):CommunityMemberCategoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.member_category_title, parent, false)
            return CommunityMemberCategoryViewHolder(view)
        }
    }

    fun bind(@StringRes titleRes: Int) {
        itemView.title.setText(titleRes)
    }

}