package social.tsu.android.ui.reset_password

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_enter_email.*
import kotlinx.android.synthetic.main.progress_layout.*
import social.tsu.android.R
import social.tsu.android.ui.MainActivity
import social.tsu.android.ui.internetSnack
import social.tsu.android.ui.isInternetAvailable
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.dismissKeyboard
import social.tsu.android.utils.snack
import social.tsu.android.validation.ValidatedTextField
import social.tsu.android.validation.ensureValidEmail
import javax.inject.Inject

class EnterEmailFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<ResetPasswordViewModel>({ activity as MainActivity }) { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_enter_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        request_otp_button.setOnClickListener { handleOTPRequest() }
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

    private fun handleOTPRequest() {
        resetErrorMessage()

        //validate email field
        val fields = listOf(
            ValidatedTextField.validateField(email_edittext, email_til, ensureValidEmail)
        )

        val errorOccurred = fields.any { it }

        if (errorOccurred) {
            return
        }

        //email field valid, request one time code
        viewModel.email = email_edittext.text.toString()

        if (requireActivity().isInternetAvailable()) {

            viewModel.requestOTP().observe(viewLifecycleOwner, Observer {

                progress_layout.visibility = if (it is Data.Loading) View.VISIBLE else View.GONE
                request_otp_button.isEnabled = it !is Data.Loading

                when (it) {
                    is Data.Success -> {//one time code request successful
                        //go to one time code page after successful request
                        val navController = Navigation.findNavController(requireView())
                        navController.navigate(R.id.oneTimeCodeFragment)
                    }

                    is Data.Error -> {//one time code request failed
                        snack(it.throwable.message!!)
                    }
                }

            })
        } else
            requireActivity().internetSnack()

    }

    private fun resetErrorMessage() {
        //remove error message on form textfields
        email_til.error = null
    }
}