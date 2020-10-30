package social.tsu.android.ui.community.members

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView
import social.tsu.android.R
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.model.PendingRequest


private const val TYPE_TITLE = 0
private const val TYPE_MEMBER = 1

class PendingMembershipAdapter(
    private val callback: PendingRequestsActionCallback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var context: Context

    val content = mutableListOf<PendingRequest>()

    private var layoutManager: LinearLayoutManager? = null

    fun updateItems(pendingRequests: List<PendingRequest>?) {
        pendingRequests?.let {
            content.clear()
            content.addAll(pendingRequests)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_pending_member, parent, false)

        return PendingRequestsHolder(callback, view)
    }

    override fun getItemCount(): Int {
        return content.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = content[position]
        if(holder is PendingRequestsHolder){
            holder.bind(item)
        }

    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context

        val layoutManager = recyclerView.layoutManager
        if (layoutManager is LinearLayoutManager) {
            this.layoutManager = layoutManager
        }
    }

    fun remove(pendingId: Int) {
        content.removeAll {
                request -> request.membershipId == pendingId
        }

        notifyDataSetChanged()
    }


}

class PendingRequestsHolder(
    val actionCallback: PendingRequestsActionCallback?,
    view: View
) : RecyclerView.ViewHolder(view) {

    var photo: CircleImageView
    var name: TextView
    var followersText: TextView
    var acceptBtn: MaterialButton
    var denyBtn: MaterialButton

    init {
        photo = itemView.findViewById(R.id.user_item_photo)
        name = itemView.findViewById(R.id.user_item_name)
        followersText = itemView.findViewById(R.id.friends_count)
        acceptBtn = itemView.findViewById(R.id.pending_btn_accept)
        denyBtn = itemView.findViewById(R.id.pending_btn_deny)
    }

    fun bind(pendingRequest : PendingRequest) {
        name.text = pendingRequest.fullname
        followersText.visibility = View.GONE
        acceptBtn.setOnClickListener {
            actionCallback?.acceptClicked(pendingRequest)
        }

        denyBtn.setOnClickListener {
            actionCallback?.denyClicked(pendingRequest)
        }

        itemView.setOnClickListener {
            actionCallback?.itemClicked(pendingRequest)
        }

        val url = pendingRequest.profilePictureUrl.split("/users").last()

        if(url.contains("/assets/user.png")) {
            //missing, load from resource
            Glide.with(itemView.context).load(R.drawable.user).into(photo)
        } else {
            Glide.with(itemView.context).load(formatUrl("/users$url")).into(photo)
        }

    }

    private fun formatUrl(source: String): String {
        if (source.startsWith("/")){
            return "${HostProvider.imageHost}${source}"
        }

        return source
    }
}

interface PendingRequestsActionCallback {
    fun acceptClicked(pendingRequest: PendingRequest)
    fun denyClicked(pendingRequest: PendingRequest)
    fun itemClicked(pendingRequest: PendingRequest)
}