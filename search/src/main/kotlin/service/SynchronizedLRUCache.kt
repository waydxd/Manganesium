import java.util.LinkedHashMap

/**
 * Thread-safe LRU Cache implementation using LinkedHashMap with synchronized access.
 */
class SynchronizedLRUCache<K, V>(private val capacity: Int) {
    private val cache = object : LinkedHashMap<K, V>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>?): Boolean {
            return size > capacity
        }
    }

    /**
     * Retrieves a value from the cache, or null if not present.
     */
    fun get(key: K): V? = synchronized(cache) {
        cache[key]
    }

    /**
     * Stores a key-value pair in the cache.
     */
    fun put(key: K, value: V) = synchronized(cache) {
        cache[key] = value
    }

    /**
     * Retrieves a value from the cache, computing and storing it if absent.
     */
    fun getOrPut(key: K, defaultValue: () -> V): V = synchronized(cache) {
        val value = cache[key]
        if (value != null) return@synchronized value
        val newValue = defaultValue()
        cache[key] = newValue
        newValue
    }

    /**
     * Returns the current size of the cache.
     */
    fun size(): Int = synchronized(cache) {
        return cache.size
    }
}