package social.tsu.android.ui.user_invite


import androidx.lifecycle.ViewModel
import social.tsu.android.data.repository.UserInviteRepository
import javax.inject.Inject

class UserInviteViewModel @Inject constructor(private val userRepo: UserInviteRepository) :
    ViewModel() {

    var email = ""

    fun verifyUserInvite(email: String) = userRepo.verifyUserInvite(email)

    fun verifyOldUser(email: String, verificationCode: Int) =
        userRepo.verifyOldUserInvite(email, verificationCode)


}