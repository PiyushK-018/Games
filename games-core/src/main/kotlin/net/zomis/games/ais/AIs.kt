package net.zomis.games.ais

import net.zomis.games.dsl.Actionable
import net.zomis.games.dsl.GameplayCallbacks
import net.zomis.games.dsl.impl.GameControllerScope

interface AIFactory<T: Any> {
    val gameType: String
    val playerName: String
    fun createController(): GameListeningController<T>
}
class AIFactorySimple<T : Any>(
    override val gameType: String,
    override val playerName: String,
    val controller: GameController<T>
): AIFactory<T> {
    override fun createController(): GameListeningController<T> = GameIndependentController(controller)
}

typealias GameController<T> = (GameControllerScope<T>) -> Actionable<T, Any>?

interface GameListeningController<T: Any> {
    val controller: GameController<T>
    val listener: GameplayCallbacks<T>
}

class GameIndependentController<T: Any>(override val controller: GameController<T>): GameListeningController<T> {
    override val listener: GameplayCallbacks<T> = GameplayCallbacks()
}
