package social.tsu.android.network.model.diffutil

import androidx.recyclerview.widget.DiffUtil
import social.tsu.android.data.local.entity.Post


class PostDiffCallback(
    private val oldList: List<Post>,
    private val newList: List<Post>
) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].contentEquals(newList[newItemPosition])
    }

}