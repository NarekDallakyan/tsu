package social.tsu.android.ui.reset_password

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import dagger.android.support.AndroidSupportInjection
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_change_password.*
import kotlinx.android.synthetic.main.progress_layout.*
import social.tsu.android.R
import social.tsu.android.rx.plusAssign
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.ext.addReactiveValidator
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.dismissKeyboard
import social.tsu.android.utils.snack
import social.tsu.android.validation.*
import javax.inject.Inject

class ChangePasswordFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<ResetPasswordViewModel>({ activity as MainActivity }) { viewModelFactory }

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_change_password, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        change_password_button.setOnClickListener { handlePasswordChange() }

        compositeDisposable += new_password_edittext.addReactiveValidator(
            PasswordValidator,
            TextInputLayoutReactor(
                new_password_til,
                getString(R.string.signup_password_validation_error)
            )
        )
        compositeDisposable += confirm_new_password_edittext.addReactiveValidator(
            PasswordValidator,
            TextInputLayoutReactor(
                confirm_new_password_til,
                getString(R.string.signup_password_validation_error)
            )
        )
    }

    private fun handlePasswordChange() {
        resetErrorMessage()

        val validity = listOf(
            ValidatedTextField.validateField(
                new_password_edittext,
                new_password_til,
                ensureValidPassword
            ),
            ValidatedTextField.validateField(
                confirm_new_password_edittext,
                confirm_new_password_til,
                ensureValidPassword
            )
        )

        val isValid = validity.any { it }
        if (isValid) {
            return
        }

        //validate password fields
        val fields = listOf(
            ValidatePassword.validateField(
                Pair(new_password_edittext, confirm_new_password_edittext.text.toString()),
                new_password_til, ensureEqualPasswords
            ),
            ValidatePassword.validateField(
                Pair(confirm_new_password_edittext, new_password_edittext.text.toString()),
                confirm_new_password_til, ensureEqualPasswords
            )
        )

        val errorOccurred = fields.any { it }

        if (errorOccurred) {
            return
        }

        dismissKeyboard()

        // password is valid , reset user password
        viewModel.newPassword = new_password_edittext.text.toString()
        if (requireActivity().isInternetAvailable()) {

            viewModel.resetPassword().observe(viewLifecycleOwner, Observer {
                progress_layout.visibility = if (it is Data.Loading) View.VISIBLE else View.GONE
                change_password_button.isEnabled = it !is Data.Loading
                when (it) {
                    is Data.Success -> {// reset password success

                        snack(getString(R.string.password_reset_success_message))

                        val action =
                            ChangePasswordFragmentDirections.actionChangePasswordFragmentToLoginFragment()
                        if (viewModel.email.isNotEmpty())
                            action.email = viewModel.email

                        val navController = findNavController()
                        val navOptions = NavOptions.Builder()
                            .setPopUpTo(R.id.loginFragment, true)
                            .build()

                        navController.navigate(action, navOptions)
                    }

                    is Data.Error -> {// error resetting password
                        snack(it.throwable.message!!)
                    }
                }
            })
        } else
            requireActivity().internetSnack()

    }

    private fun resetErrorMessage() {
        //remove error message on form textfields
        new_password_til.error = null
        confirm_new_password_til.error = null
    }
}