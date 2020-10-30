package social.tsu.android.ui.community.members

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import social.tsu.android.network.model.CommunityMember
import social.tsu.android.network.model.diffutil.CommunityMemberDiffCallback


private const val TYPE_TITLE = 0
private const val TYPE_MEMBER = 1

class CommunityMembersAdapter(
    private val callback: CommunityAdminViewCallback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isAllowedToEdit: Boolean = false

    private val dataList = arrayListOf<Any>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TITLE -> CommunityMemberCategoryViewHolder.create(parent)
            else -> CommunityMemberViewHolder.create(parent, isAllowedToEdit, callback)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val value = dataList[position]
        if (value is Int) return TYPE_TITLE
        return TYPE_MEMBER
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = dataList[position]
        if (holder is CommunityMemberCategoryViewHolder && data is Int) {
            holder.bind(data)
        } else if (holder is CommunityMemberViewHolder && data is CommunityMember) {
            holder.bind(data)
        }
    }

    suspend fun submitList(membersData: List<Any>) = withContext(Dispatchers.IO) {
        val diffUtil = DiffUtil.calculateDiff(CommunityMemberDiffCallback(dataList, membersData))
        withContext(Dispatchers.Main) {
            if (isActive) {
                dataList.clear()
                dataList.addAll(membersData)
                diffUtil.dispatchUpdatesTo(this@CommunityMembersAdapter)
            }
        }
    }

}