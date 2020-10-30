package social.tsu.android.data.local.dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import social.tsu.android.data.local.entity.*


@Dao
interface PostFeedDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun saveFeedSource(vararg feedSource: FeedSource)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun saveFeedPostSource(vararg feedSourcePostJoin: FeedSourcePostJoin)

    @Query("SELECT * FROM post " +
            "INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id " +
            "WHERE feedsource_posts_join.source_type=:feedSourceType " +
            "ORDER BY timestamp DESC")
    fun getPosts(feedSourceType: FeedSource.Type): DataSource.Factory<Int, Post>

    @Query("SELECT * FROM post " +
            "INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id " +
            "LEFT JOIN feed_order_posts_join ON post.id=feed_order_posts_join.post_id " +
            "WHERE feedsource_posts_join.source_type=:feedSourceType " +
            "ORDER BY order_id ASC, timestamp DESC")
    fun getPostsWithOrder(feedSourceType: FeedSource.Type): DataSource.Factory<Int, Post>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun savePosts(post: Post): Long

    @Update
    fun updatePosts(vararg post: Post)

    @Transaction
    fun upsertPosts(vararg posts: Post) {
        posts.forEach {
            val id = savePosts(it)
            if (id == -1L) {
                updatePosts(it)
            }
        }
    }

    @Transaction
    fun upsertOrder(vararg orders: FeedOrder): Int {
        var conflictCounter = 0
        orders.forEach {
            val result = saveOrder(it)
            if (result == -1L) {
                conflictCounter++
                updateOrder(it)
            }
        }
        return conflictCounter
    }

    @Update
    fun updateOrder(vararg order: FeedOrder)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun saveOrder(order: FeedOrder): Long

    @Transaction
    fun upsertFeedSourceOrder(vararg orders: FeedSourceOrderPostJoin) {
        orders.forEach {
            val result = saveFeedSourceOrder(it)
            if (result == -1L) {
                updateFeedSourceOrder(it)
            }
        }
    }

    @Update
    fun updateFeedSourceOrder(vararg order: FeedSourceOrderPostJoin)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun saveFeedSourceOrder(order: FeedSourceOrderPostJoin): Long

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType AND user_id=:userId ORDER BY timestamp DESC")
    fun getUserPosts(
        userId: Int?,
        feedSourceType: FeedSource.Type = FeedSource.Type.USER
    ): DataSource.Factory<Int, Post>

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType AND groupId=:communityId ORDER BY timestamp DESC")
    fun getCommunityPosts(
        communityId: Int?,
        feedSourceType: FeedSource.Type = FeedSource.Type.COMMUNITY
    ): DataSource.Factory<Int, Post>

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType AND user_id=:userId ORDER BY timestamp DESC")
    fun getUserPhotoPosts(
        userId: Int?,
        feedSourceType: FeedSource.Type = FeedSource.Type.USER_PHOTOS
    ): DataSource.Factory<Int, Post>

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType AND content LIKE '%' || :hashtag || '%' ORDER BY timestamp DESC")
    fun getHashtagPosts(
        hashtag: String,
        feedSourceType: FeedSource.Type = FeedSource.Type.HASHTAG
    ): DataSource.Factory<Int, Post>

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType AND user_id=:userId ORDER BY timestamp DESC")
    fun getUserVideoPosts(
        userId: Int?,
        feedSourceType: FeedSource.Type = FeedSource.Type.USER_VIDEOS
    ): DataSource.Factory<Int, Post>

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType ORDER BY timestamp DESC")
    fun getDiscoveryFeedPosts(
        feedSourceType: FeedSource.Type = FeedSource.Type.DISCOVERY_FEED
    ): DataSource.Factory<Int, Post>

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType AND user_id=:userId ORDER BY timestamp DESC")
    fun getUserVideoPostsLiveData(
        userId: Int?,
        feedSourceType: FeedSource.Type = FeedSource.Type.USER_VIDEOS
    ): LiveData<List<Post>>

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType ORDER BY timestamp DESC")
    fun getUDiscoveryFeedLiveData(
        feedSourceType: FeedSource.Type = FeedSource.Type.DISCOVERY_FEED
    ): LiveData<List<Post>>

    @Query("SELECT * FROM post " +
            "INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id " +
            "WHERE feedsource_posts_join.source_type=:feedSourceType AND content LIKE '%' || :hashtag || '%' " +
            "ORDER BY timestamp DESC")
    fun getCacheByHashtag(hashtag: String, feedSourceType: FeedSource.Type): LiveData<List<Post>>

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType ORDER BY timestamp DESC")
    fun getDiscoveryCache(feedSourceType: FeedSource.Type): LiveData<List<Post>>

    @Query("SELECT * FROM post WHERE id=:postId LIMIT 1")
    fun getPostById(postId: Long): DataSource.Factory<Int, Post>

    @Query("SELECT * FROM post WHERE id=:postId LIMIT 1")
    fun getPostSync(postId: Long): Post?

    @Query("SELECT * FROM post WHERE id=:postId LIMIT 1")
    fun getPost(postId: Long): LiveData<Post>

    @Query("DELETE FROM post WHERE id=:postId ")
    fun deletePost(postId: Long)

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType ORDER BY timestamp ASC LIMIT 1")
    fun getOldestPost(feedSourceType: FeedSource.Type): Post?

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType AND user_id=:userId  ORDER BY timestamp ASC LIMIT 1")
    fun getOldestUserPost(
        userId: Int?,
        feedSourceType: FeedSource.Type = FeedSource.Type.USER
    ): Post?

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType AND user_id=:userId  ORDER BY timestamp ASC LIMIT 1")
    fun getOldestUserPhotoPost(
        userId: Int?,
        feedSourceType: FeedSource.Type = FeedSource.Type.USER_PHOTOS
    ): Post?

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType AND user_id=:userId  ORDER BY timestamp ASC LIMIT 1")
    fun getOldestUserVideoPost(
        userId: Int?,
        feedSourceType: FeedSource.Type = FeedSource.Type.USER_VIDEOS
    ): Post?

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType  ORDER BY timestamp ASC LIMIT 1")
    fun getOldestDiscoveryPost(
        feedSourceType: FeedSource.Type = FeedSource.Type.DISCOVERY_FEED
    ): Post?

    @Query("SELECT * FROM post INNER JOIN feedsource_posts_join ON post.id=feedsource_posts_join.post_id WHERE feedsource_posts_join.source_type=:feedSourceType AND groupId=:communityId  ORDER BY timestamp ASC LIMIT 1")
    fun getOldestCommunityPost(
        communityId: Int?,
        feedSourceType: FeedSource.Type = FeedSource.Type.COMMUNITY
    ): Post?

    @Query("SELECT * FROM post WHERE original_user_id=:userId ORDER BY timestamp DESC LIMIT 1 ")
    fun getUserRecentPost(userId: Int?): Post?

    @Query("UPDATE post SET like_count= :likeCount, has_liked = 1 WHERE id=:postId")
    fun likePost(postId: Long, likeCount: Int)

    // TODO: Remove when api/v1/user/420/photos works correctly
    @Query("UPDATE post SET like_count= :likeCount, has_liked = 1, like_list = :likeList WHERE id=:postId")
    fun likePost(postId: Long, likeCount: Int, likeList: String)

    @Query("UPDATE post SET like_count= :likeCount, has_liked = 0 WHERE id=:postId")
    fun unlikePost(postId: Long, likeCount: Int)

    // TODO: Remove when api/v1/user/420/photos works correctly
    @Query("UPDATE post SET like_count= :likeCount, has_liked = 0, like_list = :likeList WHERE id=:postId")
    fun unlikePost(postId: Long, likeCount: Int, likeList: String)

    @Query("UPDATE post SET share_count= :shareCount, has_shared = 1 WHERE id=:postId")
    fun sharePost(postId: Long, shareCount: Int)

    @Query("DELETE FROM post WHERE shared_id=:sharedId")
    fun unsharePost(sharedId: Int)

    @Query("DELETE FROM post WHERE user_id=:blockedUserId OR original_user_id=:blockedUserId")
    fun deleteAllPostsByUser(blockedUserId: Int)

    @Query("UPDATE post SET comment_count=comment_count+1 WHERE id=:postId")
    fun increaseCommentCount(postId: Long)

    @Query("UPDATE post SET comment_count=:commentCount WHERE id=:postId")
    fun updateCommentCount(postId: Long, commentCount: Int)

    @Transaction
    fun decreaseCommentCount(postId: Long) {
        val post = getPostSync(postId)
        var commentsCount = (post?.comment_count ?: 1) - 1
        if (commentsCount < 0) {
            commentsCount = 0
        }
        updateCommentCount(postId, commentsCount)
    }

    @Query("DELETE FROM post")
    fun deleteAllPosts()

    @Transaction
    fun savePosts(result: List<Post>, type: FeedSource.Type, isFeedExperimental: Boolean = false) {
        saveFeedSource(
            FeedSource(FeedSource.Type.MAIN),
            FeedSource(FeedSource.Type.USER),
            FeedSource(FeedSource.Type.COMMUNITY),
            FeedSource(FeedSource.Type.USER_PHOTOS),
            FeedSource(FeedSource.Type.USER_VIDEOS),
            FeedSource(FeedSource.Type.DISCOVERY_FEED),
            FeedSource(FeedSource.Type.HASHTAG),
            FeedSource(FeedSource.Type.ORDER)
        )
        upsertPosts(*result.toTypedArray())
        saveFeedPostSource(*result.map { FeedSourcePostJoin(type, it.id) }.toTypedArray())
        if (type == FeedSource.Type.ORDER && isFeedExperimental) {
            // insert posts order
            val numberOfConflicts = upsertOrder(*result.map { FeedOrder( postId = it.id) }.toTypedArray())

            // get last inserted orderIds
            val orderIds = getOrderIds(10 - numberOfConflicts).reversed()

            // insert into join table
            upsertFeedSourceOrder(*orderIds.map { feedOrder -> FeedSourceOrderPostJoin(type, feedOrder.postId, feedOrder.order) }.toTypedArray())
        }
    }

    @Query("SELECT * FROM feed_order ORDER BY `order` DESC LIMIT :lastInserted ")
    fun getOrderIds(lastInserted:Int): List<FeedOrder>

    @Query("DELETE FROM feed_order")
    fun clearOrderTable()

    @Transaction
    fun removeCache() {
        deleteAllPosts()
        clearOrderTable()
        resetAutoincrement()
    }

    @Query("DELETE FROM sqlite_sequence WHERE name = 'feed_order'")
    fun resetAutoincrement()

    @Transaction
    fun savePosts(result: Post, type: FeedSource.Type) {
        saveFeedSource(
            FeedSource(FeedSource.Type.MAIN),
            FeedSource(FeedSource.Type.USER),
            FeedSource(FeedSource.Type.COMMUNITY),
            FeedSource(FeedSource.Type.USER_PHOTOS),
            FeedSource(FeedSource.Type.USER_VIDEOS),
            FeedSource(FeedSource.Type.DISCOVERY_FEED),
            FeedSource(FeedSource.Type.HASHTAG),
            FeedSource(FeedSource.Type.ORDER)
        )
        upsertPosts(result)
        saveFeedPostSource(FeedSourcePostJoin(type, result.id))
    }

}