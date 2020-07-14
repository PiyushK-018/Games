package net.zomis.games.server.games.actions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.sync.Mutex
import net.zomis.games.common.isObserver
import net.zomis.games.dsl.Actionable
import net.zomis.games.server.games.ServerGame
import net.zomis.games.server2.ClientJsonMessage
import net.zomis.games.server2.games.ActionListRequestHandler
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class ServerGameActions<T : Any>(val serverGame: ServerGame<T>) {

    private val nextMoveIndex = AtomicInteger(0)

    val mutex = Mutex()
    var lastMove: Long = Instant.now().toEpochMilli()

    fun nextMoveIndex(): Int = nextMoveIndex.getAndIncrement()
    fun setMoveIndex(next: Int) = nextMoveIndex.set(next)

    val actionListHandler = ActionListRequestHandler(serverGame)

    fun actionRequest(message: ClientJsonMessage) {
        // Does not matter if it's an incomplete action or not
        this.actionListHandler.actionRequest(message, serverGame.replayable!!)
    }
    private val mapper = jacksonObjectMapper()

    @Deprecated("Replace with action instead. This approach is only *required* for non-DSL games which no longer exists")
    fun moveRequest(message: ClientJsonMessage) {
        val actionType = message.data.get("moveType").asText()
        val actionJson = message.data.get("move")
        val playerIndex = serverGame.playerManagement.playerIndex(message.client, message.data.get("playerIndex").asInt())
        if (playerIndex.isObserver()) {
            throw IllegalArgumentException("Client ${message.client.name} did not send the right playerIndex or is an observer")
        }
        val actionTypeImpl = serverGame.replayable!!.game.actions.type(actionType)!!

        val actionSerialized = mapper.convertValue(actionJson, actionTypeImpl.actionType.serializedType.java)
        val action = actionTypeImpl.createActionFromSerialized(playerIndex!!, actionSerialized)

        serverGame.replayable!!.perform(action)
    }

    fun perform(action: Actionable<T, Any>) {
        serverGame.replayable!!.perform(action)
    }

}
