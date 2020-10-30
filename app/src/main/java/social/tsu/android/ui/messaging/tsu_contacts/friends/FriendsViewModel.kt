package social.tsu.android.ui.messaging.tsu_contacts.friends

import androidx.lifecycle.ViewModel
import social.tsu.android.data.local.models.TsuContact
import social.tsu.android.data.repository.TsuContactsRepository
import javax.inject.Inject

class FriendsViewModel @Inject constructor(private val tsuContactsRepo: TsuContactsRepository): ViewModel(){

    init {
        tsuContactsRepo.contactType = TsuContact.Type.FRIEND
    }

    val loadState by lazy {
        tsuContactsRepo.loadState
    }

    val initialLoadState by lazy {
        tsuContactsRepo.initialState
    }

    val myFriends by lazy {
        tsuContactsRepo.myFriends
    }

    fun refreshFriends() {
        myFriends.value?.dataSource?.invalidate()
    }

    fun retry() {
        tsuContactsRepo.retry()
    }

}