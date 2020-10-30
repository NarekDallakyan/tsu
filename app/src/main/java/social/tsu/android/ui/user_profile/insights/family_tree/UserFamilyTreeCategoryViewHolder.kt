package social.tsu.android.ui.user_profile.insights.family_tree

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.family_tree_category_title.view.*
import kotlinx.android.synthetic.main.member_category_title.view.title
import social.tsu.android.R


class UserFamilyTreeCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        fun create(parent: ViewGroup): UserFamilyTreeCategoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.family_tree_category_title, parent, false)
            return UserFamilyTreeCategoryViewHolder(
                view
            )
        }
    }

    fun bind(@StringRes titleRes: Int) {
        itemView.title.setText(titleRes)
    }

    fun bind(data: Children) {
        itemView.title.setText(data.titleRes)
        itemView.counter.text = data.childrenCount.toString()
    }

}