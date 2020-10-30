package social.tsu.android.ui.messaging.recents

import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.android.synthetic.main.recent_contact_item.view.*
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import social.tsu.android.R
import social.tsu.android.data.local.entity.RecentContact
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.Constants
import social.tsu.android.ui.util.BaseViewHolder
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

private const val DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

class RecentContactViewHolder(
    itemView: View,
    private val actions: RecentContactsAdapter.ViewHolderAction
) : BaseViewHolder(itemView) {

    private lateinit var recent: RecentContact

    private var lastAvatar: String? = null

    private val contactProfilePic = itemView.contact_profile_pic

    private val contactNameTextView = itemView.contact_name_textview

    private val recentMessageTextView = itemView.recent_message_textview

    private val recentMessageDateTextView = itemView.recent_message_date_textview

    private val recentSwipeLayout = itemView.contact_swipe_layout

    private val unreadBadge = itemView.recent_message_unread_indicator

    private val unreadBadgeValue = itemView.recent_message_unread_value

    init {
        itemView.findViewById<View>(R.id.contact_delete).setOnClickListener {
            recentSwipeLayout.close()
            actions.onRecentContactDelete(recent)
        }
        itemView.findViewById<View>(R.id.contact_item).setOnClickListener {
            actions.onRecentContactClick(recent)
        }

        contactProfilePic.setOnClickListener {
            actions.onProfilePicClick(recent)
        }
    }

    override fun <T> bind(item: T) {
        recent = item as RecentContact

        if (lastAvatar != recent.otherUser?.profilePictureUrl) {
            lastAvatar = recent.otherUser?.profilePictureUrl
            Glide.with(contactProfilePic)
                .load(lastAvatar)
                .error(R.drawable.user)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(300)
                .into(contactProfilePic)
        }

        val content = SpannableString(recent.otherUser?.fullName.plus(" "))
        recent.otherUser?.let { user ->
            if (Constants.isVerified(user.verifiedStatus)) {
                val imageSpan =
                    ImageSpan(contactNameTextView?.context!!, R.drawable.ic_verified_extra_small)
                content.setSpan(imageSpan, content.length - 1, content.length, 0)
            }
        }
        contactNameTextView.text = content

        if (recent.isRead == false && recent.senderId != AuthenticationHelper.currentUserId) {
            unreadBadge.show()
        } else {
            unreadBadge.hide()
        }

        if (recent.text.isNullOrBlank()) {
            recentMessageTextView.hide()
        } else {
            recentMessageTextView.show()
            recentMessageTextView.text = recent.text
        }

        val pattern = getPattern(recent.createdAt.parseLocalDate(DATE_TIME_PATTERN))
        if (isTimePattern(pattern))
            recentMessageDateTextView.text = recent.createdAt.formatTime(DATE_TIME_PATTERN, pattern)
        else
            recentMessageDateTextView.text = recent.createdAt.formatDate(DATE_TIME_PATTERN, pattern)
    }

    fun delete() {
        actions.onRecentContactDelete(recent)
    }

    private fun String.formatTime(parserPattern: String, timeFormatPattern: String): String {
        return LocalDateTime
            .parse(this, DateTimeFormatter.ofPattern(parserPattern))
            .atOffset(ZoneOffset.UTC)
            .withOffsetSameInstant(OffsetDateTime.now().offset)
            .toOffsetTime()
            .format(DateTimeFormatter.ofPattern(timeFormatPattern))
    }

    private fun String.formatDate(parserPattern: String, datePattern: String): String {
        val date = this.parseLocalDateTime(parserPattern)
        return date.format(DateTimeFormatter.ofPattern(datePattern))
    }

    private fun getPattern(messageLocalDate: LocalDate): String {
        val currentDate = LocalDate.now()
        return when {
            currentDate == messageLocalDate -> getLocalizedTimePattern()
            messageLocalDate.isBefore(currentDate) && messageLocalDate.isAfter(
                currentDate.minusDays(
                    7
                )
            ) -> "E"
            else -> "MMMM dd, yyyy"
        }
    }

    private fun String.parseLocalDateTime(pattern: String): LocalDateTime {
        return LocalDateTime.parse(this, DateTimeFormatter.ofPattern(pattern))
    }

    private fun String.parseLocalDate(pattern: String): LocalDate {
        return LocalDate.parse(this, DateTimeFormatter.ofPattern(pattern))
    }

    private fun getLocalizedTimePattern(): String {
        val formatter =
            DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()) as SimpleDateFormat
        return formatter.toLocalizedPattern()
    }

    private fun isTimePattern(pattern: String) = pattern == "HH:mm" || pattern == "h:mm a"

    companion object {

        fun create(
            parent: ViewGroup,
            actions: RecentContactsAdapter.ViewHolderAction
        ): RecentContactViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.recent_contact_item, parent, false)
            return RecentContactViewHolder(view, actions)
        }
    }
}