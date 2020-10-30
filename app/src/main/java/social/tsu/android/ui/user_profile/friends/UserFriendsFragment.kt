package social.tsu.android.ui.user_profile.friends

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.users_list.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import social.tsu.android.R
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.model.UserProfile
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.findParentNavController
import social.tsu.android.utils.snack
import javax.inject.Inject


class UserFriendsFragment : DaggerFragment(), CoroutineScope by MainScope() {

    @Inject
    lateinit var viewModel: UserFriendsViewModel

    private lateinit var viewManager: LinearLayoutManager
    private var lastUpdateJob: Job? = null

    private var firstVisible = -1
    private var lastVisible = -1

    private val adapter = UserFriendListAdapter(object : UserListCallback {
        override fun onAddFriendClick(userProfile: UserProfile) {
            if (requireActivity().isInternetAvailable())
                viewModel.requestFriend(userProfile)
            else
                requireActivity().internetSnack()
        }

        override fun didUserClick(userProfile: UserProfile) {
            findNavController().showUserProfile(userProfile.id)
        }

        override fun onShowOptionsClick(userProfile: UserProfile) {
            showUserFriendOptions(userProfile)
        }

        override fun onFollowClick(userProfile: UserProfile) {
            if (requireActivity().isInternetAvailable())
                viewModel.toggleFollowingForUser(userProfile)
            else
                requireActivity().internetSnack()
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.users_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getInt("id") ?: UserProfile.NO_USER_ID
        viewModel.userId = userId

        val listTypeId = arguments?.getInt("list_type") ?: 0
        viewModel.listType = UserFriendListType.values()[listTypeId]

        adapter.type = viewModel.listType
        adapter.isCurrentUser = userId == AuthenticationHelper.currentUserId

        user_list_swipe_refresh.setOnRefreshListener {
            viewModel.reloadList()
        }

        viewManager = LinearLayoutManager(context)
        user_list.adapter = adapter
        user_list.layoutManager = viewManager
        user_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            private val visibleThreshold = 10
            private var lastVisibleItem = 0
            var totalItemCount = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                totalItemCount = viewManager.itemCount
                lastVisibleItem = viewManager.findLastVisibleItemPosition()

                val swipeRefresh = user_list_swipe_refresh ?: return
                if (!swipeRefresh.isRefreshing && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    if (viewModel.hasMoreItems) {
                        swipeRefresh.isRefreshing = true
                        viewModel.loadNextItems()
                    } else {
                        Log.d("UserFriends", "Not triggering loadmore as next_page is null")
                    }

                }
            }
        })

        viewModel.userListLiveData.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Data.Success -> {
                    lastUpdateJob?.cancel()
                    lastUpdateJob = launch {
                        adapter.submitList(it.data)
                        //can be executed after fragment view is destroyed, so safe call required
                        user_list_swipe_refresh?.isRefreshing = false
                    }
                }
                is Data.Error -> {
                    snack(it.throwable.message ?: "")
                    user_list_swipe_refresh?.isRefreshing = false
                }
                is Data.Loading -> user_list_swipe_refresh?.isRefreshing = true
            }
        })
        viewModel.requestErrorLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) snack(it)
        })

    }

    override fun onResume() {
        super.onResume()
        if (adapter.itemCount == 0) {
            viewModel.loadNextItems()
        } else {
            // To update last opened profile
            viewModel.reloadItems(firstVisible, lastVisible)
        }
    }

    override fun onPause() {
        super.onPause()
        firstVisible = viewManager.findFirstVisibleItemPosition()
        lastVisible = viewManager.findLastVisibleItemPosition()
    }

    private fun showUserFriendOptions(userProfile: UserProfile) {
        val activity = this.activity ?: return

        val sheetView = activity.layoutInflater.inflate(
            R.layout.dialog_bottom_user_friend_actions, null
        ) ?: return
        val actionSheet = BottomSheetDialog(context as Context, R.style.TSUBottomDialog)
        actionSheet.setContentView(sheetView)
        actionSheet.show()

        sheetView.findViewById<View>(R.id.dialog_cancel_button)?.setOnClickListener {
            actionSheet.dismiss()
        }

        sheetView.findViewById<TextView>(R.id.dialog_message_button)?.let { messageBtn ->
            messageBtn.text = getString(R.string.dialog_message_btn, userProfile.fullName)
            messageBtn.setOnClickListener {
                findParentNavController().navigate(
                    R.id.chatFragment,
                    bundleOf("recipient" to userProfile.toPostUser())
                )
                actionSheet.dismiss()
            }
        }

        sheetView.findViewById<TextView>(R.id.dialog_follow_button)?.let { messageBtn ->
            messageBtn.text = if (userProfile.isFollowing) {
                getString(R.string.dialog_unfollow_btn, userProfile.fullName)
            } else {
                getString(R.string.dialog_follow_btn, userProfile.fullName)
            }
            messageBtn.setOnClickListener {
                viewModel.toggleFollowingForUser(userProfile)
                actionSheet.dismiss()
            }
        }

        sheetView.findViewById<TextView>(R.id.dialog_unfriend_button)?.let { messageBtn ->
            messageBtn.text = if (userProfile.isFriend) {
                getString(R.string.dialog_unfriend_btn, userProfile.fullName)
            } else {
                getString(R.string.dialog_friend_btn, userProfile.fullName)
            }
            messageBtn.setOnClickListener {
                viewModel.unfriendUser(userProfile)
                actionSheet.dismiss()
            }
        }

        sheetView.findViewById<TextView>(R.id.dialog_block_button)?.let { messageBtn ->
            messageBtn.text = if (userProfile.isBlocked) {
                getString(R.string.dialog_unblock_btn, userProfile.fullName)
            } else {
                getString(R.string.dialog_block_btn, userProfile.fullName)
            }
            messageBtn.setOnClickListener {
                viewModel.toggleBlockForUser(userProfile)
                actionSheet.dismiss()
            }
        }
    }

}