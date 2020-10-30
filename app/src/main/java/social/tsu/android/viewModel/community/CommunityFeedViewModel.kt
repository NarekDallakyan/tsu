package social.tsu.android.viewModel.community

import social.tsu.android.TsuApplication
import social.tsu.android.data.local.entity.Post
import social.tsu.android.network.model.*
import social.tsu.android.service.*
import social.tsu.android.ui.BaseFeedViewModel

interface CommunityFeedViewModelCallback {
    fun didUpdatePost(post: Post)
    fun didLoadGroup(group: Group)
    fun didLoadMembership(membership: Membership?)
    fun didUpdateFeed()
    fun didReportPost(postId: Long)
    fun didErrorWith(message: String)
    fun didBlockUser(userId: Int, message: String)
    fun didUnblockUser(userId: Int, message: String)
    fun didFailedToBlockUser(userId: Int, message: String)
    fun didFailedToUnblockUser(userId: Int, message: String)
    fun didFailedLoadPosts(code: Int)
    fun didReachedFeedEnd()
    fun didLoadPendingPosts(posts: List<PendingPost>)
    fun didFailedLoadPendingPosts(message: String)
    fun didLeftGroup()
    fun didFailedToLeaveGroup(message: String)
    fun didDeletedPost(postId: Long)
}

abstract class CommunityFeedViewModel : BaseFeedViewModel {
    abstract fun getCommunityPosts(communityId: Int, page: Int?)
    abstract fun getPendingPosts(communityId: Int)
    abstract fun feedForCommunity(communityId: Int): List<Post>
    abstract fun leave(membershipId: Int)
    abstract fun joinCommunity(community: Group)
    abstract fun loadCommunity()
    abstract fun loadMembership()
    abstract var nextPage: Int?
    abstract var communityId: Int
}

