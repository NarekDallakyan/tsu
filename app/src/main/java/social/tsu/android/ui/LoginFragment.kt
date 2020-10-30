package social.tsu.android.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.create_account.password
import kotlinx.android.synthetic.main.create_account.password_layout
import kotlinx.android.synthetic.main.create_account.username
import kotlinx.android.synthetic.main.create_account.username_layout
import kotlinx.android.synthetic.main.login.*
import social.tsu.android.R
import social.tsu.android.appComponent
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.model.CreateAccountResponse
import social.tsu.android.network.model.LoginRequest
import social.tsu.android.service.AuthenticationService
import social.tsu.android.utils.*
import social.tsu.android.validation.ValidatedTextField
import social.tsu.android.validation.ensureNotBlank
import javax.inject.Inject

class LoginFragment : Fragment() {

    private var btnLogin: Button? = null

    @Inject
    lateinit var authenticationService: AuthenticationService

    val args by navArgs<LoginFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.login,
            container, false
        )
        btnLogin = view.findViewById<Button>(R.id.login)
        view.findViewById<Button>(R.id.login).setOnClickListener {
            handlLogin()
        }

        view.findViewById<Button>(R.id.forgot_password).setOnClickListener {
            findNavController().navigate(R.id.enterEmailFragment)
        }

        val fragmentComponent = requireActivity().application
            .appComponent().fragmentComponent().create()

        fragmentComponent.inject(this)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        args.email?.let {
            username.setText(it)
            username.isEnabled = false
            username_layout.endIconMode = TextInputLayout.END_ICON_NONE
        }
        // disable back event if back goes to main feed fragment
        if ((activity as? MainActivity)?.isPreviousFragmentNavStartFragment() == true) {
            exitAppOnBackPressed()
        }
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

    override fun onStart() {
        super.onStart()
        if (hasStoredCredentials()) {
            val creds = fetchCredentials()
            if (requireActivity().isInternetAvailable()) {
                val request = LoginRequest(
                    login = creds.first, password = creds.second,
                    deviceId = "device", clientVersion = AppVersion.versionNameCodeConcat
                )
//            authenticate(request)

                authenticationService.authenticate(
                    request,
                    this::navigateToSuccess,
                    this::displayLoginError
                ).apply {
                    btnLogin?.isEnabled = false
                }
            } else
                requireActivity().internetSnack()

        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.setupTopLevelConfigurations(R.id.loginFragment)
    }

    fun handlLogin() {
        resetErrorMessage()

        val fields = listOf(
            ValidatedTextField.validateField(username, username_layout, ensureNotBlank),
            ValidatedTextField.validateField(password, password_layout, ensureNotBlank)
        )

        val errorOccurred = fields.any { it }

        if (errorOccurred) {
            return
        }

        if (requireActivity().isInternetAvailable()) {

            val request = LoginRequest(
                username.text.toString().trim(),
                password.text.toString().trim(),
                "device", AppVersion.versionNameCodeConcat
            )
            authenticationService.authenticate(
                request,
                this::navigateToSuccess,
                this::displayLoginError
            ).apply {
                btnLogin?.isEnabled = false
            }
        } else {
            requireActivity().internetSnack()

        }
    }

        private fun displayLoginError(t: Throwable) {
            error_message?.text = activity?.getString(R.string.error_unsuccessful_login)
            error_message?.visibility = View.VISIBLE
            Log.e("LOGIN", "error", t)
            btnLogin?.isEnabled = true
        }

        private fun resetErrorMessage() {
            error_message?.text = null
            error_message?.visibility = View.GONE
            btnLogin?.isEnabled = true
        }

        private fun navigateToSuccess(response: CreateAccountResponse) {

            response.data?.let { payload ->
                AuthenticationHelper.update(payload)
                bindUserInfo(
                    payload.fullName,
                    payload.username,
                    payload.profilePic,
                    payload.verified
                )
            }

            if (remember_me_switch?.isChecked == true) {
                storeCredentials()
            }

            updateLoginStatus()
            dismissKeyboard()

            //TODO: Find proper way for handling dead fragments
            //Post to view. If fargment is detached - view won't be available, so no navigation is needed
            view?.post {
                findNavController().navigate(R.id.showFeedFragment)
            }
        }

        private fun storeCredentials() {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                "PreferencesFilename",
                masterKeyAlias,
                activity?.applicationContext!!,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            sharedPreferences.edit()?.apply {
                putString("LOGIN_USER", username.text.toString().trim())
                putString("LOGIN_PASS", password.text.toString().trim())
                apply()
            }
        }

        private fun fetchCredentials(): Pair<String, String> {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                "PreferencesFilename",
                masterKeyAlias,
                activity?.applicationContext!!,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val user = sharedPreferences.getString("LOGIN_USER", "")!!
            val pass = sharedPreferences.getString("LOGIN_PASS", "")!!

            return Pair(user, pass)
        }

        private fun hasStoredCredentials(): Boolean {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                "PreferencesFilename",
                masterKeyAlias,
                activity?.applicationContext!!,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            return sharedPreferences.contains("LOGIN_USER") && sharedPreferences.contains("LOGIN_PASS")
        }


    }