package net.zomis.games.server2.games

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import klog.KLoggers
import net.zomis.games.common.isObserver
import net.zomis.games.dsl.GameReplayableImpl
import net.zomis.games.dsl.impl.ActionInfoByKey
import net.zomis.games.dsl.impl.GameImpl
import net.zomis.games.server.games.ServerGame
import net.zomis.games.server2.Client
import net.zomis.games.server2.ClientJsonMessage

class FrontendActionInfo(val keys: ActionInfoByKey)
class ActionList<T: Any>(val playerIndex: Int, val game: ServerGame<T>, val actions: FrontendActionInfo)

object ActionListHelper {
    fun <T: Any> availableActionsMessage(obj: GameImpl<T>, playerIndex: Int, moveType: String?, chosen: List<Any>?): FrontendActionInfo {
        return if (moveType != null) {
            val actionType = obj.actions.type(moveType)!!
            val actionInfo = actionType.actionInfoKeys(playerIndex, chosen ?: emptyList())
            FrontendActionInfo(actionInfo)
        } else {
            FrontendActionInfo(obj.actions.allActionInfo(playerIndex, chosen ?: emptyList()))
        }
    }
}

class ActionListRequestHandler<T: Any>(val serverGame: ServerGame<T>) {
    private val logger = KLoggers.logger(this)
    private val mapper = jacksonObjectMapper()

    fun <T: Any> availableActionsMessage(obj: GameImpl<T>, playerIndex: Int, moveType: String?, chosen: List<Any>?): FrontendActionInfo
        = ActionListHelper.availableActionsMessage(obj, playerIndex, moveType, chosen)

    fun sendActionList(message: ClientJsonMessage) {
        val actionParams = actionParams(message)
        this.sendActionParams(message.client, actionParams)
    }

    private fun sendActionParams(client: Client, actionParams: ActionList<T>) {
        val game = actionParams.game
        logger.info { "Sending action list data for ${game.gameId} of type ${game.gameTypeName} to ${actionParams.playerIndex}" }
        client.send(
            serverGame.gameInfoMessages.message("ActionList")
                .plus("playerIndex" to actionParams.playerIndex)
                .plus("actions" to actionParams.actions.keys.keys)
        )
    }

    private fun actionParams(message: ClientJsonMessage): ActionList<T> {
        val obj = serverGame.replayable!!.game
        val playerIndex = serverGame.playerManagement.playerIndex(message)
        if (playerIndex.isObserver()) {
            throw IllegalArgumentException("Client ${message.client} does not have index $playerIndex in Game ${serverGame.gameId} of type ${serverGame.gameTypeName}")
        }
        if (obj.isGameOver()) {
            return ActionList(playerIndex!!, serverGame, FrontendActionInfo(ActionInfoByKey(emptyMap())))
        }

        val moveType = message.data.get("moveType")?.asText()
        val chosenJson = message.data.get("chosen") ?: emptyList<JsonNode>()
        val chosen = mutableListOf<Any>()

        for (choiceJson in chosenJson) {
            val actionParams = availableActionsMessage(obj, playerIndex!!, moveType, chosen)
            val actionInfo = actionParams.keys.keys.values.flatten()
            val nextChosenClazz = actionInfo.filter { !it.isParameter }.map { it.serialized::class }.toSet().let {
                if (it.size == 1) { it.single() } else throw IllegalStateException("Expected only one class but found $it in $actionInfo")
            }

            val parameter: Any
            try {
                parameter = mapper.convertValue(choiceJson, nextChosenClazz.java)
            } catch (e: Exception) {
                logger.error(e, "Error reading choice: $choiceJson")
                throw e
            }
            chosen.add(parameter)
        }
        return ActionList(playerIndex!!, serverGame, availableActionsMessage(obj, playerIndex, moveType, chosen))
    }

    fun actionRequest(message: ClientJsonMessage, replayable: GameReplayableImpl<T>) {
        val actionParams = actionParams(message)
        val frontendActionInfo = actionParams.actions.keys.keys.values.flatten()

        val actionInfoKey = if (message.data.has("perform") && message.data["perform"].asBoolean()) frontendActionInfo[0]
            else frontendActionInfo.singleOrNull()?.takeIf { it.isParameter }
        if (actionInfoKey != null) {
            val actionTypeImpl = replayable.game.actions.type(actionInfoKey.actionType)!!
            val action = actionTypeImpl.createActionFromSerialized(actionParams.playerIndex, actionInfoKey.serialized)
            replayable.perform(action)
        } else {
            this.sendActionParams(message.client, actionParams)
        }
    }

}