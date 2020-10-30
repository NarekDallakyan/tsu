package social.tsu.android.rx

import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import social.tsu.android.RxSchedulers

class TestSchedulers: RxSchedulers {
    override fun main(): Scheduler = Schedulers.trampoline()
    override fun io(): Scheduler = Schedulers.trampoline()
}
