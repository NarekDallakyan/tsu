package social.tsu.android

import io.reactivex.Scheduler

interface RxSchedulers {
    fun main(): Scheduler
    fun io(): Scheduler
}
