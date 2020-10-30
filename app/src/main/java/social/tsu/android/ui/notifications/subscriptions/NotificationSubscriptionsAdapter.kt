package social.tsu.android.ui.notifications.subscriptions

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.data.local.entity.TsuSubscriptionTopic
import social.tsu.android.ui.util.BaseViewHolder

class NotificationSubscriptionsAdapter(private val actions: ViewHolderActions) :
    RecyclerView.Adapter<BaseViewHolder>() {

    var alData = ArrayList<TsuSubscriptionTopic>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return NotificationSubscriptionViewHolder.create(
            parent,
            actions
        )
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(alData[position])
    }

    interface ViewHolderActions {
        fun onSubscriptionStatusChanged(item: TsuSubscriptionTopic, isSubscribed:Boolean)
    }
    override fun getItemCount(): Int {
        return alData.size
    }

    fun addData(list: ArrayList<TsuSubscriptionTopic>) {
        this.alData = list
        notifyDataSetChanged()
    }
    /*companion object{
        private val SUBSCRIPTION_DIFF_CALLBACK = object: DiffUtil.ItemCallback<TsuSubscriptionTopic>(){
            override fun areItemsTheSame(oldItem: TsuSubscriptionTopic, newItem: TsuSubscriptionTopic): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: TsuSubscriptionTopic, newItem: TsuSubscriptionTopic): Boolean {
                return oldItem.name == newItem.name && oldItem.subscribed == newItem.subscribed
            }
        }
    }*/
}