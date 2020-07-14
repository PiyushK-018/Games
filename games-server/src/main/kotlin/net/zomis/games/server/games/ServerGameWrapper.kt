package net.zomis.games.server.games

import klog.KLoggers
import net.zomis.games.dsl.GameEntryPoint
import net.zomis.games.dsl.GameplayCallbacks
import net.zomis.games.server2.Client
import net.zomis.games.server2.MessageRouter
import net.zomis.games.server2.db.DBIntegration
import net.zomis.games.server2.invites.InviteOptions

interface ServerGameLoader<T: Any> {
    fun loadGame(key: String): ServerGameWrapper<T>?
}
interface ServerGameWrapperCallback<T: Any> {
    val gameplayCallbacks: (ServerGame<T>) -> List<GameplayCallbacks<Any>>
    val gameLoaders: List<ServerGameLoader<T>>
    fun remove(key: String)
    val clients: ClientList
}

class ServerGameWrapper<T : Any>(val callback: ServerGameWrapperCallback<T>, val entryPoint: GameEntryPoint<T>, val id: String): ServerGameInviteCallback<T> {

    private val logger = KLoggers.logger(this)

    fun createInvite(host: Client, options: InviteOptions) {
        val invite = ServerGameInvite(this, entryPoint, id, host, options)
        invite.broadcastInviteView()
        this.invite = invite
    }

    val router = MessageRouter(this)
        .route("invite") { invite?.router }
        .route("game") { game?.router }

    var invite: ServerGameInvite<T>? = null
    var game: ServerGame<T>? = null

    override fun startGame(invite: ServerGameInvite<T>) {
        logger.info { "Starting game for invite $invite" }
        val game = ServerGame(callback, entryPoint, id, invite.inviteOptions)
        game.playerManagement.addPlayers(invite.clients().map { ServerGamePlayer(it) })
        this.game = game
        this.invite = null
        game.gameFirstStarted()
    }

    override val clients: ClientList get() = callback.clients
    override fun remove(key: String) = callback.remove(key)
    override val gameplayCallbacks: (ServerGame<T>) -> List<GameplayCallbacks<Any>> get() = callback.gameplayCallbacks
    override val gameLoaders: List<ServerGameLoader<T>> get() = callback.gameLoaders

}