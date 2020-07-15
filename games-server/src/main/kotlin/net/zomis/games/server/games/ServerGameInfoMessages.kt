package net.zomis.games.server.games

import net.zomis.games.dsl.GameEntryPoint
import net.zomis.games.server2.invites.playerMessage

class ServerGameInfoMessages<T: Any>(val entryPoint: GameEntryPoint<T>, val gameId: String) {

    fun message(type: String): Map<String, Any> = mapOf("type" to type, "gameType" to this.entryPoint.gameType, "gameId" to this.gameId)

    fun gameInfoMessage(players: List<ServerGamePlayer<T>>, indices: List<Int>): Map<String, Any?> {
        val playerData = players.asSequence().map { playerMessage(it.client) }.toList()
        return message("GameInfo")
            .plus("yourIndex" to indices.singleOrNull())
            .plus("yourIndices" to indices)
            .plus("players" to playerData)
    }

}