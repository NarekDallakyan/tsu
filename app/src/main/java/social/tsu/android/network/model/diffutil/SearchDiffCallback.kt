package social.tsu.android.network.model.diffutil

import androidx.recyclerview.widget.DiffUtil
import social.tsu.android.network.model.Group
import social.tsu.android.network.model.HashTag
import social.tsu.android.network.model.SearchUser


class SearchDiffCallback(
    private val oldList: List<Any>,
    private val newList: List<Any>
) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldValue = oldList[oldItemPosition]
        val newValue = newList[newItemPosition]
        if (oldValue is SearchUser && newValue is SearchUser) {
            return oldValue.id == newValue.id
        }
        if (oldValue is Group && newValue is Group) {
            return oldValue.id == newValue.id
        }
        if (oldValue is HashTag && newValue is HashTag) {
            return oldValue.id == newValue.id
        }
        return false
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

}