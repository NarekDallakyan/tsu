package social.tsu.android.helper

import android.graphics.drawable.Drawable

object UserProfileImageCache: Cache<String, Drawable> {

    private var cache: HashMap<String, Drawable> = HashMap()

    override val size: Int
        get() = cache.size

    override fun set(key: String, value: Drawable) {
        cache.set(key, value)
    }

    override fun get(key: String): Drawable? {
        return cache.get(key)
    }

    override fun remove(key: String): Drawable? {
        return cache.remove(key)
    }

    override fun clear() {
        cache.clear()
    }

}