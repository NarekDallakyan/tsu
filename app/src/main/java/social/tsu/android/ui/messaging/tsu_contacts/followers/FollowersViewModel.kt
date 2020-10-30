package social.tsu.android.ui.messaging.tsu_contacts.followers

import androidx.lifecycle.ViewModel
import social.tsu.android.data.local.models.TsuContact
import social.tsu.android.data.repository.TsuContactsRepository
import javax.inject.Inject

class FollowersViewModel @Inject constructor(private val tsuContactsRepo: TsuContactsRepository): ViewModel(){

    init {
        tsuContactsRepo.contactType = TsuContact.Type.FOLLOWER
    }

    val loadState by lazy {
        tsuContactsRepo.loadState
    }

    val initialLoadState by lazy {
        tsuContactsRepo.initialState
    }

    val myFollowers by lazy {
        tsuContactsRepo.myFollowers
    }

    fun refreshFollowers() {
        myFollowers.value?.dataSource?.invalidate()
    }

    fun retry() {
        tsuContactsRepo.retry()
    }

}