class DefaultCommunityFeedViewModel(
    private val application: TsuApplication,
    private val callback: CommunityFeedViewModelCallback?
) : CommunityFeedViewModel(), LikeServiceCallback, ShareServiceCallback,
    BlockServiceCallback, FeedServiceCallback, CommunityServiceCallback,
    MembershipServiceCallback {

    private val communityService: CommunityService by lazy {
        DefaultCommunityService(application, this)
    }

    private val membershipService: MembershipService by lazy {
        DefaultMembershipService(application, this)
    }

    private val feedService: FeedService by lazy {
        DefaultFeedService(application, this)
    }

    private val likeSerivce: LikeService by lazy {
        DefaultLikeService(application, this)
    }

    private val shareService: ShareService by lazy {
        DefaultShareService(application, this)
    }

    private val blockService: BlockService by lazy {
        DefaultBlockService(application, this)
    }

    override var nextPage: Int? = null
    override var communityId: Int = 0

    init {
        application.appComponent.inject(this)
    }

    override fun feedForCommunity(communityId: Int): List<Post>{
        //Log.d("CommunityFeedViewModel", "Get comm feed, commId: $communityId, nextPage = $nextPage")
        return communityService.getCommunityPosts(communityId)
    }


    override fun getCommunityPosts(postId: Int, page: Int?) {
        //Log.d("CommunityFeedViewModel", "Get comm posts, commId: $postId, page: $page, nextPage: $nextPage")
        communityService.getCommunityFeed(postId, page)
    }

    override fun joinCommunity(community: Group) {
        communityService.joinCommunity(community)
    }

    override fun loadCommunity() {
        communityService.loadCommunityInfo(communityId)
    }

    override fun loadMembership() {
        membershipService.getMemberships()
    }

    override fun completeGetMembership(memberships: List<Membership>) {
        callback?.didLoadMembership(memberships.find { it.group.id == communityId })
    }

    override fun completeDeleteMembership(membershipId: Int) {
    }

    override fun errorDeleteMembership(membershipId: Int) {
    }

    override fun like(post: Post) {
        likeSerivce.like(post)
    }

    override fun unlike(post: Post) {
        likeSerivce.unlike(post)
    }

    override fun share(post: Post) {
        shareService.share(post.id)
    }

    override fun unshare(post: Post) {
        post.shared_id?.let {
            shareService.unshare(it)
        }
    }

    override fun report(postId: Long, reasonId: Int) {
        feedService.report(postId, reasonId)
    }

    override fun delete(postId: Long) {
        feedService.deletePost(postId)
    }

    override fun completedDeletePost(postId: Long) {
        callback?.didDeletedPost(postId)
    }

    override fun completedReport(postId: Long) {
        callback?.didReportPost(postId)
    }

    override fun completedError(message: String) {
        didErrorWith(message)
    }

    override fun didLoadCommunity(community: Group) {
        callback?.didLoadGroup(community)
    }

    override fun didCompletePostLike(post: Post, response: LikeResponse) {
        if (response.postId > 0) {
            callback?.didUpdatePost(post)
        } else {
            didErrorWith(response.mesaage)
        }
    }

    override fun didCompletePostUnlike(post: Post, response: LikeResponse) {
        if (response.postId > 0) {
            callback?.didUpdatePost(post)
        } else {
            didErrorWith(response.mesaage)
        }
    }

    override fun didCreateCommunity() {}

    override fun didLoadSuggestedCommunities(communities: List<Group>) {}
    override fun didLoadCommunityPendingRequests(requests: List<PendingRequest>) {
    }

    override fun didJoinCommunity(community: Group) {
        loadMembership()
        getCommunityPosts(community.id, null)
    }

    override fun didFailedToJoinCommunity(community: Group) {}

    override fun completedGetCommunityPosts(nextPage: Int?) {
        //Log.d("CommunityFeedViewModel", "Get comm posts, nextPage: $nextPage")
        if (nextPage == null) {
            callback?.didReachedFeedEnd()
        }
        this.nextPage = nextPage
        callback?.didUpdateFeed()
    }

    override fun failedGetCommunityPosts(code: Int) {
        //Log.d("CommunityFeedViewModel", "Failed get community posts, code: $code")
        callback?.didFailedLoadPosts(code)
    }

    override fun failedLoadCommunityPendingRequests() {

    }

    override fun didDeleteCommunity() {
    }

    override fun didUpdateCommunity(group: Group) {
    }

    override fun didErrorWith(message: String) {
        callback?.didErrorWith(message)
    }

    override fun didCompletePostShare(response: ShareResponseDTO) {
        feedService.getPost(response.postId.toInt(), communityId, true)
    }

    override fun didCompletePostUnshare(response: ShareResponseDTO) {
        feedService.getPost(response.postId.toInt(), communityId, true)
    }

    override fun didFailToSharePost(message: String) {
        callback?.didErrorWith(message)
    }

    override fun completedGetPost(post: Post) {
        callback?.didUpdatePost(post)
    }

    override fun completedGetUserPosts(lastPostId: Int?) {
        callback?.didUpdateFeed()
    }

    override fun block(userId: Int) {
        blockService.blockUser(userId)
    }

    override fun unblock(userId: Int) {
        blockService.unblockUser(userId)
    }

    override fun didCompleteUserBlock(userId: Int, message: String) {
        callback?.didBlockUser(userId, message)
    }

    override fun didCompleteUserUnblockResponse(userId: Int, message: String) {
        callback?.didUnblockUser(userId, message)
    }

    override fun didFailToBlockUser(userId: Int, message: String) {
        callback?.didFailedToBlockUser(userId, message)
    }

    override fun didFailToUnblockUser(userId: Int, message: String) {
        callback?.didFailedToUnblockUser(userId, message)
    }

    override fun didLoadPendingPosts(posts: List<PendingPost>) {
        callback?.didLoadPendingPosts(posts)
    }

    override fun didFailToLoadPendingPosts(message: String) {
        callback?.didFailedLoadPendingPosts(message)
    }

    override fun getPendingPosts(communityId: Int) {
        communityService.getPublishingRequests(communityId)
    }

    override fun leave(membershipId: Int) {
        communityService.leave(membershipId)
    }

    override fun didLeftGroup() {
        callback?.didLeftGroup()
    }

    override fun didFailedToLeaveGroup(message: String) {
        callback?.didFailedToLeaveGroup(message)
    }

}