package net.zomis.games.server.games

import net.zomis.games.PlayerElimination
import net.zomis.games.dsl.ActionReplay
import net.zomis.games.dsl.Actionable
import net.zomis.games.dsl.GameplayCallbacks
import net.zomis.games.dsl.impl.ActionLogEntry
import net.zomis.games.dsl.impl.LogEntry

class ServerGameGameplayCallback<T: Any>(private val serverGame: ServerGame<T>) : GameplayCallbacks<T>() {

    override fun onMove(actionIndex: Int, action: Actionable<T, Any>, actionReplay: ActionReplay) {
        // Does Client care about game moves ? It might to reset the possible actions, otherwise no.
        val message = serverGame.gameInfoMessages.message("GameMove")
            .plus("player" to action.playerIndex)
            .plus("actionType" to action.actionType)
        // Some action types contain hidden information (such as in Skull) so don't send information about move
        serverGame.playerManagement.broadcast { message }
    }

    override fun onElimination(elimination: PlayerElimination) {
        val message = serverGame.gameInfoMessages.message("PlayerEliminated")
            .plus("player" to elimination.playerIndex)
            .plus("winner" to elimination.winResult.isWinner())
            .plus("winResult" to elimination.winResult.name)
            .plus("position" to elimination.position)

        serverGame.playerManagement.broadcast { message }
    }

    override fun onLog(log: List<ActionLogEntry>) {
        log.forEach { sendLogs(it) }
    }

    private fun sendLogs(log: ActionLogEntry) {
        val yourLog = logEntryMessage(log.secret ?: log.public)
        val othersLog = logEntryMessage(log.public)
        serverGame.playerManagement.allClients().forEach { (index, client) ->
            val msg = if (index == log.playerIndex) yourLog else othersLog
            msg?.let { client.send(it) }
        }
    }

    private fun logEntryMessage(entry: LogEntry?): Map<String, Any>? {
        if (entry == null) return null
        return serverGame.gameInfoMessages.message("ActionLog")
            .plus("highlights" to entry.highlights.associateWith { true })
            .plus("parts" to entry.parts)
    }

    override fun onGameOver() {
        val message = serverGame.gameInfoMessages.message("GameEnded")
        serverGame.playerManagement.broadcast { message }
    }

}
