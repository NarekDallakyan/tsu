package social.tsu.android.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.adapters.ContactItemsAdapter
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.ui.model.Data
import javax.inject.Inject


class InviteContactsFragment : Fragment() {
    companion object {
        const val MAILTO = "mailto:"
        const val SMS = "sms:"
        const val EXTRA_ADDRESS = "address"
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: InviteContactsViewModel

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper
    private var friendsInvited = 0
    val args by navArgs<InviteContactsFragmentArgs>()
    val properties = HashMap<String, Any?>()

    private lateinit var invitedContacts: List<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        viewModel = ViewModelProvider(this, viewModelFactory)[InviteContactsViewModel::class.java]

        return inflater.inflate(R.layout.invite_contacts_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.contactList)

        val contactItemsAdapter = ContactItemsAdapter(R.layout.invite_contacts_list_item, viewModel)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        recyclerView.adapter = contactItemsAdapter

        viewModel.queryContacts()
        if (requireActivity().isInternetAvailable())
            viewModel.getInvitedList()
        else
            requireActivity().internetSnack()

        viewModel.invitedContacts.observe(viewLifecycleOwner, Observer {
            invitedContacts = when (it) {
                is Data.Success -> it.data
                else -> emptyList()
            }
        })

        viewModel.contacts.observe(viewLifecycleOwner, Observer { contacts ->
            contactItemsAdapter.updateContacts(contacts)
            contactItemsAdapter.notifyDataSetChanged()
        })

        viewModel.selectedContact.observe(viewLifecycleOwner, Observer { contactItem ->

            if (contactItem.contactMethods.size == 1) {
                inviteContact(contactItem, contactItem.contactMethods[0])
            } else {
                val sheetView = layoutInflater.inflate(R.layout.invite_contact_bottom_sheet, null)
                val actionSheet = BottomSheetDialog(requireContext())
                actionSheet.setContentView(sheetView)

                val methodHolder = sheetView.findViewById<LinearLayout>(R.id.contact_methods)
                val layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                contactItem.contactMethods.forEach { method ->
                    val inviteButton = Button(requireContext())

                    when (method) {
                        is ContactRepository.ContactMethod.Email -> {
                            inviteButton.text = method.emailAddress
                            inviteButton.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_email,
                                0,
                                0,
                                0
                            )
                        }

                        is ContactRepository.ContactMethod.Phone -> {
                            inviteButton.text = method.number
                            inviteButton.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_sms,
                                0,
                                0,
                                0
                            )
                        }
                    }

                    inviteButton.setOnClickListener {
                        inviteContact(contactItem, method)
                    }

                    methodHolder.addView(inviteButton, layoutParams)
                }

                actionSheet.show()
            }
        })
    }

    private fun inviteContact(
        contactItem: ContactRepository.ContactItem,
        method: ContactRepository.ContactMethod
    ) {
        when (method) {
            is ContactRepository.ContactMethod.Email -> {
                composeEmail(method.emailAddress, args.inviteLink, args.username)
            }
            is ContactRepository.ContactMethod.Phone -> {
                composeTextMessage(method.number, args.inviteLink, args.username)
            }
        }

        viewModel.markInvited(contactItem)
    }

    fun composeEmail(emailAddress: String, inviteLink: String, username: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(MAILTO)
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.invite_subject))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_message, username, inviteLink))
        }

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            properties["type"] = "email"
            properties["count"] = invitedContacts.size
            analyticsHelper.logEvent("friends_invited", properties)
            startActivity(intent)
        }

    }

    fun composeTextMessage(phoneNumber: String, inviteLink: String, username: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(SMS)
            putExtra(EXTRA_ADDRESS, phoneNumber)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.invite_subject))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_message, username, inviteLink))
        }

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            properties["type"] = "SMS"
            properties["count"] = invitedContacts.size
            analyticsHelper.logEvent("friends_invited", properties)
            startActivity(intent)
        }


    }
}
