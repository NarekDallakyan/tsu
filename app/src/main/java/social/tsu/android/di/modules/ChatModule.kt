package social.tsu.android.di.modules

import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.data.local.dao.MessagingDao
import social.tsu.android.network.api.MessagingApi
import social.tsu.android.service.ChatService
import social.tsu.android.service.DefaultChatService
import social.tsu.android.ui.messaging.chats.ChatFragment


@Module(includes = [ChatModule.BindsInstance::class])
class ChatModule {

    @Provides
    fun providesChatService(
        application: TsuApplication,
        messagingApi: MessagingApi,
        rxSchedulers: RxSchedulers,
        messagingDao: MessagingDao,
        moshi: Moshi
    ): ChatService {
        return DefaultChatService(application, messagingApi, rxSchedulers, messagingDao, moshi)
    }

    @Module
    interface BindsInstance {

        @ContributesAndroidInjector
        fun chatFragment(): ChatFragment

    }

}