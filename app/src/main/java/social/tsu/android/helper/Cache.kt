package social.tsu.android.helper

interface Cache<K, V> {
    val size: Int
    operator fun set(key: K, value: V)

    operator fun get(key: K): V?

    fun remove(key: K): V?

    fun clear()
}