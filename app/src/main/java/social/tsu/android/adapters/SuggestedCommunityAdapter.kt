package social.tsu.android.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import social.tsu.android.R
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.model.Group

class SuggestedCommunityAdapter(
    context: Context,
    var suggestedCommunityListener: SuggestedCommunityListener? = null
) : RecyclerView.Adapter<SuggestedCommunityViewHolder>() {

    private var data: List<GroupViewModel> = emptyList()

    val glide: RequestManager = Glide.with(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SuggestedCommunityViewHolder {
        return SuggestedCommunityViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.suggested_community,
                parent,
                false
            ), suggestedCommunityListener
        )
    }

    fun setData(data: List<Group>) {
        this.data = data.map { GroupViewModel(it) }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: SuggestedCommunityViewHolder, position: Int) {
        holder.bind(data[position], glide)
    }

    fun unjoinComuunity(group: Group) {
        val index = data.indexOfFirst { it.group == group }
        if (index >= 0) {
            data[index].joined = false
            notifyItemChanged(index)
        }
    }
}

class SuggestedCommunityViewHolder(
    itemView: View,
    val suggestedCommunityListener: SuggestedCommunityListener? = null
) :
    RecyclerView.ViewHolder(itemView) {
    fun bind(groupViewModel: GroupViewModel, glide: RequestManager) {
        val group = groupViewModel.group

        itemView.findViewById<ViewGroup>(R.id.parent).setOnClickListener {
            suggestedCommunityListener?.onCommunityClicked(group)
        }
        itemView.findViewById<TextView>(R.id.title).text = group.name
        itemView.findViewById<TextView>(R.id.members_count).text = itemView.resources.getQuantityString(
            R.plurals.members_quantity,
            group.membersCount,
            group.membersCount
        )

        itemView.findViewById<TextView>(R.id.description).text = group.description
        if (!groupViewModel.joined) {
            itemView.findViewById<ToggleButton>(R.id.join_button)
                .apply {
                    isEnabled = true
                    setOnCheckedChangeListener(null)
                    isChecked = false
                    setOnCheckedChangeListener { button, checked ->
                        suggestedCommunityListener?.onJoinClicked(group)
                        button.setOnCheckedChangeListener(null)
                        button.isEnabled = false
                        groupViewModel.joined = true
                    }
                }
        } else {
            itemView.findViewById<ToggleButton>(R.id.join_button)
                .apply {
                    setOnCheckedChangeListener(null)
                    isChecked = true
                    isEnabled = false
                }
        }

        var url = group.pictureUrl
        if (!group.pictureUrl.startsWith("/")) {
            url = group.pictureUrl.split("/groups").last()
        }
        glide.load(HostProvider.imageHost + "/groups" + url)
            .placeholder(R.drawable.ic_group_placeholder)
            .into(itemView.findViewById(R.id.thumbnail))
    }
}

interface SuggestedCommunityListener {

    fun onJoinClicked(group: Group)
    fun onCommunityClicked(group: Group)
}

data class GroupViewModel(val group: Group, var joined: Boolean = false)