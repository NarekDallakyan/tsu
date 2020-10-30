package social.tsu.android.service

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.models.TsuContact
import social.tsu.android.helper.AuthenticationHelper
import social.tsu.android.network.api.TsuContactsApi
import social.tsu.android.rx.plusAssign
import javax.inject.Inject


abstract class TsuContactsService: DefaultService(){

    abstract fun getMyFriends(page:Int, count:Int, serviceCallback: ServiceCallback<List<TsuContact>>)
    abstract fun getMyFollowers(page:Int, count:Int, serviceCallback: ServiceCallback<List<TsuContact>>)
    abstract fun getMyFollowings(page:Int, count:Int, serviceCallback: ServiceCallback<List<TsuContact>>)

}

class DefaultTsuContactsService @Inject constructor(private val application: TsuApplication,
                                                    private val tsuContactsApi: TsuContactsApi,
                                                    private val schedulers: RxSchedulers): TsuContactsService(){
    override val tag: String = "DefaultResetPasswordService"

    override val compositeDisposable = CompositeDisposable()



    override fun getMyFriends(page: Int, count: Int, serviceCallback: ServiceCallback<List<TsuContact>>) {
        compositeDisposable += tsuContactsApi.fetchMyFriends(AuthenticationHelper.currentUserId!!, page, count)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe(
                { response ->
                    handleResponseWithWrapper(application, response, serviceCallback)
                },
                { err ->
                    Log.d(tag, err.toString())
                    handleApiCallError(application, err, serviceCallback)
                }
            )
    }

    override fun getMyFollowers(page:Int, count:Int, serviceCallback: ServiceCallback<List<TsuContact>>) {
        compositeDisposable += tsuContactsApi.fetchMyFollowers(AuthenticationHelper.currentUserId!!, page, count)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe(
                { response ->
                    handleResponseWithWrapper(application, response, serviceCallback)
                },
                { err ->
                    Log.d(tag, err.toString())
                    handleApiCallError(application, err, serviceCallback)
                }
            )
    }

    override fun getMyFollowings(page:Int, count:Int, serviceCallback: ServiceCallback<List<TsuContact>>) {
        compositeDisposable += tsuContactsApi.fetchMyFollowings(AuthenticationHelper.currentUserId!!, page, count)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.main())
            .subscribe(
                { response ->
                    handleResponseWithWrapper(application, response, serviceCallback)
                },
                { err ->
                    Log.d(tag, err.toString())
                    handleApiCallError(application, err, serviceCallback)
                }
            )
    }
}
