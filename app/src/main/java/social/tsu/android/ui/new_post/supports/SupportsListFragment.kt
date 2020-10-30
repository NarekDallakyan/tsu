package social.tsu.android.ui.new_post.supports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.post_supports_list.*
import kotlinx.coroutines.*
import social.tsu.android.R
import social.tsu.android.network.model.UserProfile
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.utils.snack
import javax.inject.Inject


class SupportsListFragment : DaggerFragment(), CoroutineScope by MainScope(), SupportUserCallback {

    @Inject
    lateinit var viewModel: SupportsListViewModel

    private val args: SupportsListFragmentArgs by navArgs()

    private val adapter = SupportsListAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.post_supports_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        supports_list.adapter = adapter

        viewModel.postId = args.postId
        viewModel.userListLiveData.observe(viewLifecycleOwner, observer)

        viewModel.loadDataForPost()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.userListLiveData.removeObserver(observer)
    }

    override fun didUserClick(userProfile: UserProfile) {
        findNavController().navigate(R.id.showUserProfile, bundleOf("id" to userProfile.id))
    }

    private val observer = Observer<Data<List<UserProfile>>> {
        when (it) {
            is Data.Success -> {
                launch {
                    if (this.isActive) {
                        adapter.submitList(it.data)
                        supports_list_progress?.hide()
                        val count = it.data.size
                        supports_list_title?.text = if (count == 1) {
                            getString(R.string.supporters_list_title_one)
                        } else {
                            getString(R.string.supporters_list_title_many, count)
                        }
                    }
                }
            }
            is Data.Error -> {
                snack(it.throwable.message ?: "")
                supports_list_progress?.hide()
                supports_list_title?.text = getString(R.string.supporters_list_title_many, 0)
            }
            is Data.Loading -> supports_list_progress?.show()
        }
    }

}