package social.tsu.android.network.model.diffutil

import androidx.recyclerview.widget.DiffUtil
import social.tsu.android.network.model.UserProfile


class UserProfileDiffCallback(
    private val oldList: List<UserProfile>,
    private val newList: List<UserProfile>
):DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList.getOrNull(oldItemPosition)?.id == newList.getOrNull(newItemPosition)?.id
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList.getOrNull(oldItemPosition) == newList.getOrNull(newItemPosition)
    }

}