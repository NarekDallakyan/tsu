package social.tsu.android.ui.notifications.feed

import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.view.View
import android.widget.TextView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import social.tsu.android.R
import social.tsu.android.data.local.entity.ResourceType
import social.tsu.android.data.local.entity.TsuNotification
import social.tsu.android.data.local.models.TsuNotificationType
import social.tsu.android.helper.Constants
import social.tsu.android.helper.DateHelper
import social.tsu.android.helper.TSUTextTokenizingHelper
import social.tsu.android.network.api.HostProvider
import social.tsu.android.service.UserProfileImageService
import social.tsu.android.ui.util.BaseViewHolder


abstract class BaseNotificationViewHolder(
    private val actions: NotificationAdapter.ViewHolderActions,
    itemView: View
) : BaseViewHolder(itemView) {

    private val userIcon: CircleImageView? = itemView.findViewById(R.id.notification_user_icon)
    private val message: TextView? = itemView.findViewById(R.id.notification_text)
    private val createdAt: TextView? = itemView.findViewById(R.id.notification_creation_date)

    override fun <T> bind(item: T) {
        val notification = item as TsuNotification

        var pictureUrl = notification.pictureUrl
        if (pictureUrl == null || UserProfileImageService.MISSING_IMAGE_VALUES.contains(pictureUrl)) {
            userIcon?.setImageResource(R.drawable.user)
        } else if (userIcon != null) {

            if (pictureUrl.contains("/groups")) {
                val url = pictureUrl.split("/groups").last()
                pictureUrl = formatUrl("/groups$url")
            }
            Glide.with(itemView)
                .load(pictureUrl)
                .override(300)
                .error(R.drawable.user)
                .into(userIcon)
        }

        userIcon?.setOnClickListener {
            actions.onNotificationProfileClick(notification)
        }

        message?.text = notification.message
        if (notification.actionUserId != null
            || notification.resource?.resourceType == ResourceType.USER
            || notification.resource?.resourceType == ResourceType.GROUP
        ) {
            handleNameLink(notification)
        }
        //convert date to milliseconds from unix epoch time seconds
        val createdAtText =
            DateHelper.prettyDate(itemView.context, (notification.timestamp ?: 0) * 1000)
        createdAt?.text = createdAtText

        // To avoid accessibility crash, but keep it's functionality
        itemView.contentDescription = "${notification.message} $createdAtText"
    }

    private fun handleNameLink(notification: TsuNotification) {
        val notificationMessage = notification.message ?: return
        if (message == null) return

        var fullName = getUserName(notification)
        val communityName = getGroupName(notification)
        if (communityName.isEmpty() && fullName.isEmpty()) return

        val tokenizedMessage =
            TSUTextTokenizingHelper.normalize(itemView.context, notificationMessage)

        val delimiters = mutableListOf<String>()
        if (fullName.isNotEmpty()) delimiters.add(fullName)
        if (communityName.isNotEmpty()) delimiters.add(communityName)
        notificationMessage.split(*delimiters.toTypedArray()).forEach {
            TSUTextTokenizingHelper.clickable(
                itemView.context,
                tokenizedMessage,
                it,
                TSUTextTokenizingHelper.TsuClickableTextStyle.NORMAL
            ) {
                actions.onNotificationClick(notification)
            }
        }

        TSUTextTokenizingHelper.clickable(itemView.context, tokenizedMessage, fullName) {
            actions.onNotificationProfileClick(notification)
        }
        TSUTextTokenizingHelper.clickable(itemView.context, tokenizedMessage, communityName) {
            actions.onNotificationGroupClick(notification)
        }

        userIcon?.contentDescription = fullName

        if (fullName.isNotEmpty()) {
            notification.extra?.actionUser?.verifiedStatus?.let { status ->
                if (Constants.isVerified(status)) {
                    fullName = fullName.plus(" ")
                    val imageSpan = ImageSpan(message.context, R.drawable.ic_verified_small)
                    tokenizedMessage.setSpan(imageSpan, fullName.length - 1, fullName.length, 0)
                }
            }
        }

        message.text = tokenizedMessage
        message.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun getUserName(notification: TsuNotification): String {
        if (notification.actionUserId != null || notification.resource?.resourceType == ResourceType.USER) {
            notification.markedMessage?.let { message ->
                return message.substringBefore("[/b]").substringAfter("[b]")
            }

            val notificationMessage = notification.message ?: ""
            val messageSplit = notificationMessage.split(" ")
            if (messageSplit.size >= 3) {
                return "${messageSplit[0]} ${messageSplit[1]}"
            }
        }
        return ""
    }

    private fun getGroupName(notification: TsuNotification): String {
        if (notification.resource?.resourceType == ResourceType.GROUP) {
            notification.markedMessage?.let { message ->
                return message.substringAfterLast("[b]").substringBefore("[/b]")
            }

            val notificationMessage = notification.message ?: ""

            return when (notification.notificationType) {
                TsuNotificationType.PENDING_POST_IN_CHANNEL_QUEUE -> {
                    notificationMessage.substringAfter("New pending posts in ").substringBefore(".")
                }
                TsuNotificationType.GROUP_PROMOTION_REQUEST -> {
                    notificationMessage.substringAfter("administer community ").substringBefore(".")
                }
                TsuNotificationType.GROUP_MEMBERSHIP_APPROVAL,
                TsuNotificationType.GROUP_MEMBERSHIP_INVITE,
                TsuNotificationType.GROUP_MEMBERSHIP_REQUEST -> {
                    notificationMessage.substringAfter("join community ").substringBefore(".")
                }
                else -> ""
            }
        }
        return ""
    }

    private fun formatUrl(source: String): String {
        if (source.startsWith("/")) {
            return "${HostProvider.imageHost}${source}".replace("square", "cover")
        }
        return source
    }

}