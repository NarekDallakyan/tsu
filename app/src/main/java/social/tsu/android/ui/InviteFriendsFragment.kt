package social.tsu.android.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.android.synthetic.main.invite_friends_fragment.view.*
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.UserProfile
import social.tsu.android.service.DefaultUserInfoService
import social.tsu.android.service.UserInfoServiceCallback
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import javax.inject.Inject

class InviteFriendsFragment : Fragment(), UserInfoServiceCallback {

    private val REQUEST_PERMISSION_CONTACTS = 100

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: InviteFriendsViewModel

    private val userInfoService by lazy {
        DefaultUserInfoService(activity?.application as TsuApplication, this)
    }
    private lateinit var inviteLink: String
    private lateinit var username: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()
        fragmentComponent.inject(this)

        viewModel = ViewModelProvider(this, viewModelFactory)[InviteFriendsViewModel::class.java]

        return inflater.inflate(R.layout.invite_friends_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.invite_message.text = Firebase.remoteConfig.getString("invite_message")

        viewModel.inviteLink.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Data.Success -> {
                    inviteLink = response.data
                    view.invite_button.isEnabled = ::username.isInitialized
                    view.invite_contact_button.isEnabled = ::username.isInitialized
                    view.progress_bar.hide()

                    view.invite_message.setText(R.string.invite_page_message)
                    view.invite_title.show()
                    view.invite_message.show()
                    view.invite_button.show()
                    view.invite_contact_button.show()
                }
                is Data.Error -> {
                    view.invite_button.isEnabled = false
                    view.invite_contact_button.isEnabled = false
                    view.invite_message.text = getString(R.string.general_error)

                    view.progress_bar.hide()
                    view.invite_message.show()
                    view.invite_button.show()
                    view.invite_contact_button.show()
                    Log.e("INVITE", "Error occurred fetching invite", response.throwable)
                }
                is Data.Loading -> {
                    view.progress_bar.visibility = View.VISIBLE
                }
            }
        })
        if (requireActivity().isInternetAvailable())
            viewModel.fetchInviteLink()
        else
            requireActivity().internetSnack()
        AuthenticationHelper.currentUserId?.let { userInfoService.getUserInfo(it, true) }

        view.invite_button.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.invite_message, username, inviteLink)
                )
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.invite_subject))
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        view.invite_contact_button.setOnClickListener {
            checkPermissions(
                Manifest.permission.READ_CONTACTS,
                REQUEST_PERMISSION_CONTACTS,
                R.string.contact_reason
            ) {
                showContactsScreen()
            }

        }
    }

    private fun showContactsScreen() {
        findNavController().navigate(
            InviteFriendsFragmentDirections.actionInviteFriendsFragmentToInviteContactsFragment(
                inviteLink,
                username
            )
        )
    }

    private fun checkPermissions(
        permission: String,
        requestCode: Int,
        @StringRes messageResource: Int,
        action: () -> Unit
    ) {
        activity?.apply {
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                action()
            } else {

                if (shouldShowRequestPermissionRationale(permission)) {
                    val dialog = AlertDialog.Builder(this)
                        .setMessage(messageResource)
                        .setPositiveButton(
                            R.string.alert_positive
                        ) { dialog, which ->
                            this@InviteFriendsFragment.requestPermissions(
                                arrayOf(permission),
                                requestCode
                            )
                        }
                        .setTitle(R.string.permission_alert_title)
                        .setNegativeButton(R.string.alert_negative) { dialog, which ->
                            snack(getString(messageResource))
                        }
                        .create()

                    dialog.show()
                } else {
                    this@InviteFriendsFragment.requestPermissions(arrayOf(permission), requestCode)
                }
            }

        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CONTACTS && grantResults.isNotEmpty()) {
            if (grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                showContactsScreen()
            }
        }
    }

    fun getMyUserInfo(): UserProfile? {
        return AuthenticationHelper.currentUserId?.let {
            userInfoService.getCachedUserInfo(it)
        }?.run { null }
    }

    override fun completedGetUserInfo(info: UserProfile?) {
        username = info?.username ?: ""
        view?.invite_button?.isEnabled = ::inviteLink.isInitialized
        view?.invite_contact_button?.isEnabled = ::inviteLink.isInitialized
    }

    override fun didErrorWith(message: String) {

    }
}
