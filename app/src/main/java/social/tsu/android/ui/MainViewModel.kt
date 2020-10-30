package social.tsu.android.ui

import androidx.lifecycle.ViewModel
import social.tsu.android.data.repository.LogoutRepository
import javax.inject.Inject

class MainViewModel @Inject constructor(private val logoutRepo: LogoutRepository): ViewModel(){

    fun logoutUser(){
        logoutRepo.logoutUser()
    }

}