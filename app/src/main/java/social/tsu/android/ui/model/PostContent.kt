package social.tsu.android.ui.model

import social.tsu.android.data.local.entity.Post
import social.tsu.android.network.model.PendingPost

class PostContent constructor(private val post: Post) :
    FeedContent<Post> {

    override fun getContent(): Post = post
}

class VideoPostContent constructor(private val post: Post) :
    FeedContent<Post> {

    override fun getContent(): Post = post
}

class LinkPostContent constructor(private val post: Post) :
    FeedContent<Post> {

    override fun getContent(): Post = post
}

class PendingPostContent constructor(private val post: PendingPost) :
    FeedContent<PendingPost> {

    override fun getContent(): PendingPost = post
}

