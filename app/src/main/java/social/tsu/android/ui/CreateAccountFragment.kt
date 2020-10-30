package social.tsu.android.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.*
import android.widget.DatePicker
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.textfield.TextInputEditText
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.create_account.*
import kotlinx.android.synthetic.main.invite_only_notice.*
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import social.tsu.android.BirthDate
import social.tsu.android.R
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.databinding.CreateAccountBinding
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.helper.TSUTextTokenizingHelper
import social.tsu.android.network.api.CreateAccountApi
import social.tsu.android.network.model.CreateAccountResponsePayload
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.SharedPrefManager
import social.tsu.android.ui.ext.addReactivePairTextChangedValidator
import social.tsu.android.ui.ext.addReactiveTextChangedValidator
import social.tsu.android.ui.ext.addReactiveValidator
import social.tsu.android.ui.model.Data
import social.tsu.android.ui.util.HtmlAnchorClickListener
import social.tsu.android.ui.util.addClickableSpan
import social.tsu.android.utils.*
import social.tsu.android.validation.*
import social.tsu.android.viewModel.signup.SignupViewModel
import java.util.*
import javax.inject.Inject


const val DEFAULT_YEAR = 2020
const val MIN_AGE = 17
const val MAX_AGE = 120
const val DEFAULT_MONTH = 1
const val DEFAULT_DAY = 1

class CreateAccountFragment : Fragment() {

    @Inject
    lateinit var createAccountAPI: CreateAccountApi

    @Inject
    lateinit var schedulers: RxSchedulers

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    private val args by navArgs<CreateAccountFragmentArgs>()

    private var birthdate: BirthDate = BirthDate(DEFAULT_MONTH, DEFAULT_DAY, DEFAULT_YEAR)
    private val compositeDisposable = CompositeDisposable()

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var signupViewModel: SignupViewModel

    private val invitedBy by lazy { args.invitedBy }
    private val hubspotId by lazy { args.hubspotId }
    private val oldTsuUser by lazy { args.oldUserDetails }
    private val invitedByUsername by lazy { args.invitedByUsername }
    private val hasInvitation by lazy { invitedBy > 0 || hubspotId > 0 || oldTsuUser != null || invitedByUsername != null }
    val properties = HashMap<String, Any?>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()

        fragmentComponent.inject(this)


        val view = inflater.inflate(
            if (hasInvitation) R.layout.create_account else {
                supportActionBar?.hide()
                R.layout.invite_only_notice
            },
            container, false
        )

        Log.d("CreateAccount", "Invite code = $invitedBy")

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        supportActionBar?.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        if (!hasInvitation) {
            setupInvitationNotice()
            return
        }


        initBinding(view)

        initBirthdayInput()

        initValidators()

        signupViewModel.createUserLiveData.observe(viewLifecycleOwner,
            Observer<Data<CreateAccountResponsePayload>> { t ->
                t.let {
                    when (it) {
                        is Data.Error -> {
                            var errorMessage = it.throwable.message
                            if (TextUtils.isEmpty(errorMessage)) {
                                errorMessage = getString(R.string.error_create_account)
                            }
                            error_message.visibility = View.VISIBLE
                            error_message.text = errorMessage
                            properties["error"] = true
                            analyticsHelper.logEvent("signup", properties)

                        }
                        is Data.Success -> {
                            error_message.visibility = View.GONE
                            bindUserInfo(
                                it.data.fullName,
                                it.data.username,
                                it.data.profilePic,
                                it.data.verified
                            )
                            updateLoginStatus()
                            findNavController().navigate(
                                CreateAccountFragmentDirections.actionCreateAccountFragmentToOldUserOnBoardingFragment()
                                    .apply {
                                        oldUserDetails = oldTsuUser
                                    })
                            it.data.id?.let { it1 -> sharedPrefManager.setUserId(it1) }
                            properties["success"] = true
                            properties["userId"] = it.data.id
                            Log.d("userIdtest", "it is: " + it.data.id)
                            analyticsHelper.logEvent("signup", properties)
                            analyticsHelper.logEvent("initial_onboarding", properties)

                        }

                    }
                }
            })
        onBackPressed(true) {
            properties["canceled"] = true
            analyticsHelper.logEvent("signup", properties)

        }


