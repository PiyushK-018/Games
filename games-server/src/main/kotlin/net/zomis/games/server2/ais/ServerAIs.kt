package net.zomis.games.server2.ais

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import klog.KLoggers
import net.zomis.core.events.EventSystem
import net.zomis.games.ais.*
import net.zomis.games.dsl.*
import net.zomis.games.dsl.impl.GameImpl
import net.zomis.games.server.ServerGamesSystem
import net.zomis.games.server.games.ServerGame
import net.zomis.games.server2.ClientConnected
import net.zomis.games.server2.ClientJsonMessage
import net.zomis.games.server2.ClientLoginEvent
import net.zomis.games.server2.ShutdownEvent
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ServerAIs(private val dslGameTypes: Collection<GameSpec<out Any>>) {
    private val logger = KLoggers.logger(this)

    fun <T: Any> randomActionable(game: GameImpl<T>, playerIndex: Int): Actionable<T, Any>? {
        val actionTypes = game.actions.types()
        val actions = actionTypes.flatMap {actionType ->
            actionType.availableActions(playerIndex, null)
        }
        if (actions.isEmpty()) {
            return null
        }
        return actions.random()
    }

    fun ais(): List<AIFactory<out Any>> {
//        ServerAlphaBetaAIs(aiRepository).setup(events)
//        ServerScoringAIs(aiRepository).setup(events)
        return dslGameTypes.map {gameSpec ->
            AIFactorySimple<Any>(gameSpec.name, "#AI_Random") { randomActionable(it.game, it.playerIndex) }
        }.plus(ServerAlphaBetaAIs().ais())
    }

    fun aiClient(name: String, gameTypes: List<String>, events: EventSystem): AIClient {
        val client = AIClient { events.execute(it) }
        events.execute(ClientConnected(client))
        client.updateInfo(name, UUID.randomUUID())
        events.execute(ClientLoginEvent(client, name, name, ServerAIProvider, name))
        val mapper = jacksonObjectMapper()
        val interestingGames = mapper.readTree(mapper.writeValueAsString(mapOf("route" to "lobby/join",
            "gameTypes" to gameTypes, "maxGames" to 100
        )))
        events.execute(ClientJsonMessage(client, interestingGames))
        return client
    }

    val executor = Executors.newScheduledThreadPool(2)
    fun addAIs(events: EventSystem, gamesSystem2: ServerGamesSystem) {
        val ais = this.ais().groupBy { it.playerName }
        ais.entries.forEach { aiEntry ->
            val playerName = aiEntry.key
            val client = aiClient(playerName, aiEntry.value.map { it.gameType }, events)
            aiEntry.value.forEach { factory ->
                gamesSystem2.addAIs(listOf(ServerAI(executor, client, factory.gameType) {
                    factory.createController() as GameListeningController<Any>
                }))
            }
        }
        events.listen("shut down AI executor", ShutdownEvent::class, {true}) {
            executor.shutdown()
        }
    }

}
