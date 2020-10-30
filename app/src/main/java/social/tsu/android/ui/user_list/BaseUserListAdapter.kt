package social.tsu.android.ui.user_list

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import social.tsu.android.network.model.UserProfile
import social.tsu.android.network.model.diffutil.UserProfileDiffCallback


abstract class BaseUserListAdapter<T : BaseUserViewHolder> : RecyclerView.Adapter<T>() {

    private val userList = arrayListOf<UserProfile>()

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: T, position: Int) {
        val item = userList[position]
        holder.bind(item)
    }

    suspend fun submitList(list: List<UserProfile>) = withContext(Dispatchers.IO) {
        val oldList = ArrayList(userList)
        val diffCallback = UserProfileDiffCallback(oldList, list)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        withContext(Dispatchers.Main) {
            if (isActive) {
                userList.clear()
                userList.addAll(list)
                diffResult.dispatchUpdatesTo(this@BaseUserListAdapter)
            }
        }
    }

}