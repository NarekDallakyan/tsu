package social.tsu.android.ui.community

import android.net.Uri
import androidx.lifecycle.ViewModel
import social.tsu.android.network.model.Group

class CommunityViewModel : ViewModel() {
        var selectedGroup: Group? = null
        var pictureUri: Uri? = null

        var communityListeners: HashSet<CommunityCreateListener> = HashSet()

        fun triggerCommunityCreate() {
            communityListeners.forEach {
                it.onCommunityCreated()
            }
        }

        fun triggerCommunityChanged(group: Group) {
            communityListeners.forEach {
                it.onCommunityChanged(group)
            }
        }

        fun triggerCommunityDeleted() {
            communityListeners.forEach {
                it.onCommunityDeleted()
            }
        }
    }
