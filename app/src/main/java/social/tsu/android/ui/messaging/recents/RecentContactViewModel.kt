package social.tsu.android.ui.messaging.recents

import androidx.lifecycle.ViewModel
import social.tsu.android.data.local.entity.RecentContact
import social.tsu.android.data.repository.MessagingRepository
import social.tsu.android.ui.model.Data
import javax.inject.Inject

class RecentContactViewModel @Inject constructor(private val messagingRepo: MessagingRepository): ViewModel(){

    val loadState by lazy {
        messagingRepo.loadState
    }

    val myRecentContacts by lazy {
        messagingRepo.retry()
        messagingRepo.recentContacts
    }

    fun refreshFollowers() {
        myRecentContacts.value?.dataSource?.invalidate()
    }

    fun retry() {
        val currentState = messagingRepo.loadState.value
        if (currentState != null && currentState !is Data.Loading<Boolean>) {
            messagingRepo.retry()
        }
    }

    fun deleteContact(recentContact: RecentContact) {
        messagingRepo.deleteRecentContact(recentContact)
    }

}