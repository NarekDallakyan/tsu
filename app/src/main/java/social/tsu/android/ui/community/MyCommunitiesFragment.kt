package social.tsu.android.ui.community

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.adapters.MembershipActionCallback
import social.tsu.android.adapters.MembershipAdapter
import social.tsu.android.network.model.Group
import social.tsu.android.network.model.Membership
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.post_feed.community.CommunityFeedFragmentDirections
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.utils.snack
import social.tsu.android.viewModel.community.DefaultMyCommunitiesViewModel
import social.tsu.android.viewModel.community.MyCommunitiesViewModel
import social.tsu.android.viewModel.community.MyCommunitiesViewModelCallback

class MyCommunitiesFragment : Fragment(), MyCommunitiesViewModelCallback, MembershipActionCallback,
    CommunityCreateListener {

    private val viewModel: MyCommunitiesViewModel by lazy {
        DefaultMyCommunitiesViewModel(activity?.application as TsuApplication, this)
    }

    private val membershipAdapter: MembershipAdapter by lazy {
        MembershipAdapter(
            activity?.application as TsuApplication,
            this
        )
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoData: AppCompatTextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val model: CommunityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_communities, container, false)

        val viewManager = LinearLayoutManager(context)

        swipeRefreshLayout = view.findViewById(R.id.my_communities_swipe_refresh)
        swipeRefreshLayout.setOnRefreshListener {
            fetchData()
        }

        recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview).apply {
            layoutManager = viewManager
            adapter = membershipAdapter
        }
        tvNoData = view.findViewById(R.id.tvNoData)

        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            viewManager.orientation
        )

        recyclerView.addItemDecoration(dividerItemDecoration)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchData()
        swipeRefreshLayout.isRefreshing = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.communityListeners.add(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        model.communityListeners.remove(this)
        //cleanup viewmodel and services
        viewModel.destroy()
    }

    private fun fetchData() {
        if (requireActivity().isInternetAvailable())
            viewModel.getMyMemberships()
        else
            requireActivity().internetSnack()
    }

    override fun didGetMyMemberships(memberships: List<Membership>) {
        swipeRefreshLayout.isRefreshing = false
        membershipAdapter.updateItems(memberships)
        if (membershipAdapter.itemCount == 0) {
            tvNoData.show()
        } else {
            tvNoData.hide()
        }
    }

    override fun didWithError(message: String) {
        swipeRefreshLayout.isRefreshing = false
        snack(message)
    }

    override fun didAcceptMembership(membershipId: Int) {
        membershipAdapter.acceptMembership(membershipId)
        membershipAdapter.setLoading(membershipId, false)
    }

    override fun didDeclineMembership(membershipId: Int) {
        membershipAdapter.declineMembership(membershipId)
        membershipAdapter.setLoading(membershipId, false)
    }

    override fun didAcceptPromotion(membershipId: Int) {
        membershipAdapter.acceptPromotion(membershipId)
        membershipAdapter.setLoading(membershipId, false)
    }

    override fun didDeclinePromotion(membershipId: Int) {
        membershipAdapter.declinePromotion(membershipId)
        membershipAdapter.setLoading(membershipId, false)
    }

    override fun errorAcceptMembership(membershipId: Int) {
        membershipAdapter.setLoading(membershipId, false)
    }

    override fun errorDeclineMembership(membershipId: Int) {
        membershipAdapter.setLoading(membershipId, false)
    }

    override fun errorAcceptPromotion(membershipId: Int) {
        membershipAdapter.setLoading(membershipId, false)
    }

    override fun errorDeclinePromotion(membershipId: Int) {
        membershipAdapter.setLoading(membershipId, false)
    }

    override fun itemClicked(membership: Membership) {
        Log.d("MuCommunitiesFragment", "show group id ${membership.group.id}")
        val action = CommunityFeedFragmentDirections.openCommunityFeedFragment()
        action.membership = membership
        findNavController().navigate(action)
    }

    override fun acceptMembershipClicked(membership: Membership) {
        if (requireActivity().isInternetAvailable()) {
            viewModel.acceptMembership(membership.id)
            membershipAdapter.setLoading(membership, true)
        } else
            requireActivity().internetSnack()
    }

    override fun acceptPromotionClicked(membership: Membership) {
        if (requireActivity().isInternetAvailable()) {
            viewModel.acceptPromotion(membership.id)
            membershipAdapter.setLoading(membership, true)
        } else
            requireActivity().internetSnack()
    }

    override fun declineMembershipClicked(membership: Membership) {
        if (requireActivity().isInternetAvailable()) {
            viewModel.declineMembership(membership.id)
            membershipAdapter.setLoading(membership, true)
        } else
            requireActivity().internetSnack()
    }

    override fun declinePromotionClicked(membership: Membership) {
        if (requireActivity().isInternetAvailable()) {
            viewModel.declinePromotion(membership.id)
            membershipAdapter.setLoading(membership, true)
        } else
            requireActivity().internetSnack()
    }

    override fun onCommunityCreated() {
        fetchData()
    }

    override fun onCommunityChanged(group: Group) {
        fetchData()
    }

    override fun onCommunityDeleted() {
        fetchData()
    }

}