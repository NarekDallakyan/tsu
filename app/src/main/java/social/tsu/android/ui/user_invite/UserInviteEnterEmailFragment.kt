package social.tsu.android.ui.user_invite

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_user_invite_enter_email.*
import kotlinx.android.synthetic.main.progress_layout.*
import social.tsu.android.R
import social.tsu.android.data.local.models.OldUserDetails
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.*
import social.tsu.android.validation.ValidatedTextField
import social.tsu.android.validation.ensureNotBlank
import social.tsu.android.validation.ensureValidEmail
import javax.inject.Inject

class UserInviteEnterEmailFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<UserInviteViewModel> { viewModelFactory }

    private val args by navArgs<UserInviteEnterEmailFragmentArgs>()

    private var argsEmail: String? = null
    private var argsInvitedById: Int = 0
    private var argsHubspotId: Int = 0
    private var argsVerificationCode: Int = 0
    private var argsInvitedByUsername: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setArgs()
        return inflater.inflate(R.layout.fragment_user_invite_enter_email, container, false)
    }

    private fun setArgs() {
        argsEmail = args.email
        argsHubspotId = args.hubspotId
        argsInvitedById = args.invitedBy
        argsVerificationCode = args.verificationCode
        argsInvitedByUsername = args.invitedByUsername
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        args.email?.let {
            email_edittext.setText(it)
        }

        // disable back event if back goes to main feed fragment
        if ((activity as? MainActivity)?.isPreviousFragmentNavStartFragment() == true || AuthenticationHelper.currentUserId == null) {
            exitAppOnBackPressed()
        }

        request_old_user_details_button.setOnClickListener { onContinueClick() }

        verify_button.setOnClickListener { onVerifyClick() }

    }

    override fun onResume() {
        super.onResume()
        activity?.run {
            FirebaseDynamicLinks.getInstance().getDynamicLink(intent).addOnCompleteListener {
                if (it.isSuccessful) {
                    it.result?.link?.let { link -> extractAndSetData(link) }
                    Log.e("INVITATION2", it.result?.link.toString())
                }
            }
        }
        (activity as? MainActivity)?.setupTopLevelConfigurations(R.id.oldUserEnterEmailFragment)
    }

    private fun extractAndSetData(data: Uri) {
        val queryInvitedBy = data.getQueryParameter("invited_by_id")
        queryInvitedBy?.let { argsInvitedById = it.toIntOrNull() ?: 0 }

        val queryHubspotId = data.getQueryParameter("hubspot_id")
        queryHubspotId?.let { argsHubspotId = it.toIntOrNull() ?: 0 }

        val queryEmail = data.getQueryParameter("email")
        queryEmail?.let {
            argsEmail = it
            email_edittext?.setText(it)
        }

        val queryVerificationCode = data.getQueryParameter("verification_code")
        queryVerificationCode?.let {
            argsVerificationCode = it.toIntOrNull() ?: 0
        }

        val queryInvitedByUsername = data.getQueryParameter("invited_by_username")
        queryInvitedByUsername?.let { argsInvitedByUsername = it }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.support_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_support) {
            dismissKeyboard()
            findNavController().navigate(R.id.support)
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun onContinueClick() {
        resetErrorMessage()

        //validate email field
        val fields = listOf(
            ValidatedTextField.validateField(email_edittext, email_til, ensureValidEmail)
        )

        val errorOccurred = fields.any { it }

        if (errorOccurred) {
            return
        }

        dismissKeyboard()

        viewModel.email = email_edittext.text.toString().trim()

        verifyUserInvite(viewModel.email)

    }

    private fun onVerifyClick() {
        resetErrorMessage()

        //validate email field
        val fields = listOf(
            ValidatedTextField.validateField(
                verification_code_edittext,
                verification_code_til,
                ensureNotBlank
            )
        )

        val errorOccurred = fields.any { it }

        if (errorOccurred) {
            return
        }

        dismissKeyboard()

        val verificationCode = verification_code_edittext.text.toString().trim()

        verifyOldUser(viewModel.email, verificationCode.toInt())

    }

    private fun verifyUserInvite(email: String) {
        viewModel.verifyUserInvite(email).observe(viewLifecycleOwner, Observer {

            progress_layout.visibility = if (it is Data.Loading) View.VISIBLE else View.GONE
            request_old_user_details_button.isEnabled = it !is Data.Loading

            when (it) {
                is Data.Success -> {

                    when {
                        it.data.emailInUse == true -> {
                            findNavController().navigate(
                                UserInviteEnterEmailFragmentDirections.actionOldUserEnterEmailFragmentToLoginFragment()
                                    .apply {
                                        this.email = email
                                    })
                        }
                        it.data.hubspotId != null -> {
                            findNavController().navigate(
                                UserInviteEnterEmailFragmentDirections.actionOldUserEnterEmailFragmentToCreateAccountFragment()
                                    .apply {
                                        hubspotId = it.data.hubspotId!!
                                        this.email = email
                                    })
                        }
                        it.data.invitedByUsername != null -> {
                            findNavController().navigate(
                                UserInviteEnterEmailFragmentDirections.actionOldUserEnterEmailFragmentToCreateAccountFragment()
                                    .apply {
                                        invitedByUsername = it.data.invitedByUsername
                                        this.email = email
                                    })
                        }
                        it.data.oldTsuUser != null -> {
                            if (argsVerificationCode > 0) {
                                enter_email_layout?.hide()
                                enter_verification_layout?.show()
                                verification_code_edittext.setText(
                                    argsVerificationCode.toString().trim()
                                )
                            } else {
                                findNavController().navigate(
                                    UserInviteEnterEmailFragmentDirections.actionOldUserEnterEmailFragmentToCreateAccountFragment()
                                        .apply {
                                            oldUserDetails = OldUserDetails(email = email)
                                        })
                            }
                        }
                        argsInvitedById > 0 || argsHubspotId > 0 -> {
                            findNavController().navigate(
                                UserInviteEnterEmailFragmentDirections.actionOldUserEnterEmailFragmentToCreateAccountFragment()
                                    .apply {
                                        invitedBy = argsInvitedById
                                        hubspotId = argsHubspotId
                                        this.email = email
                                    })
                        }
                        else -> {
                            findNavController().navigate(
                                UserInviteEnterEmailFragmentDirections.actionOldUserEnterEmailFragmentToCreateAccountFragment()
                                    .apply {
                                        invitedByUsername = argsInvitedByUsername ?: ""
                                        this.email = email
                                    })
                        }
                    }
                }

                is Data.Error -> {

                    when {
                        argsInvitedById > 0 || argsHubspotId > 0 -> {
                            findNavController().navigate(
                                UserInviteEnterEmailFragmentDirections.actionOldUserEnterEmailFragmentToCreateAccountFragment()
                                    .apply {
                                        invitedBy = argsInvitedById
                                        hubspotId = argsHubspotId
                                        this.email = email
                                    })
                        }
                        else -> {
                            handleError(it)
                        }
                    }
                }
            }

        })
    }

    private var verificationTryCounts = 0

    private fun verifyOldUser(email: String, verificationCode: Int) {

        if (verificationTryCounts >= MAX_VERIFICATION_TRY_COUNT) {
            snack(getString(R.string.max_verification_tries_msg))
            return
        }

        if (requireActivity().isInternetAvailable()) {
            viewModel.verifyOldUser(email, verificationCode).observe(viewLifecycleOwner, Observer {

                progress_layout.visibility = if (it is Data.Loading) View.VISIBLE else View.GONE
                verify_button?.isEnabled = it !is Data.Loading

                when (it) {
                    is Data.Success -> {
                        findNavController().navigate(
                            UserInviteEnterEmailFragmentDirections.actionOldUserEnterEmailFragmentToCreateAccountFragment()
                                .apply {
                                    oldUserDetails = it.data
                                })
                    }

                    is Data.Error -> {
                        handleError(it)
                        verificationTryCounts++
                    }
                }

            })
        } else
            requireActivity().internetSnack()
    }


    private fun handleError(data: Data.Error<Any>) {
        data.throwable.message?.let { msg ->
            snack(msg)
        }
    }

    private fun resetErrorMessage() {
        email_til.error = null
        verification_code_til.error = null
    }

    companion object {
        private const val MAX_VERIFICATION_TRY_COUNT = 3
    }
}