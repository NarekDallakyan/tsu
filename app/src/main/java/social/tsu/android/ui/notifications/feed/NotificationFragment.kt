package social.tsu.android.ui.notifications.feed


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.AndroidSupportInjection
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import social.tsu.android.R
import social.tsu.android.data.local.entity.TsuNotification
import social.tsu.android.data.local.entity.TsuNotificationCategory
import social.tsu.android.data.local.models.PostUser
import social.tsu.android.data.local.models.TsuNotificationType
import social.tsu.android.helper.showUserProfile
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.util.RecyclerSectionItemDecoration
import social.tsu.android.ui.util.RetryCallback
import social.tsu.android.ui.util.TutorialDialog
import java.util.*
import javax.inject.Inject


class NotificationFragment : Fragment(), NotificationAdapter.ViewHolderActions, RetryCallback {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<NotificationsViewModel> { viewModelFactory }

    private val notificationAdapter: NotificationAdapter by lazy {
        NotificationAdapter(
            viewLifecycleOwner,
            this, this
        )
    }

    private val sectionCallback = object : RecyclerSectionItemDecoration.SectionCallback {
        override fun isSection(position: Int): Boolean {
            if (position == 0) {
                return true
            }
            val currentItemDate =
                (notificationAdapter.currentList?.get(position)?.timestamp ?: 0).toLocalDateTime()
            val previousItemDate = (notificationAdapter.currentList?.get(position - 1)?.timestamp
                ?: 0).toLocalDateTime()
            return currentItemDate.extractSectionHeader() != previousItemDate.extractSectionHeader()
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return (notificationAdapter.currentList?.get(position)?.timestamp
                ?: 0).toLocalDateTime().extractSectionHeader()
        }

        private fun Long.toLocalDateTime(): LocalDate {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneId.systemDefault())
                .toLocalDate()
        }

