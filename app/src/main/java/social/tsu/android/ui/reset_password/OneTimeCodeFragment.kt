package social.tsu.android.ui.reset_password

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_one_time_code.*
import social.tsu.android.R
import social.tsu.android.ui.MainActivity
import social.tsu.android.validation.ValidatedTextField
import social.tsu.android.validation.ensureNotBlank
import javax.inject.Inject

class OneTimeCodeFragment : Fragment(){

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
        return inflater.inflate(R.layout.fragment_one_time_code, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        go_to_reset_password_page.setOnClickListener { handleGoToResetPage() }
    }

    private fun handleGoToResetPage() {
        resetErrorMessage()

        //validate one time code field
        val fields = listOf(
            ValidatedTextField.validateField(otp_edittext, otp_til, ensureNotBlank)
        )

        val errorOccurred = fields.any { it }

        if (errorOccurred) {
            return
        }

        viewModel.code = otp_edittext.text.toString()

        val navController = Navigation.findNavController(view!!)
        navController.navigate(R.id.changePasswordFragment)

    }

    private fun resetErrorMessage() {
        //remove error message on form textfields
        otp_til.error = null
    }
}