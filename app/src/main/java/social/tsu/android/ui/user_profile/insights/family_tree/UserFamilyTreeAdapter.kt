package social.tsu.android.ui.user_profile.insights.family_tree

import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.R
import social.tsu.android.network.model.FamilyTreeResponse
import social.tsu.android.network.model.UserProfile


private const val TYPE_TITLE = 0
private const val TYPE_USER = 1
private const val TYPE_LOADING = 2

class UserFamilyTreeAdapter(
    private val callback: UserFamilyTreeUserCallback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val dataList = arrayListOf<Any>()
    private var isCurrentlyLoading = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TITLE -> UserFamilyTreeCategoryViewHolder.create(
                parent
            )
            TYPE_LOADING -> UserFamilyTreeLoadingViewHolder.create(
                parent
            )
            else -> UserFamilyTreeViewHolder.create(
                parent,
                callback
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        val value = dataList[position]
        if (value is Int || value is Children) return TYPE_TITLE
        if (value is LoadingMarker) return TYPE_LOADING
        return TYPE_USER
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = dataList[position]
        if (holder is UserFamilyTreeCategoryViewHolder && data is Int) {
            holder.bind(data)
        } else if (holder is UserFamilyTreeCategoryViewHolder && data is Children) {
            holder.bind(data)
        } else if (holder is UserFamilyTreeViewHolder && data is UserProfile) {
            holder.bind(data)
        } else if (holder is UserFamilyTreeLoadingViewHolder) {
            holder.bind()
        }
    }

    fun setLoading(loading: Boolean) {
        if (loading) {
            if (!isCurrentlyLoading) {
                isCurrentlyLoading = true
                dataList.add(LoadingMarker())
                notifyItemRangeInserted(dataList.size, 1)
            }
        } else {
            if (isCurrentlyLoading) {
                isCurrentlyLoading = false
                val idxToRemove = dataList.size - 1
                if (idxToRemove >= 0) {
                    dataList.removeAt(idxToRemove)
                    notifyItemRemoved(idxToRemove)
                }
            }
        }
    }

    fun isLoading(): Boolean {
        return isCurrentlyLoading
    }

    fun submitList(familyTreeResponse: FamilyTreeResponse) {
        if (dataList.isEmpty()) {
            dataList.add(R.string.analytics_you)
            dataList.add(familyTreeResponse.user)
            dataList.add(R.string.analytics_invited_by)
            familyTreeResponse.invitedBy?.let { user -> dataList.add(user) }
            dataList.add(Children(R.string.analytics_children, familyTreeResponse.childrenCount))
            dataList.addAll(familyTreeResponse.children)
            notifyItemRangeInserted(0, dataList.size)
        } else {
            val oldSize = dataList.size
            dataList.addAll(familyTreeResponse.children)
            notifyItemRangeInserted(oldSize, familyTreeResponse.children.size)
        }
    }

    fun resetList() {
        val size = dataList.size
        dataList.clear()
        notifyItemRangeRemoved(0, size)
    }
}

class LoadingMarker

data class Children(@StringRes val titleRes: Int, val childrenCount: Int)