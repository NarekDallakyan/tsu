package social.tsu.android.service

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.SearchApi
import social.tsu.android.TsuApplication
import social.tsu.android.network.model.HashTag
import social.tsu.android.network.model.SearchUser
import social.tsu.android.rx.plusAssign
import javax.inject.Inject

interface SearchServiceCallabck : DefaultServiceCallback {
    fun completedUserSearch(users: List<SearchUser>) {}
    fun completedHashTagSearch(users: List<HashTag>) {}
}

abstract class SearchService : DefaultService() {
    abstract fun search(term: String)
    abstract fun searchUsers(term: String)
    abstract fun searchHashtag(term: String)
}

class DefaultSearchService(
    private val application: TsuApplication,
    private val callback: SearchServiceCallabck
) : SearchService() {

    @Inject
    lateinit var searchApi: SearchApi

    @Inject
    lateinit var schedulers: RxSchedulers

    override val tag: String = "DefaultSearchService"

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    init {
        application.appComponent.inject(this)
    }

    override fun search(term: String) {

    }

    override fun searchUsers(term: String) {
        if (term.length < 3) {
            return
        }

        compositeDisposable += searchApi.searchUsers(term)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        val results = result.data.users
                        Log.d(tag, "search users results: $results")
                        callback.completedUserSearch(results)
                    },
                    onFailure = {
                        callback.completedUserSearch(emptyList())
                    }
                )
            }, { err ->
                Log.e(tag, "error = ${err.message}")
                callback.completedUserSearch(emptyList())
            })
    }

    override fun searchHashtag(term: String) {
        if (term.length < 2) {
            return
        }

        compositeDisposable += searchApi.searchHashtags(term)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        val results = result.data
                        Log.d(tag, "search users results: $results")
                        callback.completedHashTagSearch(results)
                    },
                    onFailure = {
                        callback.completedHashTagSearch(emptyList())
                    }
                )
            }, { err ->
                Log.e(tag, "error = ${err.message}")
                callback.completedHashTagSearch(emptyList())
            })
    }
}