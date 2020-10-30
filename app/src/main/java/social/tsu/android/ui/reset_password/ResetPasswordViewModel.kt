package social.tsu.android.ui.reset_password

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import social.tsu.android.data.repository.ResetPasswordRepository
import social.tsu.android.ui.model.Data
import javax.inject.Inject

class ResetPasswordViewModel @Inject constructor(private val resetPasswordRepo: ResetPasswordRepository): ViewModel(){

    var email:String = ""
    var code:String = ""
    var newPassword:String = ""

    fun resetPassword():LiveData<Data<Boolean>>{
        return resetPasswordRepo.resetPassword(email, newPassword, code)
    }

    fun requestOTP():LiveData<Data<Boolean>>{
        return resetPasswordRepo.requestOTP(email)
    }

}