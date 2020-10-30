package social.tsu.android.di

import dagger.Module
import dagger.Subcomponent
import social.tsu.android.adapters.CommentsViewHolder
import social.tsu.android.adapters.viewholders.PostViewHolder
import social.tsu.android.ui.messaging.chats.IncomingMessageViewHolder
import social.tsu.android.ui.messaging.chats.OutcomingMessageViewHolder


@Subcomponent(modules = [ViewHolderModule::class])
interface ViewHolderComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): ViewHolderComponent
    }

    fun inject(viewHolder: PostViewHolder)
    fun inject(viewHolder: CommentsViewHolder)
    fun inject(viewHolder: IncomingMessageViewHolder)
    fun inject(viewHolder: OutcomingMessageViewHolder)
    fun inject(viewHolder: social.tsu.android.ui.post_feed.view_holders.PostViewHolder)

}


@Module
class ViewHolderModule