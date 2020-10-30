package social.tsu.android.ui.messaging.tsu_contacts.friends

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import dagger.android.support.AndroidSupportInjection
import social.tsu.android.data.local.models.TsuContact
import social.tsu.android.ui.messaging.tsu_contacts.BaseContactsFragment
import social.tsu.android.ui.model.Data
import javax.inject.Inject

class FriendsListFragment: BaseContactsFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<FriendsViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun getContacts(): LiveData<PagedList<TsuContact>> {
        return viewModel.myFriends
    }

    override fun getLoadState(): LiveData<Data<Boolean>> {
        return viewModel.loadState
    }

    override fun getInitialLoadState(): LiveData<Data<Boolean>> {
        return viewModel.initialLoadState
    }

    override fun retry() {
        viewModel.retry()
    }

    override fun onRefresh() {
        viewModel.refreshFriends()
    }
}