package social.tsu.android.ui.messaging.tsu_contacts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagedList
import kotlinx.android.synthetic.main.fragment_base_contacts.*
import kotlinx.android.synthetic.main.tsu_contact_item.*
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.data.local.models.TsuContact
import social.tsu.android.helper.showUserProfile
import social.tsu.android.network.api.CommunityApi
import social.tsu.android.network.model.CommunityInviteMemberPayload
import social.tsu.android.service.handleResponse
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.util.RetryCallback
import social.tsu.android.utils.findParentNavController
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.utils.snack
import javax.inject.Inject

abstract class BaseContactsFragment : Fragment(), TsuContactAdapter.ViewHolderAction, RetryCallback {

    private val _contactsAdapter by lazy {
        TsuContactAdapter(
            this,
            this,
            provideActionButtonText()
        )
    }
    private val invitedContacts = hashSetOf<TsuContact>()
    var bundle: Bundle?=null
    @Inject
    lateinit var communityApi: CommunityApi

    @Inject
    lateinit var schedulers: RxSchedulers

    private val contactProfilePic = contact_profile_pic

    private val args by lazy {
        requireParentFragment().navArgs<TsuContactsFragmentArgs>().value
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_base_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tsu_contacts_recycler.adapter = _contactsAdapter

        getContacts().observe(viewLifecycleOwner, Observer(_contactsAdapter::submitList))

        getInitialLoadState().observe(viewLifecycleOwner, Observer(this::handleInitialLoadState))

        getLoadState().observe(viewLifecycleOwner, Observer {
            swipeRefreshLayout.isRefreshing = it is Data.Loading
            _contactsAdapter.setLoadState(it)
        })

        swipeRefreshLayout.setOnRefreshListener { onRefresh() }
    }

    override fun onStart() {
        super.onStart()
        onRefresh()
    }

    abstract fun getContacts(): LiveData<PagedList<TsuContact>>

    abstract fun getLoadState(): LiveData<Data<Boolean>>

    abstract fun getInitialLoadState(): LiveData<Data<Boolean>>

    abstract fun onRefresh()

    private fun provideActionButtonText(): String? {
        return when (args.mode) {
            TsuContactsFragment.ContactsMode.CHAT -> null
            TsuContactsFragment.ContactsMode.COMMUNITY_INVITE -> getString(R.string.invite)
        }
    }

    private fun handleInitialLoadState(loadState: Data<Boolean>) {
        swipeRefreshLayout.isRefreshing = loadState is Data.Loading

        if (loadState is Data.Error) {
            error_textview.show()
            error_textview.text = loadState.throwable.message
        } else {
            error_textview.hide()
        }
    }

    override fun onProfilePicClicked(contact: TsuContact) {
        findNavController().showUserProfile(contact.id)
    }

    override fun onContactClick(contact: TsuContact) {
        when (args.mode) {
            TsuContactsFragment.ContactsMode.CHAT -> {
                findParentNavController().navigate(
                    R.id.chatFragment,
                    bundleOf("recipient" to contact.toPostUser())
                )
            }
            TsuContactsFragment.ContactsMode.COMMUNITY_INVITE -> {
                findNavController().showUserProfile(contact.id)
            }
        }
    }

    override fun onActionButtonClick(contact: TsuContact) {
        when (args.mode) {
            TsuContactsFragment.ContactsMode.CHAT -> {
            }
            TsuContactsFragment.ContactsMode.COMMUNITY_INVITE -> {
                inviteUser(contact)
            }
        }
    }

    private fun inviteUser(contact: TsuContact) {
        val subscribe =
            communityApi.inviteMember(args.communityId, CommunityInviteMemberPayload(contact.id))
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .subscribe({
                    handleResponse(
                        requireActivity(),
                        it,
                        onSuccess = {
                            invitedContacts.add(contact)
                            snack(R.string.invited)
                        },
                        onFailure = {
                            snack(R.string.error_invite)
                        }
                    )
                }, { err ->
                    Log.e(tag, "Error creating community", err)
                    snack(R.string.error_invite)
                })
    }

    override fun isContactInvited(contact: TsuContact): Boolean {
        if (args.mode != TsuContactsFragment.ContactsMode.COMMUNITY_INVITE) return false
        return invitedContacts.contains(contact)
    }

}