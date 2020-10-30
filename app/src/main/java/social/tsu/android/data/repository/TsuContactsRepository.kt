package social.tsu.android.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import social.tsu.android.R
import social.tsu.android.data.local.models.TsuContact
import social.tsu.android.data.repository.paging.PagingDataSource
import social.tsu.android.data.repository.paging.PagingDataSourceFactory
import social.tsu.android.service.TsuContactsService
import social.tsu.android.ui.model.Data
import javax.inject.Inject

class TsuContactsRepository @Inject constructor(
    private val application: Application,
    private val tsuContactsService: TsuContactsService
) {

    lateinit var contactType: TsuContact.Type
    
    val initialState: LiveData<Data<Boolean>> by lazy {
        Transformations.switchMap(
            itemsDataSourceFactory.itemsDataSourceLiveData,
            PagingDataSource<TsuContact>::initialLoad
        )
    }

    val loadState: LiveData<Data<Boolean>> by lazy {
        Transformations.switchMap(
            itemsDataSourceFactory.itemsDataSourceLiveData,
            PagingDataSource<TsuContact>::loadState
        )
    }
    
    val myFriends by lazy {
        LivePagedListBuilder(itemsDataSourceFactory, config).build()
    }

    val myFollowers by lazy {
        LivePagedListBuilder(itemsDataSourceFactory, config).build()
    }

    val myFollowings by lazy {
        LivePagedListBuilder(itemsDataSourceFactory, config).build()
    }

    fun retry() {
        itemsDataSourceFactory.itemsDataSourceLiveData.value?.retry()
    }

    private val itemsDataSourceFactory by lazy{
        when (contactType){
            TsuContact.Type.FRIEND -> PagingDataSourceFactory(
                tsuContactsService::getMyFriends, application.getString(R.string.no_friends_msg)
            )
            TsuContact.Type.FOLLOWER -> PagingDataSourceFactory(
                tsuContactsService::getMyFollowers, application.getString(
                    R.string.no_followers_msg
                )
            )
            TsuContact.Type.FOLLOWING -> PagingDataSourceFactory(
                tsuContactsService::getMyFollowings, application.getString(
                    R.string.no_following_error
                )
            )
        }
    }
    

    private var config: PagedList.Config = PagedList.Config.Builder()
        .setPageSize(15)
        .setInitialLoadSizeHint(15)
        .setEnablePlaceholders(false)
        .build()
}