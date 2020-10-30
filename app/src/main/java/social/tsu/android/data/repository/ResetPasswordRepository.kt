package social.tsu.android.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.reset_password.ResetPasswordService
import social.tsu.android.ui.model.Data
import javax.inject.Inject

class ResetPasswordRepository @Inject constructor(private val resetPasswordService: ResetPasswordService){


    fun resetPassword(email: String, newPassword:String, code:String):LiveData<Data<Boolean>>{
        val loadState = MutableLiveData<Data<Boolean>>()
        resetPasswordService.resetPassword(email, newPassword, code, object :
            ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) { // reset password success
                //update reset password load state with success
                loadState.value = Data.Success(result)
            }

            override fun onFailure(errMsg: String) {// reset password failed
                //update reset password load state with error
                loadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return loadState
    }

    fun requestOTP(email:String): LiveData<Data<Boolean>>{
        val loadState = MutableLiveData<Data<Boolean>>()
        loadState.value = Data.Loading()
        resetPasswordService.requestOTP(email, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {// request otp success
                // update request otp load state with success
                loadState.value = Data.Success(true)
            }

            override fun onFailure(errMsg: String) {// reset otp failed
                //update request otp load state with error
                loadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return loadState
    }

}