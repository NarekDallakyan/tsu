package social.tsu.android.helper

import social.tsu.android.data.local.entity.Post

object PostsCache: Cache<Long, Post> {

    private var cache: HashMap<Long, Post> = HashMap()

    override val size: Int
        get() = cache.size

    fun allCases(): Set<Long> {
        return cache.keys
    }

    override fun set(key: Long, value: Post) {
        cache.set(key, value)
    }

    override fun get(key: Long): Post? {
        return cache.get(key)
    }

    override fun remove(key: Long): Post? {
        return cache.remove(key)
    }

    override fun clear() {
        cache.clear()
    }

}