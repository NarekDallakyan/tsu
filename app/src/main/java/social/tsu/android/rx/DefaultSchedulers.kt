package social.tsu.android.rx

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import social.tsu.android.RxSchedulers

class DefaultSchedulers : RxSchedulers {
    override fun main(): Scheduler  = AndroidSchedulers.mainThread()
    override fun io(): Scheduler  = Schedulers.io()
}
