package social.tsu.android.ui.messaging.recents

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import social.tsu.android.data.local.entity.RecentContact

class RecentContactsAdapter(
    private val actions: ViewHolderAction
): PagedListAdapter<RecentContact, RecentContactViewHolder>(RECENT_DIFF_CALLBACK){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentContactViewHolder {

        return RecentContactViewHolder.create(parent,actions)
    }

    override fun onBindViewHolder(holder: RecentContactViewHolder, position: Int) {

        holder.bind(getItem(position)!!)

    }

    companion object{

       private val RECENT_DIFF_CALLBACK = object: DiffUtil.ItemCallback<RecentContact>(){
            override fun areItemsTheSame(oldItem: RecentContact, newItem: RecentContact): Boolean {
                // On some reason RecentContact.id can differ for same conversation
                return oldItem.otherUser?.id == newItem.otherUser?.id
            }

            override fun areContentsTheSame(oldItem: RecentContact, newItem: RecentContact): Boolean {
                return oldItem == newItem
            }
        }
    }

    interface ViewHolderAction{

        fun onRecentContactClick(contact: RecentContact)

        fun onRecentContactDelete(contact: RecentContact)

        fun onProfilePicClick(contact: RecentContact)

    }
}