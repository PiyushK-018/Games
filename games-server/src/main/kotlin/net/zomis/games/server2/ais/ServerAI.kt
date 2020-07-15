package net.zomis.games.server2.ais

import klog.KLoggers
import net.zomis.games.ais.GameController
import net.zomis.games.ais.GameListeningController
import net.zomis.games.common.isObserver
import net.zomis.games.dsl.*
import net.zomis.games.dsl.impl.GameControllerContext
import net.zomis.games.server.games.ServerGame
import net.zomis.games.server2.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

data class AIMoveRequest<T: Any>(val client: Client, val game: ServerGame<T>)
data class DelayedAIMoves<T: Any>(val game: ServerGame<T>, val action: Actionable<T, Any>)

val ServerAIProvider = "server-ai"

class AIClient(private val response: (ClientJsonMessage) -> Unit): Client() {
    private val logger = KLoggers.logger(this)
    override fun sendData(data: String) {
        val json = mapper.readTree(data)
        if (json.getTextOrDefault("type", "") != "Invite") {
            return
        }
        val inviteId = json.get("inviteId").asText()
        val gameType = json.get("game").asText()
        val outgoingData = mapper.readTree(mapper.writeValueAsString(
                mapOf("route" to "games/$gameType/$inviteId/invite/respond", "accepted" to true)))
        response(ClientJsonMessage(this, outgoingData))
    }
}

class ServerAI<T: Any>(
    private val executor: ScheduledExecutorService,
    val client: AIClient,
    val gameType: String,
    val factory: (ServerGame<T>) -> GameListeningController<T>
) {

    val name: String get() = client.name!!
    private val logger = KLoggers.logger(this)

    fun createListener(game: ServerGame<T>): GameplayCallbacks<T> {
        return AIGameCallback(executor, game, this.client, factory.invoke(game))
    }

    class AIGameCallback<T: Any>(private val executor: ScheduledExecutorService,
                                 val game: ServerGame<T>, val client: Client,
                                 listeningController: GameListeningController<T>
    ): GameplayCallbacks<T>(), GameListeningController<T> {
        private val logger = KLoggers.logger(this)
        override val controller: GameController<T> = listeningController.controller
        override val listener: GameplayCallbacks<T> = GameplayCallbacksList(listOf(listeningController.listener, this))

        override fun startedState(playerCount: Int, config: Any, state: GameSituationState) {
            this.checkAIMove()
        }

        override fun onMove(actionIndex: Int, action: Actionable<T, Any>, actionReplay: ActionReplay) {
            this.checkAIMove()
        }

        private fun checkAIMove() {
            val playerIndices = game.playerManagement.playersInGame.indices.filter {
                !game.playerManagement.playerIndex(client, it).isObserver()
            }
            if (playerIndices.isEmpty()) {
                return
            }
            executor.submit {
                try {
                    val aiMoves = playerIndices.map {
                        val context = GameControllerContext(game.replayable!!.game, it)
                        controller.invoke(context)
                    }.filterNotNull()
                    aiMoves.forEach { singleAIMoves ->
                        val command = {
                            game.replayable!!.perform(singleAIMoves)
                        }
                        executor.schedule(command, 1000, TimeUnit.MILLISECONDS)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Unable to make move for $this in $game" }
                }
            }
        }
    }

}
