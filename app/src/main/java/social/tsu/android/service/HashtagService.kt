package social.tsu.android.service

import com.squareup.moshi.Moshi
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.network.NetworkConstants
import social.tsu.android.network.api.PostApi
import social.tsu.android.network.model.DiscoveryFeedResponse
import social.tsu.android.network.model.HashtagResponse
import social.tsu.android.rx.plusAssign
import social.tsu.android.utils.errorFromResponse
import javax.inject.Inject


abstract class HashtagService : DefaultService() {
    abstract fun loadPosts(
        hashtag: String,
        nextPage: Int,
        callback: ServiceCallback<HashtagResponse>
    )
}

class DefaultHashtagService @Inject constructor(
    private val postApi: PostApi,
    private val moshi: Moshi,
    private val schedulers: RxSchedulers
) : HashtagService() {

    override val tag: String = "DefaultHashtagService"

    override val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun loadPosts(
        hashtag: String,
        nextPage: Int,
        callback: ServiceCallback<HashtagResponse>
    ) {
        compositeDisposable += postApi.getPosts(hashtag, nextPage)
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe({ response ->
                val data = response.body()
                if (response.code() in NetworkConstants.HTTP_SUCCESS_RANGE && data != null) {
                    callback.onSuccess(data)
                } else {
                    val error = moshi.errorFromResponse(response)?.message ?: ""
                    callback.onFailure(error)
                }
            }, { error ->
                callback.onFailure(error.message ?: "")
            })
    }

}