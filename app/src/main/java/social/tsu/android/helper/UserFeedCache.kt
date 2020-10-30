package social.tsu.android.helper

import social.tsu.android.data.local.entity.Post

abstract class BaseUserFeedCache: Cache<Long, List<Post>> {

    private var cache: HashMap<Long, List<Post>> = HashMap()

    override val size: Int
        get() = cache.size

    fun allKeys(): List<Long> {
        return cache.keys.toList()
    }

    override fun set(key: Long, value: List<Post>) {
        cache.set(key, value)
    }

    override fun get(key: Long): List<Post> {
        return cache.get(key)?: emptyList()
    }

    override fun remove(key: Long): List<Post>? {
        return cache.remove(key)
    }

    override fun clear() {
        cache.clear()
    }

}

// TODO: Rework with loading from database
object UserPhotoFeedCache : BaseUserFeedCache()
object UserFeedCache : BaseUserFeedCache()