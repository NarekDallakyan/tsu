package social.tsu.android.helper

import social.tsu.android.data.local.entity.PostPreview


object PostPreviewCache : Cache<String, PostPreview> {

    private var cache: HashMap<String, PostPreview> = HashMap()

    override val size: Int
        get() = cache.size

    fun allCases(): Set<String> {
        return cache.keys
    }

    override fun set(key: String, value: PostPreview) {
        cache[key] = value
    }

    override fun get(key: String): PostPreview? {
        return cache[key]
    }

    override fun remove(key: String): PostPreview? {
        return cache.remove(key)
    }

    override fun clear() {
        cache.clear()
    }

}