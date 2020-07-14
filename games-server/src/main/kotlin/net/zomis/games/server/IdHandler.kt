package net.zomis.games.server

class IdHandler<E> {

    // TODO: Use a cache for self-deleting items. Use callback to log when it happens, and to send messages.
//    private val gameIdCache = Caffeine.newBuilder()
//        .expireAfterAccess(1, TimeUnit.HOURS)
//        .build { key: String -> ServerGameWrapper(gameEntryPoint, key) }

    val items = mutableMapOf<String, E>()

    fun add(key: String, item: E) { items[key] = item }
    fun remove(key: String) { items.remove(key) }
    operator fun get(key: String): E? = items[key]

}