package social.tsu.android.network.model.diffutil

import androidx.recyclerview.widget.DiffUtil
import social.tsu.android.network.model.CommunityMember


class CommunityMemberDiffCallback(
    private val oldList: List<Any>,
    private val newList: List<Any>
) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldValue = oldList[oldItemPosition]
        val newValue = newList[newItemPosition]
        if (oldValue is CommunityMember && newValue is CommunityMember) {
            return oldValue.id == newValue.id
        }
        if (oldValue is Int && newValue is Int) {
            return oldValue == newValue
        }
        return false
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

}