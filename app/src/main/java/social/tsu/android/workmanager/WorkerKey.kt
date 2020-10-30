package social.tsu.android.workmanager

import androidx.work.RxWorker
import dagger.MapKey
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention()
@MapKey
annotation class WorkerKey(val value: KClass<out RxWorker>)
