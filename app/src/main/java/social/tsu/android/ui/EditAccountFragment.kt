package social.tsu.android.ui

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.databinding.EditAccountBinding
import social.tsu.android.databinding.PasswordFieldBinding
import social.tsu.android.rx.plusAssign
import social.tsu.android.ui.ext.addReactiveValidator
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.bindUserInfo
import social.tsu.android.utils.dismissKeyboard
import social.tsu.android.utils.updateLoginStatus
import social.tsu.android.validation.EmailValidator
import social.tsu.android.validation.ObservableFieldReactor
import social.tsu.android.validation.TextNotEmptyValidator
import social.tsu.android.validation.UsernameValidator
import social.tsu.android.viewModel.account.EditAccountViewModel
import javax.inject.Inject


class EditAccountFragment : Fragment() {

    @Inject
    lateinit var viewModel: EditAccountViewModel

    private val compositeDisposable = CompositeDisposable()
    private lateinit var binding: EditAccountBinding
    private var alertIsBeingShown = false

    var accountDeleteTryCount: Int = 3

    var logoutListener: LogoutListener? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        logoutListener = context as LogoutListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.edit_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()

        fragmentComponent.inject(this)

        binding = EditAccountBinding.bind(view)
        binding.model = viewModel
        binding.editAccountProceed.setOnClickListener {
            if (viewModel.validateForm()) {
                showPasswordDialog()
            }
        }

        binding.deleteAccountProceed.setOnClickListener {


            if (accountDeleteTryCount > 0) {
                showAccountDeleteWarning()
            } else {
                didErrorWith(getString(R.string.account_delete_message))
            }
        }

        initValidators()
        if (requireActivity().isInternetAvailable())
            viewModel.fetchUserInfo()
        else
            requireActivity().internetSnack()

        viewModel.requiredFieldsLiveData.observe(viewLifecycleOwner, Observer {
            if (!it) {
                didErrorWith(getString(R.string.edit_account_empty_field_error))
            }
        })

        viewModel.accountDeleteLiveData.observe(viewLifecycleOwner, Observer {
            if (!it) {
                if (accountDeleteTryCount > 0) {
                    showAgainAccountDelete()
                } else {
                    didErrorWith(getString(R.string.account_delete_message))
                }
            } else {
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp()
                logoutListener?.onLogOutSuccess()
            }
        })

