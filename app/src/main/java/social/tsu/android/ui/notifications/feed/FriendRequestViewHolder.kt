package social.tsu.android.ui.notifications.feed

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.daimajia.swipe.SwipeLayout
import social.tsu.android.R
import social.tsu.android.data.local.entity.TsuNotification
import social.tsu.android.helper.NetworkHelper
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.model.Data


private const val TAG = "FriendRequestViewHolder"

class FriendRequestViewHolder(
    itemView: View,
    private val lifecycleOwner: LifecycleOwner,
    private val callback: NotificationAdapter.ViewHolderActions
) : BaseNotificationViewHolder(callback, itemView) {

    private val swipeLayout: SwipeLayout = itemView.findViewById(R.id.notification_swipe_layout)
    private val confirmBtn: TextView = itemView.findViewById(R.id.request_confirm)
    private val declineBtn: TextView = itemView.findViewById(R.id.request_delete)
    private val progressBar: ProgressBar = itemView.findViewById(R.id.request_action_progress)

    override fun <T> bind(item: T) {
        val friendRequest = item as TsuNotification

        super.bind(friendRequest)

        confirmBtn.setOnClickListener {
            //Toast.makeText(itemView.context, "Accept friend ${friendRequest.user_id} / ${friendRequest.full_name}", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Accept clicked")
            swipeLayout.close()
            if (NetworkHelper.isNetworkAvailable(swipeLayout.context)) {
                callback.didTapOnAccept(friendRequest)
                    .observe(lifecycleOwner, Observer(this::handleRequestLoadState))
            } else
                MainActivity.instance?.internetSnack()
        }

        declineBtn.setOnClickListener {
            //Toast.makeText(itemView.context, "DELCINE friend ${friendRequest.user_id} / ${friendRequest.full_name}", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Decline clicked")
            swipeLayout.close()
            if (NetworkHelper.isNetworkAvailable(swipeLayout.context)) {
                callback.didTapOnDecline(friendRequest)
                    .observe(lifecycleOwner, Observer(this::handleRequestLoadState))
            } else
                MainActivity.instance?.internetSnack()
        }

        itemView.setOnClickListener {
            callback.onNotificationClick(friendRequest)
        }
    }

    private fun handleRequestLoadState(loadState: Data<Boolean>) {
        swipeLayout.isSwipeEnabled = loadState !is Data.Loading
        progressBar.visibility = if (loadState is Data.Loading) View.VISIBLE else View.GONE
    }

    companion object {
        fun create(
            parent: ViewGroup,
            lifecycleOwner: LifecycleOwner, actions: NotificationAdapter.ViewHolderActions
        ): FriendRequestViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view =
                layoutInflater.inflate(R.layout.friend_request_notification_item, parent, false)
            return FriendRequestViewHolder(
                view,
                lifecycleOwner,
                actions
            )
        }
    }

}