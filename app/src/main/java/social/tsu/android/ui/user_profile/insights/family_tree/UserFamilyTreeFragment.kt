package social.tsu.android.ui.user_profile.insights.family_tree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_user_family_tree.*
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.model.UserProfile
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.utils.snack
import social.tsu.android.viewModel.userProfile.UserFamilyTreeViewModel
import javax.inject.Inject

class UserFamilyTreeFragment : Fragment() {

    private val TAG = UserFamilyTreeFragment::class.java.simpleName

    companion object {
        fun newInstance() = UserFamilyTreeFragment()
    }

    @Inject
    lateinit var viewModel: UserFamilyTreeViewModel

    private val adapter =
        UserFamilyTreeAdapter(
            object :
                UserFamilyTreeUserCallback {
                override fun onUserClick(userProfile: UserProfile) {
                    findNavController().showUserProfile(userProfile.id)
                }
            })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_family_tree, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        fragment_user_family_tree_list.adapter = adapter
        fragment_user_family_tree_list.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {

            private val visibleThreshold = 5
            private var lastVisibleItem = 0
            var totalItemCount = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
                totalItemCount = linearLayoutManager.itemCount
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition()
                if (!adapter.isLoading() && !fragment_user_family_tree_swipe.isRefreshing
                    && totalItemCount <= (lastVisibleItem + visibleThreshold)
                ) {
                    if (viewModel.hasNext()) {
                        if (requireActivity().isInternetAvailable()) {
                            adapter.setLoading(true)
                            viewModel.loadNextFamilyTreePage()
                        } else
                            requireActivity().internetSnack()
                    }
                }
            }

        })

        fragment_user_family_tree_swipe.setOnRefreshListener {
            if (requireActivity().isInternetAvailable()) {
                adapter.resetList()
                viewModel.loadFamilyTree()
            } else {
                requireActivity().internetSnack()
            }
        }

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            adapter.setLoading(false)
            fragment_user_family_tree_swipe.isRefreshing = false
            if (it != null) snack(it)
        })

        viewModel.familyTreeLiveData.observe(viewLifecycleOwner, Observer {
            adapter.setLoading(false)
            fragment_user_family_tree_swipe.isRefreshing = false
            adapter.submitList(it)
        })
        if (requireActivity().isInternetAvailable()) {
            fragment_user_family_tree_swipe.isRefreshing = true
            adapter.resetList()
            viewModel.loadFamilyTree()
        } else
            requireActivity().internetSnack()
    }
}