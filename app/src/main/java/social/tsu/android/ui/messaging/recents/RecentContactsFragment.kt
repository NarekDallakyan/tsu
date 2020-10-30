package social.tsu.android.ui.messaging.recents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_recents.*
import social.tsu.android.R
import social.tsu.android.data.local.entity.RecentContact
import social.tsu.android.helper.showUserProfile
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import javax.inject.Inject

class RecentContactsFragment : Fragment(), RecentContactsAdapter.ViewHolderAction {

    private val _adapter = RecentContactsAdapter(this)

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<RecentContactViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_recents, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recent_contacts_swipe_refresh.setOnRefreshListener {
            if (requireActivity().isInternetAvailable())
                viewModel.retry()
            else
                requireActivity().internetSnack()
        }

        recent_contacts_recycler.adapter = _adapter

        viewModel.loadState.observe(viewLifecycleOwner, Observer {
            recent_contacts_swipe_refresh.isRefreshing = it is Data.Loading
        })

        viewModel.myRecentContacts.observe(viewLifecycleOwner, Observer {
            _adapter.submitList(it)
            if (!_adapter.currentList.isNullOrEmpty()) error_textview.hide()
        })

    }

    override fun onResume() {
        super.onResume()
        if (requireActivity().isInternetAvailable())
            viewModel.retry()
        else
            requireActivity().internetSnack()
    }

    private fun handleLoadState(loadState: Data<Boolean>) {
        if (loadState is Data.Error && _adapter.itemCount < 1) {
            error_textview.show()
            error_textview.text = loadState.throwable.message
        } else {
            error_textview.hide()
        }
    }

    override fun onRecentContactClick(contact: RecentContact) {
        findNavController().navigate(R.id.chatFragment, bundleOf("recipient" to contact.otherUser))
    }

    override fun onProfilePicClick(contact: RecentContact) {
        findNavController().showUserProfile(contact.otherUser?.id)
    }

    override fun onRecentContactDelete(contact: RecentContact) {
        val localActivity = activity ?: return

        AlertDialog.Builder(localActivity)
            .setTitle(R.string.recent_delete_dialog_title)
            .setMessage(
                getString(
                    R.string.recent_delete_dialog_message,
                    contact.otherUser?.username
                )
            )
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                viewModel.deleteContact(contact)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

}