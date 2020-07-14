package net.zomis.games.server

import com.fasterxml.jackson.databind.node.ArrayNode
import klog.KLoggers
import net.zomis.games.dsl.GameEntryPoint
import net.zomis.games.dsl.GameSpec
import net.zomis.games.dsl.GamesImpl
import net.zomis.games.server.games.GameIdGenerator
import net.zomis.games.server.games.ServerGameType
import net.zomis.games.server.games.ServerGameTypeCallback
import net.zomis.games.server2.*
import net.zomis.games.server2.db.DBIntegration
import net.zomis.games.server2.invites.playerMessage
import java.util.UUID
import java.util.concurrent.ScheduledExecutorService

class ServerGamesSystem(val executor: ScheduledExecutorService, val dbIntegration: DBIntegration?): ServerGameTypeCallback {

    private val logger = KLoggers.logger(this)

    private val gameTypes: MutableMap<String, ServerGameType<Any>> = mutableMapOf()
    private val dynamicRouter: MessageRouterDynamic<ServerGameType<Any>> = { key -> this.getGameType(key)?.router ?: throw IllegalArgumentException("No such gameType: $key") }
    val lobby = Lobby(gameTypes)
    val router = MessageRouter(this)
        .dynamic(dynamicRouter)

    fun getGameType(gameType: String): ServerGameType<Any>? = gameTypes[gameType]

    fun addGames(values: Collection<GameSpec<out Any>>): ServerGamesSystem {
        values.forEach {
            if (gameTypes[it.name] != null) throw IllegalStateException("Game ${it.name} already exists")
            logger.info { "Adding gameType ${it.name}" }

            gameTypes[it.name] = ServerGameType(this, GamesImpl.game(it) as GameEntryPoint<Any>)
        }
        return this
    }

    fun clientDisconnects(client: Client) = gameTypes.values.forEach { it.clientDisconnected(client) }

    class Lobby(val gameTypes: Map<String, ServerGameType<Any>>) {
        private val logger = KLoggers.logger(this)

        val router = MessageRouter(this)
            .handler("list", this::listLobby)
            .handler("join", this::joinLobby)

        private fun listLobby(message: ClientJsonMessage) {
            val interestingGames = gameTypes.values.filter { it.lobby.clients.contains(message.client) }

            // Return Map<GameType, List<Client name>>
            val resultingMap = interestingGames.associate { serverGameType ->
                serverGameType.gameTypeName to serverGameType.lobby.clients.list()
                    .filter { it != message.client }
                    .filter { it.name != null }
                    .map { playerMessage(it) }
            }
            message.client.send(mapOf("type" to "Lobby", "users" to resultingMap))
        }

        private fun joinLobby(message: ClientJsonMessage) {
            val interestingGameTypes = (message.data.get("gameTypes") as ArrayNode)
                .map { it.asText() }.map { gameTypeName ->
                    this.gameTypes[gameTypeName].also { gt ->
                        if (gt == null) logger.warn { "Client requested gameType $gameTypeName which does not exist." }
                    }
                }

            val newClientMessage = newClientMessage(message.client, interestingGameTypes.filterNotNull().map { it.gameTypeName })
            interestingGameTypes.filterNotNull().flatMap {
                it.lobby.clients.list()
            }.toSet().send(newClientMessage)

            interestingGameTypes.filterNotNull().forEach {
                it.lobby.clients.add(message.client)
            }
            logger.info { "${message.client.name} joined lobby for $interestingGameTypes" }
        }

        private fun newClientMessage(client: Client, interestingGameTypes: Collection<String>): Map<String, Any?> {
            return mapOf(
                    "type" to "LobbyChange",
                    "player" to playerMessage(client),
                    "action" to "joined",
                    "gameTypes" to interestingGameTypes
            )
        }
    }

    override val idGenerator: GameIdGenerator
        get() = { UUID.randomUUID().toString() }

}