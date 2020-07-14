package net.zomis.games.server2.ais

import net.zomis.core.events.EventSystem
import net.zomis.games.dsl.impl.GameController
import net.zomis.games.dsl.impl.GameControllerContext

class AIFactoryScoring {

    fun <T: Any> createAI(events: EventSystem, gameType: String, name: String, controller: GameController<T>) {
        ServerAI<T>(gameType, name) { game, index ->
            val obj = game.replayable!!.game
            val controllerContext = GameControllerContext(obj, index)
            val action = controller(controllerContext)
            action
        }.register(events)
    }

}