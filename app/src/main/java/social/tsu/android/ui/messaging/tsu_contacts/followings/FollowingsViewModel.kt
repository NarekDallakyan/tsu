package social.tsu.android.ui.messaging.tsu_contacts.followings

import androidx.lifecycle.ViewModel
import social.tsu.android.data.local.models.TsuContact
import social.tsu.android.data.repository.TsuContactsRepository
import javax.inject.Inject

class FollowingsViewModel @Inject constructor(private val tsuContactsRepo: TsuContactsRepository): ViewModel(){

    init {
        tsuContactsRepo.contactType = TsuContact.Type.FOLLOWING
    }

    val loadState by lazy {
        tsuContactsRepo.loadState
    }

    val initialLoadState by lazy {
        tsuContactsRepo.initialState
    }

    val myFollowings by lazy {
        tsuContactsRepo.myFollowings
    }

    fun refreshFollowings() {
        myFollowings.value?.dataSource?.invalidate()
    }

    fun retry() {
        tsuContactsRepo.retry()
    }

}