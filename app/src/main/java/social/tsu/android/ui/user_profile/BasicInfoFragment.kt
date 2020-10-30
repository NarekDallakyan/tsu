package social.tsu.android.ui.user_profile

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavArgs
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.textfield.TextInputEditText
import social.tsu.android.BirthDate
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.network.model.ProfileEditDTO
import social.tsu.android.network.model.ProfileEditInfoDTO
import social.tsu.android.network.model.UserProfile
import social.tsu.android.network.model.UserProfileParams
import social.tsu.android.service.DefaultSettingsService
import social.tsu.android.service.SettingsServiceCallback
import social.tsu.android.ui.DEFAULT_DAY
import social.tsu.android.ui.DEFAULT_MONTH
import social.tsu.android.ui.DEFAULT_YEAR
import social.tsu.android.ui.setVisibleOrGone
import social.tsu.android.utils.bindUserInfo
import social.tsu.android.utils.dismissKeyboard

class BasicInfoFragment : Fragment(), SettingsServiceCallback {

    private val args by navArgs<BasicInfoFragmentArgs>()

    lateinit var birthday: EditText
    lateinit var name: EditText
    lateinit var nameClearIcon: ImageView
    lateinit var hometown: EditText

    private val service: DefaultSettingsService by lazy {
        DefaultSettingsService(activity?.application as TsuApplication, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_menu, menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_basic_info, container, false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.save_btn) {
            saveData()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveData() {
        service.updateInfo(ProfileEditInfoDTO(ProfileEditDTO.Builder()
            .namePronunciation(name.text.toString())
            .birthday(birthday.text.toString())
            .hometown(hometown.text.toString())
            .build()))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        birthday = view.findViewById<TextInputEditText>(R.id.birthdate).apply {
            setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    showBirthdatePicker()
                }
            }
            setOnClickListener {
                showBirthdatePicker()
            }
            keyListener = null

        }
        name = view.findViewById(R.id.name)
        nameClearIcon = view.findViewById(R.id.name_icon)
        hometown = view.findViewById(R.id.hometown)

        name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                nameClearIcon.setVisibleOrGone(s?.isEmpty() == false)
            }
        })
        nameClearIcon.setOnClickListener { name.setText("") }
        bindUserData(args.user)
    }

    private fun bindUserData(user: UserProfileParams) {
        user.namePronunciation?.let { name.setText(it) }
        user.birthday?.let { birthday.setText(it) }
        user.hometown?.let { hometown.setText(it) }
    }

    private fun showBirthdatePicker() {
        dismissKeyboard()
        val datePickerDialog = DatePickerDialog(
            requireActivity(),
            { datePicker: DatePicker, year: Int, month: Int, day: Int ->
                val birthDate = BirthDate(month, day, year)
                birthday.setText(birthDate.stringValue)
            }, DEFAULT_YEAR, DEFAULT_MONTH, DEFAULT_DAY
        )
        datePickerDialog.setOnCancelListener {
            birthday.clearFocus()
        }
        datePickerDialog.show()
    }

    override fun failedToUpdateUserProfile(message: String?) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun completedUserProfileUpdate(info: UserProfile) {
        findNavController().navigateUp()
    }

    override fun didErrorWith(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}