package social.tsu.android.ui.community.members

import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView
import social.tsu.android.R
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.Constants
import social.tsu.android.network.model.CommunityMember
import social.tsu.android.utils.hide
import social.tsu.android.utils.show


interface CommunityAdminViewCallback {
    fun onUserClick(member: CommunityMember)
    fun onUserKickClick(member: CommunityMember)
    fun onUserPromoteClick(member: CommunityMember)
}

class CommunityMemberViewHolder(
    itemView: View,
    private val isAllowedToEdit: Boolean,
    private val callback: CommunityAdminViewCallback
) : RecyclerView.ViewHolder(itemView) {

    companion object {

        fun create(
            parent: ViewGroup,
            isAllowedToEdit: Boolean,
            callback: CommunityAdminViewCallback
        ): CommunityMemberViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_community_member, parent, false)
            return CommunityMemberViewHolder(view, isAllowedToEdit, callback)
        }

    }

    private val userIcon: CircleImageView? = itemView.findViewById(R.id.user_item_photo)
    private val userAdminIcon: ImageView? = itemView.findViewById(R.id.user_item_admin_icon)
    private val userName: TextView? = itemView.findViewById(R.id.user_item_name)
    private val kickBtn: MaterialButton? = itemView.findViewById(R.id.user_item_kick_btn)
    private val promoteBtn: MaterialButton? = itemView.findViewById(R.id.user_item_promote_btn)

    private var itemUser: CommunityMember? = null

    init {
        itemView.setOnClickListener {
            itemUser?.let { user -> callback.onUserClick(user) }
        }
        kickBtn?.setOnClickListener {
            itemUser?.let { user -> callback.onUserKickClick(user) }
        }
        promoteBtn?.setOnClickListener {
            itemUser?.let { user -> callback.onUserPromoteClick(user) }
        }
    }

    fun bind(member: CommunityMember?) {
        this.itemUser = member ?: return

        if (!isAllowedToEdit || member.id == AuthenticationHelper.currentUserId || member.isOwner) {
            kickBtn?.hide()
            promoteBtn?.hide()
        } else {
            kickBtn?.show()
            promoteBtn?.show()
        }

        val content = SpannableString(member.fullname.plus(" "))
        member.verifiedStatus?.let { user ->
            if (Constants.isVerified(user)) {
                val imageSpan =
                    ImageSpan(userName?.context!!, R.drawable.ic_verified_extra_small)
                content.setSpan(imageSpan, content.length - 1, content.length, 0)
            }
        }
        userName?.text = content

        if (member.isAdmin) {
            userAdminIcon?.show()
            if (member.isOwner) {
                userAdminIcon?.setImageResource(R.drawable.ic_owner)
            } else {
                userAdminIcon?.setImageResource(R.drawable.ic_admin)
            }
            configurePromoteButton(
                R.color.tsu_primary,
                R.color.black,
                R.string.community_members_demote
            )
        } else {
            userAdminIcon?.hide()
            if (member.status == 1) {
                configurePromoteButton(
                    R.color.button_background_pending,
                    R.color.button_text_pending,
                    R.string.pending,
                    R.drawable.ic_pending,
                    false
                )
            } else {
                configurePromoteButton(
                    R.color.colorPrimary,
                    R.color.black,
                    R.string.community_members_promote
                )
            }
        }

        if (userIcon != null) {
            Glide.with(itemView)
                .load(member.profilePictureUrl)
                .error(R.drawable.user)
                .override(300)
                .into(userIcon)
        }
    }

    private fun getColor(@ColorRes colorRes: Int): Int {
        return ContextCompat.getColor(itemView.context, colorRes)
    }

    private fun configurePromoteButton(
        @ColorRes backgroundColor: Int,
        @ColorRes textColor: Int,
        @StringRes textRes: Int,
        @DrawableRes iconRes: Int? = null,
        isEnabled: Boolean = true
    ) {
        if (promoteBtn == null) return

        promoteBtn.isEnabled = isEnabled
        if (iconRes != null) {
            promoteBtn.setIconResource(iconRes)
            promoteBtn.setIconTintResource(textColor)
        } else {
            promoteBtn.icon = null
        }
        promoteBtn.backgroundTintList =
            ContextCompat.getColorStateList(promoteBtn.context, backgroundColor)
        promoteBtn.setTextColor(getColor(textColor))
        promoteBtn.setText(textRes)
    }

}