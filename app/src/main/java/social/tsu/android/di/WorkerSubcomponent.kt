package social.tsu.android.di

import androidx.work.RxWorker
import androidx.work.WorkerParameters
import dagger.BindsInstance
import dagger.Subcomponent
import social.tsu.android.workmanager.workers.UploadVideoWorker
import javax.inject.Provider

@Subcomponent(
    modules = [UploadVideoWorker.Builder::class]
)
interface WorkerSubcomponent {

    fun workers(): Map<Class<out RxWorker>, Provider<RxWorker>>

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun workerParameters(param: WorkerParameters): Builder

        fun build(): WorkerSubcomponent
    }
}
