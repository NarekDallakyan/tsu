package social.tsu.android.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.model.Group
import social.tsu.android.network.model.PendingRequest
import social.tsu.android.ui.community.members.PendingMembershipAdapter
import social.tsu.android.ui.community.members.PendingRequestsActionCallback
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.setVisibleOrGone
import social.tsu.android.utils.snack
import social.tsu.android.viewModel.community.DefaultPendingMembershipViewModel
import social.tsu.android.viewModel.community.PendingMembershipViewModel
import social.tsu.android.viewModel.community.PendingMembershipViewModelCallback

class PendingMembershipFragment : Fragment(), PendingRequestsActionCallback,
    PendingMembershipViewModelCallback {

    private lateinit var community: Group

    private val args by navArgs<PendingMembershipFragmentArgs>()

    private val viewModel: PendingMembershipViewModel by lazy {
        DefaultPendingMembershipViewModel(activity?.application as TsuApplication, this)
    }

    private val pendingAdapter: PendingMembershipAdapter by lazy {
        PendingMembershipAdapter(this)
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.community_pendings_list, container, false)

        val viewManager = LinearLayoutManager(context)

        community = args.community

        recyclerView = view.findViewById<RecyclerView>(R.id.community_pending_list).apply {
            layoutManager = viewManager
            adapter = pendingAdapter
        }

        emptyText = view.findViewById<TextView>(R.id.empty_text)

        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            viewManager.orientation
        )

        recyclerView.addItemDecoration(dividerItemDecoration)
        fetchData()
        return view
    }

    private fun fetchData() {
        if (requireActivity().isInternetAvailable())
            viewModel.getPendingMemberships(community.id)
        else
            requireActivity().internetSnack()
    }


    override fun acceptClicked(pendingRequest: PendingRequest) {
        if (requireActivity().isInternetAvailable())
            viewModel.acceptPendingMembership(pendingRequest)
        else
            requireActivity().internetSnack()
    }

    override fun denyClicked(pendingRequest: PendingRequest) {
        if (requireActivity().isInternetAvailable())
            viewModel.denyPendingMembership(pendingRequest)
        else
            requireActivity().internetSnack()
    }

    override fun itemClicked(pendingRequest: PendingRequest) {
        findNavController().showUserProfile(pendingRequest.id)
    }

    override fun didGetPendingMemberships(memberships: List<PendingRequest>) {
        pendingAdapter.updateItems(memberships)
        emptyText.setVisibleOrGone(memberships.isEmpty())
    }

    override fun didWithError(message: String) {
        snack(message)
    }

    override fun didAcceptMembership(pendingId: Int) {
        pendingAdapter.remove(pendingId)
        emptyText.setVisibleOrGone(pendingAdapter.itemCount == 0)
    }

    override fun didDeclineMembership(pendingId: Int) {
        pendingAdapter.remove(pendingId)
        emptyText.setVisibleOrGone(pendingAdapter.itemCount == 0)
    }

}