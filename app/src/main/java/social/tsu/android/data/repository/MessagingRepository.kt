package social.tsu.android.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import social.tsu.android.R
import social.tsu.android.data.local.dao.MessagingDao
import social.tsu.android.data.local.entity.RecentContact
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.service.MessagingService
import social.tsu.android.service.ServiceCallback
import social.tsu.android.ui.model.Data
import java.util.concurrent.Executors
import javax.inject.Inject

class MessagingRepository @Inject constructor(
    private val application: Application,
    private val messagingService: MessagingService,
    private val messagingDao: MessagingDao
) {

    private val _loadState = MutableLiveData<Data<Boolean>>()
    val loadState: LiveData<Data<Boolean>> = _loadState

    val recentContacts by lazy {
        LivePagedListBuilder(messagingDao.getRecentContacts(), config).build()
    }

    private val _currentUserId = MutableLiveData<Int>()

    val unreadCount: LiveData<Int> = Transformations.switchMap(_currentUserId) {
        if (it != null) {
            loadNewRecents()
            messagingDao.countUnreadContacts(it)
        } else {
            MutableLiveData()
        }
    }

    fun refreshCount() {
        _currentUserId.postValue(AuthenticationHelper.currentUserId)
        val lastValue = _loadState.value
        if (lastValue == null || lastValue is Data.Success) {
            loadNewRecents()
        }
    }

    fun deleteRecentContact(recentContact: RecentContact) {
        val recipientId = recentContact.otherUser?.id ?: return

        _loadState.postValue(Data.Loading())
        messagingService.deleteRecentContact(recipientId, object : ServiceCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                _loadState.postValue(Data.Success(result))
                if (result) {
                    messagingDao.removeContactWithMessages(recentContact)
                } else {
                    _loadState.postValue(Data.Error(Throwable(application.getString(R.string.general_error))))
                }
            }

            override fun onFailure(errMsg: String) {
                _loadState.postValue(Data.Error(Throwable(errMsg)))
            }
        })
    }

    private fun loadNewRecents() = execute {

        //fetch new recents if none exists locally
        _loadState.postValue(Data.Loading())

        messagingService.getMyRecentContacts(object : ServiceCallback<List<RecentContact>> {
            override fun onSuccess(result: List<RecentContact>) {
                messagingDao.updateRecents(result)
                if (result.isNotEmpty()) {
                    _loadState.postValue(Data.Success(true))
                } else _loadState.postValue(Data.Error(Throwable(application.getString(R.string.no_recents_msg))))
            }

            override fun onFailure(errMsg: String) {
                _loadState.postValue(Data.Error(Throwable(errMsg)))
            }
        })
    }

    fun retry() = loadNewRecents()

    private var config: PagedList.Config = PagedList.Config.Builder()
        .setPageSize(20)
        .setInitialLoadSizeHint(20 * 2)
        .setEnablePlaceholders(false)
        .build()


    private fun execute (block:()->Unit) = Executors.newSingleThreadExecutor().execute(block)
}