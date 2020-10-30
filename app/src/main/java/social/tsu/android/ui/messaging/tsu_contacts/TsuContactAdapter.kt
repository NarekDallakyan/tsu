package social.tsu.android.ui.messaging.tsu_contacts

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import social.tsu.android.data.local.models.TsuContact
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.util.BaseViewHolder
import social.tsu.android.ui.util.RetryCallback

class TsuContactAdapter(
    private val actions: ViewHolderAction,
    private val retryCallback: RetryCallback,
    private val actionText: String?
) : PagedListAdapter<TsuContact, BaseViewHolder>(CONTACT_DIFF_CALLBACK) {

    private var loadState: Data<Boolean>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return TsuContactViewHolder.create(parent, actions, actionText)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(getItem(position)!!)
    }

    fun setLoadState(newLoadState:Data<Boolean>) {
        this.loadState = newLoadState
    }

    companion object {

        private val CONTACT_DIFF_CALLBACK = object : DiffUtil.ItemCallback<TsuContact>() {
            override fun areItemsTheSame(oldItem: TsuContact, newItem: TsuContact): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TsuContact, newItem: TsuContact): Boolean {
                return oldItem == newItem
            }
        }
    }

    interface ViewHolderAction {

        fun onContactClick(contact: TsuContact)
        fun onProfilePicClicked(contact: TsuContact)
        fun onActionButtonClick(contact: TsuContact)
        fun isContactInvited(contact: TsuContact): Boolean
    }

}