        viewModel.editLiveData.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Data.Error -> {
                    var errorMessage = result.throwable.message
                    if (TextUtils.isEmpty(errorMessage)) {
                        errorMessage = getString(R.string.edit_account_error)
                    }
                    didErrorWith(errorMessage!!)
                }
                is Data.Success -> {
                    bindUserInfo(
                        result.data.fullName,
                        result.data.username,
                        result.data.profilePictureUrl,
                        result.data.verifiedStatus
                    )
                    updateLoginStatus()
                    findNavController().navigate(R.id.mainFeedFragment)
                }
            }
        })

        binding.username.setOnFocusChangeListener { _, hasFocus ->
            when {
                hasFocus && !alertIsBeingShown -> {
                    alertIsBeingShown = true
                    showWarningDialog()
                }
                !hasFocus -> viewModel.checkIfUsernameExists()
            }
        }

        binding.email.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.checkIfEmailExists()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        compositeDisposable.dispose()
    }

    override fun onStop() {
        super.onStop()
        dismissKeyboard()
    }

    private fun initValidators() {
        compositeDisposable += binding.username.addReactiveValidator(
            UsernameValidator,
            ObservableFieldReactor(
                viewModel.viewForm.usernameError,
                R.string.signup_username_validation_error
            )
        )

        compositeDisposable += binding.email.addReactiveValidator(
            EmailValidator,
            ObservableFieldReactor(
                viewModel.viewForm.emailError,
                R.string.edit_account_email_validation_error
            )
        )

        compositeDisposable += binding.firstName.addReactiveValidator(
            TextNotEmptyValidator,
            ObservableFieldReactor(
                viewModel.viewForm.firstNameError,
                R.string.signup_first_name_validation_error
            )
        )

        compositeDisposable += binding.lastName.addReactiveValidator(
            TextNotEmptyValidator,
            ObservableFieldReactor(
                viewModel.viewForm.lastNameError,
                R.string.signup_last_name_validation_error
            )
        )

        viewModel.validationLiveData.observe(viewLifecycleOwner, Observer {
            for (valPair in it) {
                with(viewModel.viewForm) {
                    when (valPair.first) {
                        EditAccountViewModel.AccountFields.Email ->
                            setFieldError(
                                valPair.second,
                                emailError,
                                R.string.edit_account_email_validation_error
                            )
                        EditAccountViewModel.AccountFields.Username ->
                            setFieldError(
                                valPair.second,
                                usernameError,
                                R.string.signup_username_validation_error
                            )
                        EditAccountViewModel.AccountFields.LastName ->
                            setFieldError(
                                valPair.second,
                                lastNameError,
                                R.string.signup_last_name_validation_error
                            )
                        EditAccountViewModel.AccountFields.FirstName ->
                            setFieldError(
                                valPair.second,
                                firstNameError,
                                R.string.signup_first_name_validation_error
                            )
                    }
                }
            }
        })
    }

    private fun didErrorWith(message: String) {
        val localActivity = activity ?: return

        AlertDialog.Builder(localActivity)
            .setTitle(R.string.general_error_dialog_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showPasswordDialog() {
        val localActivity = activity ?: return

        val dialogBinding = PasswordFieldBinding.inflate(layoutInflater)
        dialogBinding.form = viewModel.viewForm

        AlertDialog.Builder(localActivity)
            .setTitle(R.string.edit_account_password_title)
            .setMessage(R.string.edit_account_password_message)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { dialogInterface, _ ->
                viewModel.saveAccount()
                dialogInterface.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener { dialogBinding.unbind() }
            .setOnCancelListener { dialogBinding.unbind() }
            .show()
    }

    private fun showPasswordDialogForDeleteAccount() {
        val localActivity = activity ?: return

        val dialogBinding = PasswordFieldBinding.inflate(layoutInflater)
        dialogBinding.form = viewModel.viewForm

        var alertDialog = AlertDialog.Builder(localActivity)
            .setTitle(R.string.enter_password)
            .setMessage(R.string.confirm_password_for_delete_account)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.confirm, null)
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener { dialogBinding.unbind() }
            .setOnCancelListener { dialogBinding.unbind() }
            .show()

        val b: Button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        b.setOnClickListener(View.OnClickListener {
            if (!viewModel.viewForm.password.isNullOrBlank()) {
                accountDeleteTryCount--
                viewModel.deleteAccount()
                alertDialog.dismiss()
            } else {
                activity?.hideKeyboard()
                requireActivity().snack(R.string.password_field_empty)
            }
        })

    }

    private fun showWarningDialog() {
        val localActivity = activity ?: return

        AlertDialog.Builder(localActivity)
            .setTitle(R.string.warning_title)
            .setMessage(R.string.warning_description)
            .setPositiveButton(R.string.btn_proceed) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showAccountDeleteWarning() {
        val localActivity = activity ?: return
        AlertDialog.Builder(localActivity)
            .setTitle(R.string.account_delete_warning_title)
            .setMessage(R.string.account_delete_warning_doc)
            .setPositiveButton(R.string.confirm) { dialogInterface, _ ->
                dialogInterface.dismiss()
                showPasswordDialogForDeleteAccount()
            }
            .setNegativeButton(R.string.cancel) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()

    }

    private fun showAgainAccountDelete() {
        var title = R.string.incorrect_password
        var message =
            "You have ${accountDeleteTryCount} attempts left after which you must wait 24 hours before making another deletion request"

        val localActivity = activity ?: return

        val dialogBinding = PasswordFieldBinding.inflate(layoutInflater)
        dialogBinding.form = viewModel.viewForm

        var alertDialog = AlertDialog.Builder(localActivity)
            .setTitle(title)
            .setMessage(message)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.confirm, null)
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener { dialogBinding.unbind() }
            .setOnCancelListener { dialogBinding.unbind() }
            .show()

        val b: Button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        b.setOnClickListener(View.OnClickListener {
            if (!viewModel.viewForm.password.isNullOrBlank()) {
                accountDeleteTryCount--
                viewModel.deleteAccount()
                alertDialog.dismiss()
            } else {
                activity?.hideKeyboard()
                requireActivity().snack(R.string.password_field_empty)
            }
        })

    }

    private fun setFieldError(
        fieldValid: Boolean,
        errorField: ObservableField<Int>,
        errorStrResource: Int
    ) {
        errorField.set(
            if (!fieldValid) errorStrResource else null
        )
    }

}