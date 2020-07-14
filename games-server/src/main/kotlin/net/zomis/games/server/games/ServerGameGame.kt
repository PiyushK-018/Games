package net.zomis.games.server.games

import klog.KLoggers
import net.zomis.games.common.PlayerIndex
import net.zomis.games.dsl.GameEntryPoint
import net.zomis.games.dsl.GameReplayableImpl
import net.zomis.games.dsl.GameplayCallbacks
import net.zomis.games.server.games.actions.ServerGameActions
import net.zomis.games.server.games.view.ServerGameView
import net.zomis.games.server2.Client
import net.zomis.games.server2.ClientJsonMessage
import net.zomis.games.server2.MessageRouter
import net.zomis.games.server2.invites.InviteOptions
import net.zomis.games.server2.invites.playerMessage

class ServerGamePlayer(val client: Client)

class ServerGame<T : Any>(
    val callback: ServerGameWrapperCallback<T>,
    val entryPoint: GameEntryPoint<T>,
    val gameId: String,
    val inviteOptions: InviteOptions
) {
    val gameTypeName: String = entryPoint.gameType
    private val logger = KLoggers.logger(this)

    var replayable: GameReplayableImpl<T>? = null

    val playerManagement = ServerGamePlayers(this)

    val gameInfoMessages = ServerGameInfoMessages(entryPoint, gameId)
    val viewer = ServerGameView(this, gameInfoMessages)
    val actions = ServerGameActions(this)

    val router = MessageRouter(this)
        .handler("view", this.viewer::view)
        .handler("viewRequest", this.viewer::viewRequest)
        .handler("actionList", this.actions.actionListHandler::sendActionList)
        .handler("join", this.playerManagement::clientJoin)
        .handler("action", this.actions::actionRequest)
        .handler("move", this.actions::moveRequest)

    fun gameFirstStarted() {
        val callbacks = listOf<GameplayCallbacks<T>>(
            ServerGameGameplayCallback(this)
            // TODO: Add stuff here. Send messages, call AIs, database or file I/O
        ).plus(callback.gameplayCallbacks(this) as List<GameplayCallbacks<T>>)
        this.replayable = entryPoint.replayable(this.playerManagement.playersCount, this.inviteOptions.gameOptions, *callbacks.toTypedArray())
//        this.databaseCallback
        this.gameResumed()
    }

    fun gameResumed() {
        this.playerManagement.sendGameInfoMessages()
        logger.info { "Game resumed: $this" }
        // this.aiChecks
    }

    override fun toString(): String = "ServerGame(${entryPoint.gameType} / $gameId)"

}

class ServerGamePlayers<T: Any>(private val serverGame: ServerGame<T>) {
    val playersCount: Int get() = playersInGame.size
    val playersInGame = mutableListOf<ServerGamePlayer>()
    internal val observers: MutableSet<Client> = mutableSetOf()

    fun playerIndex(message: ClientJsonMessage) = this.playerIndex(message.client, message.data["playerIndex"].asInt())
    fun playerIndex(client: Client, playerIndex: Int): PlayerIndex
            = if (playersInGame.getOrNull(playerIndex)?.client == client) playerIndex else null

    fun addPlayers(players: List<ServerGamePlayer>) = playersInGame.addAll(players)

    fun broadcast(message: (Client) -> Any) {
        playersInGame.forEach { it.client.send(message.invoke(it.client)) }
        observers.forEach { it.send(message.invoke(it)) }
    }

    fun clientJoin(message: ClientJsonMessage) {
        // If player should be playing, set as player.
        // Otherwise, set player as observer.

        val playerId = message.client.playerId!!
        val indices = playersInGame.withIndex().filter { it.value.client.playerId == playerId }
        indices.forEach {
            playersInGame[it.index] = ServerGamePlayer(message.client)
        }

        if (indices.isEmpty()) {
            observers.add(message.client)
        }
        message.client.send(serverGame.gameInfoMessages.gameInfoMessage(playersInGame, indices.map { it.index }))
    }

    fun sendGameInfoMessages() {
        val players = this.playersInGame.asSequence().map { playerMessage(it.client) }.toList()
        this.playersInGame.forEachIndexed { index, player ->
            player.client.send(serverGame.gameInfoMessages.message("GameStarted").plus("yourIndex" to index).plus("players" to players))
        }
    }

    fun allClients(): List<Pair<Int?, Client>> = playersInGame.mapIndexed { index, serverGamePlayer ->
        index to serverGamePlayer.client
    }.plus(observers.map { null to it })

}
