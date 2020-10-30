package social.tsu.android.data.repository.paging


import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import social.tsu.android.service.ServiceCallback

class PagingDataSourceFactory<Model>(
    private val apiCall: (page: Int, count: Int, serviceCallback: ServiceCallback<List<Model>>) -> Unit,
    private val emptyInitialResponseErrMsg: String = "No Items Found"
) : DataSource.Factory<Int, Model>() {

    val itemsDataSourceLiveData = MutableLiveData<PagingDataSource<Model>>()

    override fun create(): DataSource<Int, Model> {
        val dataSource = PagingDataSource(apiCall, emptyInitialResponseErrMsg)
        itemsDataSourceLiveData.postValue(dataSource)
        return dataSource
    }

}