        private fun LocalDate.extractSectionHeader(): String {
            val currentCalendar = Calendar.getInstance()
            val targetCalendar = Calendar.getInstance()
            targetCalendar.timeInMillis =
                atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return if (this == LocalDate.now(ZoneId.systemDefault())) {
                getString(R.string.today)
            } else if (currentCalendar[Calendar.WEEK_OF_YEAR] == targetCalendar[Calendar.WEEK_OF_YEAR] &&
                currentCalendar[Calendar.YEAR] == targetCalendar[Calendar.YEAR]
            ) {
                getString(R.string.this_week)
            } else {
                getString(R.string.previous)
            }
        }
    }

    override fun didTapOnAccept(item: TsuNotification): LiveData<Data<Boolean>> {
        if (requireActivity().isInternetAvailable().not()) {
            requireActivity().internetSnack()
        }
        return viewModel.acceptFriend(item)
    }

    override fun didTapOnDecline(item: TsuNotification): LiveData<Data<Boolean>> {
        return viewModel.declineFriend(item)
    }

    override fun onNotificationClick(item: TsuNotification) {
        viewModel.markAsRead(item)

        when (item.notificationType) {
            TsuNotificationType.NEW_COMMENT_ON_POST,
            TsuNotificationType.NEW_COMMENT_ON_POST_YOU_COMMENTED_ON,
            TsuNotificationType.NEW_LIKE_ON_COMMENT -> {
                item.resource?.id?.let {
                    findNavController().navigate(
                        R.id.commentsFragment,
                        bundleOf("postId" to it.toLongOrNull())
                    )
                }
            }
            TsuNotificationType.NEW_LIKE_ON_POST,
            TsuNotificationType.SOMEONE_SHARED_YOUR_POST,
            TsuNotificationType.NEW_POST_ON_YOUR_WALL,
            TsuNotificationType.SOMEONE_MENTIONED_YOU_IN_A_POST,
            TsuNotificationType.GROUP_POST_APPROVED,
            TsuNotificationType.NEW_POST_IN_GROUP -> {
                item.resource?.id?.let {
                    findNavController().navigate(
                        R.id.singlePostFragment,
                        bundleOf("postId" to it.toLongOrNull())
                    )
                }
            }
            TsuNotificationType.VIDEO_UPLOAD_COMPLETE -> {
                item.resource?.id?.let {
                    findNavController().navigate(R.id.mainFeedFragment)
                }
            }
            TsuNotificationType.DONATION_RECEIVED,
            TsuNotificationType.ROYALTY_EARNED -> {
                findNavController().navigate(R.id.redeemFragment)
            }
            TsuNotificationType.NEW_FOLLOWER,
            TsuNotificationType.SOMEONE_JOINED_YOUR_NETWORK,
            TsuNotificationType.SOMEONE_YOU_MAY_KNOW -> {
                item.resource?.id?.let {
                    findNavController().showUserProfile(it.toIntOrNull())
                }
            }
            TsuNotificationType.NEW_PARENT_POST -> {
                item.resource?.type?.let { resource ->
                    item.resource?.id?.let {
                        when (resource) {
                            "user" -> findNavController().navigate(
                                R.id.showUserProfile,
                                bundleOf("id" to it.toIntOrNull())
                            )
                            "post" -> findNavController().navigate(
                                R.id.singlePostFragment,
                                bundleOf("postId" to it.toLongOrNull())
                            )
                        }

                    }
                }
            }
            TsuNotificationType.NEW_DIRECT_MESSAGE -> {
                item.resource?.parameters?.userId?.let {
                    val params = item.resource!!.parameters!!
                    findNavController().navigate(
                        R.id.chatFragment,
                        bundleOf(
                            "recipient" to PostUser(
                                it,
                                params.username,
                                params.fullName,
                                item.pictureUrl
                            )
                        )
                    )
                }
            }
            TsuNotificationType.GROUP_MEMBERSHIP_REQUEST,
            TsuNotificationType.GROUP_MEMBERSHIP_INVITE,
            TsuNotificationType.GROUP_MEMBERSHIP_APPROVAL,
            TsuNotificationType.GROUP_PROMOTION_RESPONSE,
            TsuNotificationType.GROUP_PROMOTION_REQUEST -> {
                findNavController().navigate(R.id.communityFragment)
            }

            TsuNotificationType.PENDING_POST_IN_CHANNEL_QUEUE -> {
                item.resource?.id?.let {
                    val groupId = it.toIntOrNull() ?: 0
                    findNavController().navigate(
                        R.id.action_global_open_communityPublishingRequestsFragment,
                        bundleOf("groupId" to groupId)
                    )
                }
            }
        }
    }

    override fun onNotificationProfileClick(item: TsuNotification) {

        when {
            item.categoryType == TsuNotificationCategory.FRIEND_REQUESTS -> {
                item.resource?.id?.let {
                    findNavController().showUserProfile(it.toIntOrNull())
                }
            }
            item.actionUserId != null -> {
                findNavController().showUserProfile(item.actionUserId?.toInt())
            }
            else -> {
                onNotificationClick(item)
            }
        }
    }

    override fun onNotificationGroupClick(item: TsuNotification) {
        item.resource?.id?.let { groupId ->
            findNavController().navigate(
                R.id.communityFeedFragment,
                bundleOf("groupId" to groupId.toInt())
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(
            R.layout.notification,
            container, false
        )
        val viewManager = LinearLayoutManager(context)

        val sectionItemDecoration = RecyclerSectionItemDecoration(
            resources.getDimensionPixelSize(R.dimen.header),
            false,
            resources.getDimensionPixelSize(R.dimen.margin_default_small),
            sectionCallback
        )

        view.findViewById<RecyclerView>(R.id.notifications).apply {
            layoutManager = viewManager
            adapter = notificationAdapter
            addItemDecoration(sectionItemDecoration)
        }

        if (requireActivity().isInternetAvailable()) {
            viewModel.loadState.observe(
                viewLifecycleOwner,
                Observer(notificationAdapter::setLoadState)
            )
        } else
            requireActivity().internetSnack()

        viewModel.myNotifications.observe(viewLifecycleOwner, Observer {
            notificationAdapter.submitList(it)
            viewModel.markSeen()
        })

        TutorialDialog
            .Builder()
            .image(R.drawable.swipe_tutorial)
            .title(getString(R.string.swipe_tutorial_title))
            .key("notifications_swipe")
            .build()
            .show(parentFragmentManager, null)

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.findViewById<View>(R.id.ic_notify)?.isEnabled = false
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.findViewById<View>(R.id.ic_notify)?.isEnabled = true
    }

    override fun retry() {
        if (requireActivity().isInternetAvailable())
            viewModel.retryNotificationFetch()
        else
            requireActivity().internetSnack()
    }

}


