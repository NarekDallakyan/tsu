package social.tsu.android.viewModel.community

import android.net.Uri
import social.tsu.android.TsuApplication
import social.tsu.android.network.model.Group
import social.tsu.android.network.model.PendingPost
import social.tsu.android.network.model.PendingRequest
import social.tsu.android.service.CommunityService
import social.tsu.android.service.CommunityServiceCallback
import social.tsu.android.service.CommunityType
import social.tsu.android.service.DefaultCommunityService
import social.tsu.android.ui.util.ImageUtils


abstract class CommunityCreateViewModel {
    abstract fun createCommunity(
        name: String,
        description: String,
        topicId: Int,
        moderation: Boolean,
        visibility: CommunityType,
        coverUri : Uri)

    abstract fun updateCommunity(
        communityId: Int,
        name: String,
        description: String,
        topicId: Int,
        moderation: Boolean,
        visibility: CommunityType,
        coverUri : Uri)
    abstract fun deleteCommunity(communityId: Int)
}

interface CommunityCreateViewModelCallback {
    fun didCommunityCreate()
    fun didFailCommunityCreate(message: String)
    fun didCommunityUpdate(group: Group)
    fun didCommunityDelete()
}

class DefaultCommunityCreateViewModel(
    application: TsuApplication,
    private var callback: CommunityCreateViewModelCallback?
) : CommunityCreateViewModel(), CommunityServiceCallback {

    private val communityService: CommunityService by lazy {
        DefaultCommunityService(application, this)
    }

    override fun didCreateCommunity() {
        callback?.didCommunityCreate()
    }

    override fun didErrorWith(message: String) {
        callback?.didFailCommunityCreate(message)
    }

    override fun createCommunity(
        name: String,
        description: String,
        topicId: Int,
        moderation: Boolean,
        visibility: CommunityType, coverUri : Uri) {
        val image = ImageUtils.imageDataFrom(coverUri)
        communityService.createCommunity(name, description, topicId, moderation, visibility, image)
    }

    override fun updateCommunity(
        communityId: Int,
        name: String,
        description: String,
        topicId: Int,
        moderation: Boolean,
        visibility: CommunityType,
        coverUri: Uri
    ) {
        val image = ImageUtils.imageDataFrom(coverUri)
        communityService.updateCommunity(
            communityId,
            name,
            description,
            topicId,
            moderation,
            visibility,
            image
        )
    }

    override fun deleteCommunity(communityId: Int) {
        communityService.deleteCommunity(communityId)
    }

    override fun didLoadCommunity(community: Group) {
    }

    override fun didLoadSuggestedCommunities(communities: List<Group>) {
        // ignore
    }

    override fun didLoadCommunityPendingRequests(requests: List<PendingRequest>) {
    }

    override fun didJoinCommunity(community: Group) {
    }

    override fun didFailedToJoinCommunity(community: Group) {
        // ignore
    }

    override fun completedGetCommunityPosts(nextPage: Int?) {
    }

    override fun failedGetCommunityPosts(code: Int) {
    }
    
    override fun failedLoadCommunityPendingRequests() {
    }

    override fun didLoadPendingPosts(posts: List<PendingPost>) {

    }

    override fun didFailToLoadPendingPosts(message: String) {
    }

    override fun didLeftGroup() {

    }

    override fun didFailedToLeaveGroup(message: String) {}

    override fun didDeleteCommunity() {
        callback?.didCommunityDelete()
    }

    override fun didUpdateCommunity(group: Group) {
        callback?.didCommunityUpdate(group)
    }
}