package social.tsu.android.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import social.tsu.android.data.local.models.OldUserDetails
import social.tsu.android.network.model.VerifyInviteResponse
import social.tsu.android.service.ServiceCallback
import social.tsu.android.service.UserInviteService
import social.tsu.android.ui.model.Data
import javax.inject.Inject

class UserInviteRepository @Inject constructor(private val userService: UserInviteService) {


    fun verifyUserInvite(email: String): LiveData<Data<VerifyInviteResponse>> {
        val loadState = MutableLiveData<Data<VerifyInviteResponse>>()
        loadState.value = Data.Loading()
        userService.verifyUserInvite(email, object : ServiceCallback<VerifyInviteResponse> {
            override fun onSuccess(result: VerifyInviteResponse) {
                loadState.value = Data.Success(result)
            }

            override fun onFailure(errMsg: String) {
                loadState.value = Data.Error(Throwable(errMsg))
            }
        })
        return loadState
    }

    fun verifyOldUserInvite(email: String, verificationCode: Int): LiveData<Data<OldUserDetails>> {
        val loadState = MutableLiveData<Data<OldUserDetails>>()
        loadState.value = Data.Loading()
        userService.verifyOldTsuUserInvite(
            email,
            verificationCode,
            object : ServiceCallback<OldUserDetails> {
                override fun onSuccess(result: OldUserDetails) {
                    loadState.value = Data.Success(result)
                }

                override fun onFailure(errMsg: String) {
                    loadState.value = Data.Error(Throwable(errMsg))
                }
            })
        return loadState
    }


}