        val usernameTxt = view.findViewById<TextInputEditText>(R.id.username)
        usernameTxt.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                if (requireActivity().isInternetAvailable())
                    signupViewModel.checkIfUsernameExists()
                else
                    requireActivity().internetSnack()
            }
        }

        val invitedByUsernameTxt =
            view.findViewById<TextInputEditText>(R.id.invited_by_username_edittext)
        compositeDisposable += invitedByUsernameTxt.textChanges().subscribe {
            val username = extractUsername(it.toString())!!
            if (UsernameValidator.validate(username)) {
                if (requireActivity().isInternetAvailable())
                    signupViewModel.checkIfInvitedByUsernameExists(username)
                else
                    requireActivity().internetSnack()
            }
        }

        val emailTxt = view.findViewById<TextInputEditText>(R.id.email)

        emailTxt.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                signupViewModel.checkIfEmailExists()
            }
        }

        signupViewModel.tosLiveData.observe(viewLifecycleOwner, Observer<Boolean> {
            if (it) {
                terms_err.visibility = View.GONE
            }
        })

        addClickableSpan(terms_and_conditions, getString(R.string.terms_and_conditions), object :
            HtmlAnchorClickListener {
            override fun onHyperLinkClicked(name: String) {
                context?.openUrl(name)
            }

        })

        setupInvitedByUsernameBottomHint()

        prepopulateUserDetails()

    }

    private fun setupInvitationNotice() {
        invite_only_request_btn?.setOnClickListener {
            requestInvitationEmail()
        }
        invite_only_signin_btn?.setOnClickListener {
            findNavController().navigate(R.id.showLoginFragment)
        }
    }

    private fun requestInvitationEmail() {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse(
                "mailto:support@tsu.social?" +
                        "subject=${Uri.encode(getString(R.string.tsu_invitation_request_email_subject))}&" +
                        "body=${Uri.encode(getString(R.string.tsu_invitation_request_email_body))}"
            )
        }
        startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_us)))
    }

    private fun setupInvitedByUsernameBottomHint() {
        val invitedUsernameSpannable =
            SpannableString(getString(R.string.invited_by_username_hint_message))
        TSUTextTokenizingHelper.clickable(
            context,
            invitedUsernameSpannable,
            "support@tsu.social",
            TSUTextTokenizingHelper.TsuClickableTextStyle.LINK
        ) {
            requestInvitationEmail()
        }

        invited_by_username_bottom_hint?.text = invitedUsernameSpannable
        invited_by_username_bottom_hint?.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun prepopulateUserDetails() {
        if (invitedByUsername != null) {
            invited_by_username_layout.show()
            invited_by_username_bottom_hint.show()
        }
        args.email?.let {
            signupViewModel.signupForm.email = it
            email?.isEnabled = false
        }
        oldTsuUser?.let { details ->
            details.firstname?.let {
                signupViewModel.signupForm.firstName = it
            }
            details.lastname?.let { signupViewModel.signupForm.lastName = it }
            details.username?.let { signupViewModel.signupForm.username = it }
            if (details.username_in_use == true) {
                username_layout?.error = getString(R.string.signup_username_exists)
            }
            details.email?.let { signupViewModel.signupForm.email = it }
            details.birthdate?.let {
                val birthDate = LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                signupViewModel.signupForm.date.set(
                    BirthDate(
                        birthDate.year,
                        birthDate.monthValue + 1,
                        birthDate.dayOfMonth
                    )
                )
            }
        }

    }

    override fun onResume() {
        super.onResume()
        val mainActivity = requireActivity() as MainActivity
        mainActivity.setupTopLevelConfigurations(R.id.createAccountFragment)

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

    private fun initBirthdayInput() {
        birthday.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                showDatePicker()
            }
        }

        birthday.setOnClickListener {
            showDatePicker()
        }

        //Needed to disable field editing while keeping it focusable for kb navigation
        birthday.keyListener = null


        signupViewModel.birthdayLiveData.observe(viewLifecycleOwner,
            Observer<BirthDate> {
                birthday.clearFocus()
            })
    }

    private fun initBinding(view: View) {
        val accountBinding: CreateAccountBinding = CreateAccountBinding.bind(view)
        signupViewModel = ViewModelProvider(this, viewModelFactory)[SignupViewModel::class.java]
        accountBinding.model = signupViewModel
        if (invitedBy > 0) signupViewModel.signupForm.inviteId = invitedBy
        if (hubspotId > 0) signupViewModel.signupForm.hubspotId = hubspotId
        invitedByUsername?.let { signupViewModel.signupForm.invitedByUsername = it }
        oldTsuUser?.email?.let { signupViewModel.signupForm.oldEmail = it }

    }

    private fun initValidators() {
        compositeDisposable += username.addReactiveValidator(
            UsernameValidator,
            TextInputLayoutReactor(
                username_layout,
                getString(R.string.signup_username_validation_error)
            )
        )

        compositeDisposable += email.addReactiveValidator(
            EmailValidator,
            TextInputLayoutReactor(
                email_layout,
                getString(R.string.signup_email_validation_error)
            )
        )

        compositeDisposable += password.addReactiveValidator(
            PasswordValidator,
            TextInputLayoutReactor(
                password_layout,
                getString(R.string.signup_password_validation_error)
            )
        )

        compositeDisposable += first_name.addReactiveValidator(
            TextNotEmptyValidator,
            TextInputLayoutReactor(
                first_name_layout,
                getString(R.string.signup_empty_field_validation_error)
            )
        )

        compositeDisposable += last_name.addReactiveValidator(
            TextNotEmptyValidator,
            TextInputLayoutReactor(
                last_name_layout,
                getString(R.string.signup_empty_field_validation_error)
            )
        )

        compositeDisposable += birthday.addReactiveTextChangedValidator(
            DateValidator,
            TextInputLayoutReactor(
                birthday_layout,
                getString(R.string.signup_birthdate_validation_error)
            )
        )

        compositeDisposable += invited_by_username_edittext.addReactiveTextChangedValidator(
            InvitedByUsernameValidator,
            TextInputLayoutReactor(
                invited_by_username_layout,
                getString(R.string.signup_invited_by_username_validation_error)
            )
        )

        compositeDisposable += confirm_password.addReactivePairTextChangedValidator(
            password,
            EqualPasswordsValidator,
            TextInputLayoutReactor(
                confirm_password_layout,
                getString(R.string.confirm_password_error)
            )
        )

        compositeDisposable += password.addReactivePairTextChangedValidator(
            confirm_password,
            EqualPasswordsValidator,
            TextInputLayoutReactor(
                confirm_password_layout,
                getString(R.string.confirm_password_error)
            )
        )

        signupViewModel.validationLiveData.observe(
            viewLifecycleOwner,
            Observer {
                for (valPair in it) {
                    with(signupViewModel.signupForm) {
                        when (valPair.first) {
                            SignupViewModel.SignupFields.Email ->
                                setFieldError(
                                    valPair.second,
                                    emailError,
                                    R.string.signup_email_validation_error
                                )
                            SignupViewModel.SignupFields.Password ->
                                setFieldError(
                                    valPair.second,
                                    passwordError,
                                    R.string.signup_password_validation_error
                                )
                            SignupViewModel.SignupFields.Username ->
                                setFieldError(
                                    valPair.second,
                                    usernameError,
                                    R.string.signup_username_validation_error
                                )
                            SignupViewModel.SignupFields.Firstname ->
                                setFieldError(
                                    valPair.second,
                                    firstNameError,
                                    R.string.signup_first_name_validation_error
                                )
                            SignupViewModel.SignupFields.Lastname ->
                                setFieldError(
                                    valPair.second,
                                    lastNameError,
                                    R.string.signup_last_name_validation_error
                                )
                            SignupViewModel.SignupFields.TosAccepted ->
                                terms_err.visibility =
                                    if (valPair.second) View.GONE else View.VISIBLE
                            SignupViewModel.SignupFields.Date ->
                                setFieldError(
                                    valPair.second,
                                    dateError,
                                    R.string.signup_birthdate_validation_error
                                )
                            SignupViewModel.SignupFields.ConfirmPassword ->
                                setFieldError(
                                    valPair.second,
                                    confirmPasswordError,
                                    R.string.confirm_password_error
                                )
                            SignupViewModel.SignupFields.InvitedByUsername ->
                                setFieldError(
                                    valPair.second,
                                    invitedByUserNameError,
                                    R.string.signup_username_validation_error
                                )
                        }
                    }
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

    override fun onDetach() {
        super.onDetach()
        compositeDisposable.dispose()

    }

    private fun showDatePicker() {

        //Hide kb since it won't go away by itself and will be visible in background,
        //which is not looking good
        dismissKeyboard()

        val minDefaultYear =
            ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).year - MIN_AGE

        val datePickerDialog = DatePickerDialog(
            requireActivity(),
            { datePicker: DatePicker, year: Int, month: Int, day: Int ->
                Log.d("CYBO", "picked $year $month $day")
                birthdate = BirthDate(month + 1, day, year)
                signupViewModel.signupForm.date.set(birthdate)
                signupViewModel.birthdayLiveData.value = birthdate
            }, minDefaultYear, DEFAULT_MONTH, DEFAULT_DAY

        )

        datePickerDialog.setOnCancelListener {
            birthday.clearFocus()
        }

        //Set min date
        val minDate = Calendar.getInstance()
        minDate.add(Calendar.YEAR, -MAX_AGE)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.setCancelable(false)

        //Dumb DatePicker thing: there is no official way to choose year first.
        //This is most common approach to achieve such behavior
        if (datePickerDialog.datePicker.touchables.size > 0) {
            datePickerDialog.datePicker.touchables[0].performClick()
        }

        datePickerDialog.show()
    }
}