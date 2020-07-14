package net.zomis.games.server2.games

/*
class DslGameSystem<T : Any>(val dsl: GameSpec<T>, private val dbIntegration: () -> DBIntegration?) {
    val gameTypeName = dsl.name

    private val mapper = jacksonObjectMapper()
    private val logger = KLoggers.logger(this)

    fun perform() {
        val beforeMoveEliminated = controller.eliminationCallback.eliminations()
        try {
            controller.stateKeeper.clear() // TODO: Remove this and use replayable
            events.execute(PreMoveEvent(it.game, it.player, it.moveType, action.parameter))
            actionType.perform(action)
            if (controller.stateKeeper.logs().size > 20) { throw IllegalStateException("${controller.stateKeeper.logs().size}") }
            controller.stateKeeper.logs().forEach { log -> sendLogs(serverGame, log) }
        } catch (e: Exception) {
            logger.error(e) { "Error processing move $it" }
            events.execute(it.illegalMove("Error occurred while processing move: $e"))
        }
        val recentEliminations = controller.eliminationCallback.eliminations().minus(beforeMoveEliminated)

        events.execute(MoveEvent(it.game, it.player, it.moveType, action.parameter))
        for (elimination in recentEliminations) {
            events.execute(PlayerEliminatedEvent(it.game, elimination.playerIndex,
                    elimination.winResult, elimination.position))
        }

        if (controller.isGameOver()) {
            events.execute(GameEndedEvent(it.game))
        }
    }

    fun setup(events: EventSystem) {
        val entryPoint = GamesImpl.game(dsl)
        events.listen("DslGameSystem $gameTypeName Setup", GameStartedEvent::class, {it.game.gameType.type == gameTypeName}, {
            val dbIntegration = this.dbIntegration()
            it.game.obj = entryPoint.replayable(
                it.game.players.size,
                it.game.gameMeta.gameOptions ?: Unit,
                appropriateReplayListener
            ) as GameReplayableImpl<Any>
        })
    }

}
*/