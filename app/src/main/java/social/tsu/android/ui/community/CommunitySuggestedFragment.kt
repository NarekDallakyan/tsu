package social.tsu.android.ui.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.fragment_community_suggested.*
import social.tsu.android.NavGraphDirections
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.adapters.SuggestedCommunityAdapter
import social.tsu.android.adapters.SuggestedCommunityListener
import social.tsu.android.network.model.Group
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.setVisibleOrGone
import social.tsu.android.utils.snack
import social.tsu.android.viewModel.community.CommunitySuggestedViewModel
import social.tsu.android.viewModel.community.CommunitySuggestedViewModelCallback
import social.tsu.android.viewModel.community.DefaultCommunitySuggestedViewModel

class CommunitySuggestedFragment : Fragment(), CommunitySuggestedViewModelCallback,
    SuggestedCommunityListener {

    private val viewModel: CommunitySuggestedViewModel by lazy {
        DefaultCommunitySuggestedViewModel(activity?.application as TsuApplication, this)
    }

    private lateinit var communitiesRecycler: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var errorTextView: TextView
    private lateinit var adapter: SuggestedCommunityAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_community_suggested, container, false)

        adapter = SuggestedCommunityAdapter(requireContext().applicationContext, this)

        communitiesRecycler = view.findViewById(R.id.recyclerView)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        errorTextView = view.findViewById(R.id.errorTextView)
        communitiesRecycler.layoutManager = LinearLayoutManager(context)
        communitiesRecycler.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        communitiesRecycler.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            if (requireActivity().isInternetAvailable())
                viewModel.loadSuggestedCommunities()
            else
                requireActivity().internetSnack()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (requireActivity().isInternetAvailable()) {
            swipeRefreshLayout.isRefreshing = true
            viewModel.loadSuggestedCommunities()
        } else
            requireActivity().internetSnack()
    }

    override fun onCommunityClicked(group: Group) {
        val action = NavGraphDirections.openCommunityFeedFragment()
        action.group = group
        findNavController().navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    override fun didLoadCommunitySuggested(communities: List<Group>) {
        adapter.setData(communities)
        swipeRefreshLayout.isRefreshing = false
        recyclerView?.setVisibleOrGone(!communities.isEmpty())
        errorTextView.setVisibleOrGone(communities.isEmpty())
        if (communities.isEmpty()) {
            errorTextView.setText(R.string.community_suggested_empty)
        }
    }

    override fun didFailCommunitySuggested(message: String) {
        swipeRefreshLayout.isRefreshing = false
        recyclerView.setVisibleOrGone(false)
        errorTextView.setVisibleOrGone(true)
        errorTextView.setText(R.string.community_suggested_empty)
    }

    override fun onJoinClicked(group: Group) {
        if (requireActivity().isInternetAvailable())
            viewModel.joinCommunity(group)
        else
            requireActivity().internetSnack()
    }

    override fun failedToJoin(group: Group) {
        adapter.unjoinComuunity(group)
    }

    override fun didJoinCommunity(group: Group) {

    }

    override fun didErrorWith(message: String) {
        snack(message)
        swipeRefreshLayout.isRefreshing = false
        recyclerView.setVisibleOrGone(false)
        errorTextView.setText(R.string.community_suggested_empty)
        errorTextView.setVisibleOrGone(true)
    }
}