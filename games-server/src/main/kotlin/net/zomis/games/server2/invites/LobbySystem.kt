package net.zomis.games.server2.invites

import net.zomis.games.server2.*

fun playerMessage(client: Client): Map<String, String> {
    return mapOf(
        "id" to client.playerId.toString(),
        "name" to (client.name ?: "(UNKNOWN)"),
        "picture" to (client.picture ?: "")
    )
}

class LobbySystem {

    /*
    fun sendUnfinishedGames(client: Client) {
        val interestingGames = client.lobbyOptions.interestingGames
        val unfinishedGames = features[UnfinishedGames::class] ?: return

        // Return Map<GameType, List<GameSummary>>
        val games = unfinishedGames.unfinishedGames
            .filter { interestingGames.contains(it.gameType) }
            .filter { it.playersInGame.any { pig ->
                val playerId = pig.player?.playerId ?: ""
                playerId == client.playerId.toString()
            } }
            .groupBy { it.gameType }
            .mapValues { it.value.map { game -> mapOf(
                "GameId" to game.gameId,
                "TimeStarted" to game.timeStarted
            ) } }

        client.send(mapOf("type" to "LobbyUnfinished", "games" to games))
    }
    */

}