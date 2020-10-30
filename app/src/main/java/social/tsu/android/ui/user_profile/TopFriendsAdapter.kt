package social.tsu.android.ui.user_profile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import social.tsu.android.R
import social.tsu.android.network.model.UserProfile
import social.tsu.android.ui.user_profile.TopFriendsAdapter.Companion.TYPE_FRIEND
import social.tsu.android.ui.user_profile.TopFriendsAdapter.Companion.TYPE_HEADER

class TopFriendsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val TYPE_HEADER = 0
        val TYPE_FRIEND = 1
    }

    private val friends = ArrayList<TopFriendItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            TYPE_HEADER -> {
                HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_top_friend_header, parent, false))
            }
            TYPE_FRIEND -> {
                TopFriendViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_top_friend, parent, false))
            }
            else -> throw IllegalStateException()
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)) {
            TYPE_FRIEND -> {
                (holder as TopFriendViewHolder).bind(friends[position] as TopFriendModel)
            }
            TYPE_HEADER -> {
                (holder as HeaderViewHolder).bind()
            }
        }
    }

    fun setFriends(list: ArrayList<TopFriendModel>) {
        friends.clear()
        friends.addAll(list.filter { it.isTopFriend })
        friends.add(TopFriendHeader())
        friends.addAll(list.filter { !it.isTopFriend })
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return friends.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (friends[position] is TopFriendModel)
            TYPE_FRIEND
        else
            TYPE_HEADER
    }
}

class TopFriendViewHolder(view: View): RecyclerView.ViewHolder(view) {

    fun bind(user: TopFriendModel) {
        itemView.findViewById<CircleImageView>(R.id.user_item_photo).apply {
            setImageResource(R.drawable.user)
        }
        itemView.findViewById<TextView>(R.id.user_item_name).apply {
            setText(user.name)
        }
        itemView.findViewById<Button>(R.id.user_item_action_btn).apply {
            if (user.isTopFriend) {
                setText(R.string.top_friends_remove_btn)
                setTextColor(resources.getColor(android.R.color.white))
            } else {
                setText(R.string.top_friends_add_btn)
                setTextColor(resources.getColor(android.R.color.black))
            }
            isEnabled = !user.isTopFriend
        }
    }
}

class HeaderViewHolder(view: View): RecyclerView.ViewHolder(view) {

    fun bind() {
        itemView.findViewById<TextView>(R.id.item_header).setText(R.string.top_friends_header)
    }
}

interface TopFriendItem {
    fun getType(): Int
}

class TopFriendHeader: TopFriendItem {
    override fun getType(): Int {
        return TYPE_HEADER
    }
}

data class TopFriendModel(val userId: Int, val name: String, val photoUrl: String, var isTopFriend: Boolean) : TopFriendItem {
    override fun getType(): Int {
        return TYPE_FRIEND
    }
}