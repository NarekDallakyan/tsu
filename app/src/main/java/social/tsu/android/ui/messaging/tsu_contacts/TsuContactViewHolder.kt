package social.tsu.android.ui.messaging.tsu_contacts

import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.android.synthetic.main.tsu_contact_item.view.*
import social.tsu.android.R
import social.tsu.android.data.local.models.TsuContact
import social.tsu.android.helper.Constants
import social.tsu.android.ui.setVisibleOrGone
import social.tsu.android.ui.util.BaseViewHolder

class TsuContactViewHolder(
    itemView: View,
    private val actions: TsuContactAdapter.ViewHolderAction
) :
    BaseViewHolder(itemView) {

    private lateinit var contact: TsuContact

    private val contactProfilePic = itemView.contact_profile_pic

    private val contactNameTextView = itemView.contact_name_textview

    private val contactUserNameTextView = itemView.contact_username_textview

    private val actionButton = itemView.action_button

    override fun <T> bind(item: T) {
        contact = item as TsuContact


        Glide.with(contactProfilePic)
            .load(contact.profilePictureUrl)
            .placeholder(R.drawable.user)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(contactProfilePic)

        val imageSpan = ImageSpan(itemView.context, R.drawable.ic_verified_small)
        val content = SpannableString(contact.fullName.plus(" "))
        if (Constants.isVerified(item.verifiedStatus))
            content.setSpan(imageSpan, content.length - 1, content.length, 0)
        contactNameTextView.text = content
        contactUserNameTextView.text =
            itemView.context.getString(R.string.contact_username, contact.username)

        contactProfilePic.setOnClickListener {
            actions.onProfilePicClicked(contact)

        }

        itemView.setOnClickListener {
            actions.onContactClick(contact)
        }

        actionButton.isChecked = actions.isContactInvited(contact)
        actionButton.setOnClickListener {
            actions.onActionButtonClick(contact)
        }

    }

    companion object {

        fun create(
            parent: ViewGroup,
            actions: TsuContactAdapter.ViewHolderAction,
            actionText: String?
        ): TsuContactViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.tsu_contact_item, parent, false)
            val actionButton = view.findViewById<ToggleButton>(R.id.action_button)
            actionText?.let {
                actionButton.text = actionText
            } ?: run {
                actionButton.setVisibleOrGone(false)
            }
            return TsuContactViewHolder(view, actions)
        }
    }
}