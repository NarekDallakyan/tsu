package social.tsu.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider


class ProfileEdit : Fragment() {

    companion object {
        fun newInstance() = ProfileEdit()
    }

    private lateinit var profileEditViewModel: ProfileEditViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.profile_edit_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        profileEditViewModel = ViewModelProvider(this).get(ProfileEditViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
