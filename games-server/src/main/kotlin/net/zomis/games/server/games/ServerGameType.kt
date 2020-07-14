package net.zomis.games.server.games

import klog.KLoggers
import net.zomis.games.dsl.GameEntryPoint
import net.zomis.games.dsl.GameplayCallbacks
import net.zomis.games.server.IdHandler
import net.zomis.games.server2.Client
import net.zomis.games.server2.MessageRouter
import net.zomis.games.server2.invites.InviteOptions

typealias GameIdGenerator = () -> String

interface ServerGameTypeCallback {
    val idGenerator: GameIdGenerator
}

class ServerGameType<T : Any>(
    val callback: ServerGameTypeCallback,
    val gameEntryPoint: GameEntryPoint<T>
): ServerGameInvitePreparationCallback<T>, ServerGameWrapperCallback<T> {

    private val logger = KLoggers.logger(this)

    val gameTypeName = gameEntryPoint.gameType
    val invites = ServerGameInvitePreparation(this, gameEntryPoint)
    val router = MessageRouter(this)
        .route("invites", this.invites.router)
        .dynamic { key -> idHandler[key]?.router ?: loadGame(key)?.router
            ?: throw IllegalArgumentException("Unable to load game: $key in $this")
        }

    internal val lobby = ServerGameLobby()

    override val idHandler: IdHandler<ServerGameWrapper<T>> = IdHandler()
    override val clients: ClientList get() = lobby.clients

    private val ais = mutableMapOf<String, ServerGameAIFactory>()

    fun clientDisconnected(client: Client) {
        lobby.removeClient(client)
    }

    override fun remove(key: String) = idHandler.remove(key)

    override fun toString(): String = "ServerGameType($gameTypeName)"

    override fun createInvite(host: Client, options: InviteOptions): ServerGameInvite<T> {
        val inviteId = this.callback.idGenerator()
        logger.info { "Creating invite for $gameTypeName id $inviteId host $host" }
        val wrapper = ServerGameWrapper(this, gameEntryPoint, inviteId)
        wrapper.createInvite(host, options)
        idHandler.add(inviteId, wrapper)
        return wrapper.invite!!
    }

    private fun loadGame(gameId: String): ServerGameWrapper<T>? {
        return null
//        val dbGame = callback.gameLoader(gameId) ?: return null
//        val gameOptions = dbGame.summary.gameConfig
//        val loadGameOptions = InviteOptions(false, InviteTurnOrder.ORDERED, -1, gameOptions, true)
//        val serverGame = ServerGame(callback, this, gameId, loadGameOptions)
//        serverGame.setMoveIndex(dbGame.moveHistory.size)
//        serverGame.obj = GamesImpl.game(gameSpec).replay(dbGame.replayData(), GamesServer.replayStorage.database(dbIntegration!!, gameId)).replayable()
//        runningGames[serverGame.gameId] = serverGame
//
//        fun findOrCreatePlayers(playersInGame: List<PlayerInGame>): Collection<Client> {
//            return playersInGame.map {player ->
//                val playerId = player.player!!.playerId
//                val name = player.player.name
//                val uuid = UUID.fromString(playerId)
//                gameClients()?.list()?.find { it.playerId.toString() == playerId }
//                        ?: FakeClient(uuid).also { it.updateInfo(name, uuid) }
//            }
//        }
//        serverGame.players.addAll(findOrCreatePlayers(dbGame.summary.playersInGame))
//        serverGame.sendGameStartedMessages()
//         Do NOT call GameStartedEvent as that will trigger database save
//
//        return serverGame
    }

    // TODO: Add callbacks and game loaders
    override val gameplayCallbacks: (ServerGame<T>) -> List<GameplayCallbacks<Any>>
        get() = { emptyList() }

    override val gameLoaders: List<ServerGameLoader<T>>
        get() = listOf()

}
/*
Needs integrations:
 - DbIntegration
 - Executor?


GameTypes
  - Keep a list of clients. On disconnect, kick them out
Games/Invites
  - Replay ??
  - Game?
    ServerGame
      Dsl Game System
    Action handler
  - Invite?


*/
