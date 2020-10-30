package social.tsu.android.ui.notifications.feed

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import social.tsu.android.data.local.entity.TsuNotification
import social.tsu.android.data.local.entity.TsuNotificationCategory
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.util.BaseViewHolder
import social.tsu.android.ui.util.LoadStateViewHolder
import social.tsu.android.ui.util.RetryCallback


private const val TYPE_GENERAL = 0
private const val TYPE_LOADING = 1
private const val TYPE_FRIEND_REQUEST = 2
private const val TYPE_MESSAGE = 3

class NotificationAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val callback: ViewHolderActions,
    private val retryCallback: RetryCallback
) : PagedListAdapter<TsuNotification, BaseViewHolder>(NOTIFICATION_DIFF_CALLBACK) {

    private var loadState: Data<Boolean>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            TYPE_GENERAL -> GeneralNotificationViewHolder.create(parent, callback)
            TYPE_FRIEND_REQUEST -> FriendRequestViewHolder.create(
                parent,
                lifecycleOwner,
                callback
            )
            TYPE_MESSAGE -> MessageNotificationViewHolder.create(
                parent,
                callback
            )
            TYPE_LOADING -> LoadStateViewHolder.create(parent, retryCallback)
            else -> throw IllegalArgumentException("unknown view type")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_LOADING) {
            holder.bind(loadState)
        } else {
            holder.bind(getItem(position))
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (hasExtraRow()) {
            return TYPE_LOADING
        }
        return when (getItem(position)?.categoryType) {
            TsuNotificationCategory.GENERAL -> TYPE_GENERAL
            TsuNotificationCategory.FRIEND_REQUESTS -> TYPE_FRIEND_REQUEST
            TsuNotificationCategory.MESSAGES -> TYPE_MESSAGE
            else -> TYPE_GENERAL
        }
    }

    override fun getItemCount(): Int = super.getItemCount().plus(if (hasExtraRow()) 1 else 0)

    private fun hasExtraRow(): Boolean {
        return loadState != null && loadState !is Data.Success
    }

    fun setLoadState(newLoadState: Data<Boolean>) {
        if (!currentList.isNullOrEmpty() && newLoadState == loadState) {
            val previousState = this.loadState
            val hadExtraRow = hasExtraRow()
            this.loadState = newLoadState

            val hasExtraRow = hasExtraRow()

            if (hadExtraRow != hasExtraRow) {
                if (hadExtraRow) {
                    notifyItemRemoved(itemCount)
                } else {
                    notifyItemInserted(itemCount)
                }
            } else if (hasExtraRow && previousState != newLoadState) {
                notifyItemChanged(itemCount - 1)
            }
        }
    }

    interface ViewHolderActions {
        fun onNotificationClick(item: TsuNotification)
        fun onNotificationProfileClick(item: TsuNotification)
        fun onNotificationGroupClick(item: TsuNotification)
        fun didTapOnAccept(item: TsuNotification): LiveData<Data<Boolean>>
        fun didTapOnDecline(item: TsuNotification): LiveData<Data<Boolean>>
    }

    companion object {
        private val NOTIFICATION_DIFF_CALLBACK = object : DiffUtil.ItemCallback<TsuNotification>() {
            override fun areItemsTheSame(
                oldItem: TsuNotification,
                newItem: TsuNotification
            ): Boolean {
                return oldItem.dbId == newItem.dbId
            }

            override fun areContentsTheSame(
                oldItem: TsuNotification,
                newItem: TsuNotification
            ): Boolean {
                return oldItem.dbId == newItem.dbId
            }
        }
    }
}