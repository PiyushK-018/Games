package net.zomis.games.server.games.view

import klog.KLoggers
import net.zomis.games.server.games.ServerGame
import net.zomis.games.server.games.ServerGameInfoMessages
import net.zomis.games.server2.ClientJsonMessage
import net.zomis.games.server2.getTextOrDefault

class ServerGameView<T: Any>(val serverGame: ServerGame<T>, val gameInfoMessages: ServerGameInfoMessages<T>) {

    private val logger = KLoggers.logger(this)

    fun view(message: ClientJsonMessage) {
        val obj = serverGame.replayable!!.game
        val viewIndex = serverGame.playerManagement.playerIndex(message.client, message.data.get("viewer").asInt())
        logger.info { "Sending view data for ${serverGame.gameId} of type ${serverGame.entryPoint.gameType} to $viewIndex for client ${message.client}" }
        val view = obj.view(viewIndex)
        message.client.send(
            gameInfoMessages.message("GameView").plus("viewer" to viewIndex).plus("view" to view)
        )
    }

    fun viewRequest(message: ClientJsonMessage) {
        val viewIndex = serverGame.playerManagement.playerIndex(message.client, message.data.get("viewer").asInt())
        val viewDetailsResult = serverGame.replayable!!.game.viewRequest(viewIndex,
            message.data.getTextOrDefault("viewRequest", ""), emptyMap()
        )

        message.client.send(
            gameInfoMessages.message("GameViewDetails")
                .plus("viewer" to viewIndex)
                .plus("details" to viewDetailsResult)
        )
    }


}