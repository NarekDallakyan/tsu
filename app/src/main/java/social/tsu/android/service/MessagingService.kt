package social.tsu.android.service

import android.app.Application
import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.data.local.entity.RecentContact
import social.tsu.android.network.api.MessagingApi
import social.tsu.android.rx.plusAssign
import javax.inject.Inject


abstract class MessagingService: DefaultService(){

    abstract fun getMyRecentContacts(serviceCallback: ServiceCallback<List<RecentContact>>)

    abstract fun deleteRecentContact(recipientId: Int, serviceCallback: ServiceCallback<Boolean>)

}

class DefaultMessagingService @Inject constructor(
    private val application: Application,
    private val messagingApi: MessagingApi,
    private val schedulers: RxSchedulers
) : MessagingService() {

    override val tag: String = "DefaultResetPasswordService"

    override val compositeDisposable = CompositeDisposable()



    override fun getMyRecentContacts( serviceCallback: ServiceCallback<List<RecentContact>>) {
        compositeDisposable += messagingApi.fetchMyRecentContacts()
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.io())
            .subscribe(
                { response ->
                    handleResponseWithWrapper(application, response, serviceCallback)
                    Log.d(tag, response.body().toString())
                },
                { err ->
                    Log.d(tag, err.toString())
                    handleApiCallError(application, err, serviceCallback)
                }
            )
    }

    override fun deleteRecentContact(recipientId: Int, serviceCallback: ServiceCallback<Boolean>) {
        compositeDisposable += messagingApi.deleteConversation(recipientId)
            .subscribeOn(schedulers.io())
            .observeOn(schedulers.io())
            .subscribe(
                { response ->
                    Log.d(tag, response.body().toString())
                    handleResponseResult(application, response, serviceCallback)
                },
                { err ->
                    Log.d(tag, err.toString())
                    handleApiCallError(application, err, serviceCallback)
                }
            )
    }

}
