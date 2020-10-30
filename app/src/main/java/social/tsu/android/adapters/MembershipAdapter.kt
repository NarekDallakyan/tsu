package social.tsu.android.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.daimajia.swipe.SwipeLayout
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.model.Membership
import social.tsu.android.network.model.Role
import social.tsu.android.ui.setVisibleOrGone


open class MembershipAdapter(
    private val application: TsuApplication,
    private val actionCallback: MembershipActionCallback?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var context: Context

    val content = mutableListOf<Membership>()
    val loadList = HashMap<Int, Boolean>()

    private var layoutManager: LinearLayoutManager? = null
    var isLoading: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    open fun updateItems(newMemberships: List<Membership>?) {
        newMemberships?.let {
            content.clear()
            content.addAll(newMemberships)
        }
        notifyDataSetChanged()
    }

    fun addItemAtTop(membership: Membership) {
        content.add(0, membership)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.membership_list_item, parent, false)

        return MembershipHolder(application, actionCallback, view)
    }

    override fun getItemCount(): Int {
        return content.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = content[position]
        if (holder is MembershipHolder) {
            val isLoading = loadList[item.id] ?: false
            holder.bind(item, isLoading)
        }

    }

    fun setLoading(membership: Membership, isLoading: Boolean) {
        content.find { membership.id == it.id }?.let {
            loadList[membership.id] = isLoading
            notifyItemChanged(content.indexOf(it))
        }
    }

    fun setLoading(membershipId: Int, isLoading: Boolean) {
        content.find { membershipId == it.id }?.let {
            loadList[membershipId] = isLoading
            notifyItemChanged(content.indexOf(it))
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

    fun acceptMembership(membershipId: Int) {
        content.find { it.id == membershipId }?.let {
            it.status = Membership.Status.ACCEPTED.getString()
            it.group.membersCount++
            notifyItemChanged(content.indexOf(it))
        }
    }

    fun declineMembership(membershipId: Int) {
        var index = -1
        content.find { it.id == membershipId }?.let {
            index = content.indexOf(it)
        }

        if (index != -1) {
            content.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun acceptPromotion(membershipId: Int) {
        content.find { it.id == membershipId }?.let {
            it.role = Role.ADMIN.getString()
            it.status = Membership.Status.ACCEPTED.getString()
            notifyItemChanged(content.indexOf(it))
        }
    }

    fun declinePromotion(membershipId: Int) {
        content.find { it.id == membershipId }?.let {
            it.status = Membership.Status.ACCEPTED.getString()
            notifyItemChanged(content.indexOf(it))
        }
    }
}

class MembershipHolder(
    val application: TsuApplication,
    val actionCallback: MembershipActionCallback?,
    view: View
) : RecyclerView.ViewHolder(view) {

    var photo: ImageView
    var name: TextView
    var topic: TextView
    var membersCount: TextView
    var acceptButton: TextView
    var declineButton: TextView
    var actionlabel: LinearLayout
    var statusIcon: ImageView
    var statusText: TextView
    val cornerRadius: Int
    var swipeLayout: SwipeLayout
    var membershipItem: ConstraintLayout
    var progressbar: ProgressBar

    init {
        photo = itemView.findViewById(R.id.photo)
        name = itemView.findViewById(R.id.name)
        topic = itemView.findViewById(R.id.topic)
        membersCount = itemView.findViewById(R.id.members_count)
        acceptButton = itemView.findViewById(R.id.accept_invite)
        declineButton = itemView.findViewById(R.id.decline_invite)
        actionlabel = itemView.findViewById(R.id.action_label)
        statusIcon = itemView.findViewById(R.id.status_icon)
        statusText = itemView.findViewById(R.id.status_text)
        swipeLayout = itemView.findViewById(R.id.membership_item_swipe_layout)
        membershipItem = itemView.findViewById(R.id.membership_item)
        progressbar = itemView.findViewById(R.id.progressbar)
        cornerRadius = itemView.resources.getDimensionPixelSize(R.dimen.community_picture_corners)
    }

    fun bind(membership: Membership, isLoading: Boolean = false) {

        membershipItem.setOnClickListener {
            actionCallback?.itemClicked(membership)
        }

        progressbar.setVisibleOrGone(isLoading)

        //membership.status = "pending"
        when (membership.getStatus()) {
            Membership.Status.PENDING -> {
                if (membership.invitedById != null) {
                    //actions needed
                    acceptButton.visibility = View.VISIBLE
                    declineButton.visibility = View.VISIBLE
                    actionlabel.visibility = View.VISIBLE

                    statusIcon.imageTintList = getColorStateList(R.color.colorPrimary)
                    statusIcon.setImageResource(R.drawable.ic_check)
                    statusText.setText(R.string.community_invited)

                    acceptButton.setOnClickListener {
                        actionCallback?.acceptMembershipClicked(membership)
                    }
                    declineButton.setOnClickListener {
                        actionCallback?.declineMembershipClicked(membership)
                    }

                } else {
                    //waiting for approval by owner
                    acceptButton.visibility = View.GONE
                    declineButton.visibility = View.GONE
                    actionlabel.visibility = View.VISIBLE

                    statusIcon.imageTintList = getColorStateList(R.color.secondary2)
                    statusIcon.setImageResource(R.drawable.ic_pending)
                    statusText.setText(R.string.community_pending)
                }

            }
            Membership.Status.ACCEPTED -> {
                //hide buttons and label
                acceptButton.visibility = View.GONE
                declineButton.visibility = View.GONE
                actionlabel.visibility = View.GONE
            }
            Membership.Status.PENDING_PROMOTION -> {
                acceptButton.visibility = View.VISIBLE
                declineButton.visibility = View.VISIBLE

                actionlabel.visibility = View.VISIBLE

                statusIcon.imageTintList = getColorStateList(R.color.tsu_primary)
                statusIcon.setImageResource(R.drawable.ic_star)
                statusText.setText(R.string.community_promotion)

                acceptButton.setOnClickListener {
                    actionCallback?.acceptPromotionClicked(membership)
                }
                declineButton.setOnClickListener {
                    actionCallback?.declinePromotionClicked(membership)
                }

            }
        }

        //TODO: deal with this weird picture situation in proper way
        val url = membership.group.pictureUrl.split("/groups").last()

        if(url.contains("pictures/square/missing.png")) {
            //missing, load from resource
            Glide.with(itemView.context).load(R.drawable.ic_group_placeholder)
                .transform(RoundedCorners(cornerRadius)).into(photo)
        } else {
            Glide.with(itemView.context).load(formatUrl("/groups$url"))
                .transform(RoundedCorners(cornerRadius)).into(photo)
        }

        name.text = membership.group.name
        topic.text = membership.group.parentName
        membersCount.text = itemView.resources.getQuantityString(
            R.plurals.members_quantity,
            membership.group.membersCount,
            membership.group.membersCount
        )


    }

    private fun formatUrl(source: String): String {
        if (source.startsWith("/")) {
            return "${HostProvider.imageHost}${source}"
        }

        return source
    }

    private fun getColorStateList(@ColorRes colorRes: Int): ColorStateList {
        return itemView.context.getColorStateList(colorRes)
    }
}

interface MembershipActionCallback {
    fun itemClicked(membership: Membership)
    fun acceptMembershipClicked(membership: Membership)
    fun acceptPromotionClicked(membership: Membership)
    fun declineMembershipClicked(membership: Membership)
    fun declinePromotionClicked(membership: Membership)
}