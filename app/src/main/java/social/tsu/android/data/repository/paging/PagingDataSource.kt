package social.tsu.android.data.repository.paging

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Action
import io.reactivex.schedulers.Schedulers
import social.tsu.android.service.ServiceCallback
import social.tsu.android.ui.model.Data

class PagingDataSource<Model>(
    private val apiCall: (page: Int, count: Int, serviceCallback: ServiceCallback<List<Model>>) -> Unit,
    private val emptyInitialResponseErrMsg: String
) : PageKeyedDataSource<Int, Model>() {

    val loadState = MutableLiveData<Data<Boolean>>()
    val initialLoad = MutableLiveData<Data<Boolean>>()

    private var retryCompletable: Completable? = null

    private var compositeDisposable = CompositeDisposable()

    private var previousLoadResults = listOf<Model>()

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, Model>
    ) {
        loadState.postValue(Data.Loading())

        apiCall.invoke(1, params.requestedLoadSize, object : ServiceCallback<List<Model>> {
            override fun onSuccess(result: List<Model>) {
                if (result.isEmpty()) {
                    loadState.postValue(Data.Success(false))
                    initialLoad.postValue(Data.Error(Throwable(emptyInitialResponseErrMsg)))
                    setRetry { loadInitial(params, callback) }
                } else {
                    loadState.postValue(Data.Success(true))
                    initialLoad.postValue(Data.Success(true))
                    Log.w("PagingDataSource","results size: ${result.size} ")
                    previousLoadResults = result
                    callback.onResult(result,null,2)
                }
            }

            override fun onFailure(errMsg: String) {
                initialLoad.postValue(Data.Error(Throwable(errMsg)))
            }
        })
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, Model>) {
        loadState.postValue(Data.Loading())

        apiCall.invoke(params.key, params.requestedLoadSize, object : ServiceCallback<List<Model>> {
            override fun onSuccess(result: List<Model>) {
                loadState.postValue(Data.Success(true))
                Log.w("PagingDataSource","results size: ${result.size}")

                if(previousLoadResults.isNotEmpty() && result.isNotEmpty() && result.last() != previousLoadResults.last()) {
                    previousLoadResults = result
                    callback.onResult(result, params.key + 1)
                }
            }

            override fun onFailure(errMsg: String) {
                loadState.postValue(Data.Error(Throwable(errMsg)))
            }
        })
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, Model>) {

    }

    private fun setRetry(block:()->Unit) {
        retryCompletable = Completable.fromAction(Action(block))
    }

    fun retry() {
        retryCompletable?.let {
            compositeDisposable.add(it
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ }, { throwable -> Log.w("PagingDataSource", throwable.message?:"Unknown Error") }))
        }
    }
}