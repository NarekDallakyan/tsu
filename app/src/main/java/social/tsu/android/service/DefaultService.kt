package social.tsu.android.service

import io.reactivex.disposables.CompositeDisposable

interface DefaultServiceCallback {
    fun didErrorWith(message: String)
}

abstract class DefaultService {
    abstract val tag: String
    abstract val compositeDisposable: CompositeDisposable

    fun onDestroy() {
        compositeDisposable.dispose()
    }
}