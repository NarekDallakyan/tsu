package social.tsu.android.ui.community.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.community_members_list.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.model.CommunityMember
import social.tsu.android.network.model.Role
import social.tsu.android.utils.snack
import social.tsu.android.viewModel.community.CommunityMembersViewModel
import javax.inject.Inject


class CommunityMembersFragment : Fragment(), CoroutineScope by MainScope() {

    @Inject
    lateinit var viewModel: CommunityMembersViewModel

    private val adapter = CommunityMembersAdapter(object : CommunityAdminViewCallback {

        override fun onUserClick(member: CommunityMember) {
            findNavController().showUserProfile(member.id)
        }

        override fun onUserKickClick(member: CommunityMember) {
            kickMember(member)
        }

        override fun onUserPromoteClick(member: CommunityMember) {
            viewModel.promoteMember(member)
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.community_members_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        viewModel.communityId = arguments?.getInt("id") ?: 0

        arguments?.getString("role_type")?.let {
            viewModel.userRole = Role.valueOf(it)
        }
        adapter.isAllowedToEdit = viewModel.isAllowedToEdit

        val viewManager = LinearLayoutManager(requireContext())
        community_members_list.adapter = adapter
        community_members_list.layoutManager = viewManager
        community_members_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            private val visibleThreshold = 10
            private var lastVisibleItem = 0
            var totalItemCount = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                totalItemCount = viewManager.itemCount
                lastVisibleItem = viewManager.findLastVisibleItemPosition()

                if (viewModel.canLoadMore && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    viewModel.loadNextPage()
                }
            }
        })

        community_members_swipe_refresh.setOnRefreshListener {
            viewModel.refresh()
        }

        viewModel.userListLiveData.observe(viewLifecycleOwner, Observer {
            launch {
                adapter.submitList(it)
                community_members_swipe_refresh.isRefreshing = false
            }
        })
        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) snack(it)
        })
        viewModel.serviceMessageLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) snack(it)
        })

        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancel()
    }

    private fun kickMember(member: CommunityMember) {
        val localActivity = activity ?: return

        AlertDialog.Builder(localActivity)
            .setMessage(getString(R.string.community_members_kick_msg, member.fullname))
            .setPositiveButton(R.string.community_members_kick) { dialog, _ ->
                viewModel.kickMember(member)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

}