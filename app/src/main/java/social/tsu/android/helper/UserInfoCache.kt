package social.tsu.android.helper

import social.tsu.android.network.model.UserProfile

object UserInfoCache: Cache<Int, UserProfile> {

    private var cache: HashMap<Int, UserProfile> = HashMap()

    override val size: Int
    get() = cache.size

    override fun set(key: Int, value: UserProfile) {
        cache.set(key, value)
    }

    override fun get(key: Int): UserProfile? {
        return cache.get(key)
    }

    override fun remove(key: Int): UserProfile? {
        return cache.remove(key)
    }

    override fun clear() {
        cache.clear()
    }
}