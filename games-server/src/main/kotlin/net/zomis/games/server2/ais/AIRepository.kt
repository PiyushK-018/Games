package net.zomis.games.server2.ais

import net.zomis.core.events.EventSystem
import net.zomis.games.dsl.impl.GameImpl
import net.zomis.games.scorers.ScorerController

class AIRepository {

    private class AIRepositoryForGame<T: Any> {
        val scoringAIs = mutableMapOf<String, ScorerController<T>>()
        val alphaBetaAIs = mutableMapOf<String, AlphaBetaAIFactory<T>>()
//        val otherAIs = mutableMapOf<String, ServerGameAI<T>>()
    }
    private val gameTypeAIs = mutableMapOf<String, AIRepositoryForGame<Any>>()

    private fun <T: Any> repositoryForGameType(gameType: String): AIRepositoryForGame<T> {
        gameTypeAIs.computeIfAbsent(gameType) { AIRepositoryForGame() }
        return gameTypeAIs[gameType]!! as AIRepositoryForGame<T>
    }

    fun <T: Any> createScoringAI(events: EventSystem, factory: ScorerController<T>) {
        val repo = repositoryForGameType<T>(factory.gameType)
        repo.scoringAIs[factory.playerName] = factory
//        val scoringFactory = AIFactoryScoring()
//        scoringFactory.createAI(events, factory.gameType, factory.name, factory.createController())
    }

    fun <T: Any> createAlphaBetaAIs(events: EventSystem, factory: AlphaBetaAIFactory<T>) {
        val repo = repositoryForGameType<T>(factory.gameType)
        repo.alphaBetaAIs[factory.namePrefix] = factory
        factory.alphaBetaConfigurations().map {
            AIFactoryAlphaBeta(factory, it.first, it.second)
        }
    }

    fun analyze(gameType: String, game: GameImpl<Any>, aiName: String, playerIndex: Int): AIAnalyzeResult? {
        // Find AI in all AI types
        val gameTypeRepo = repositoryForGameType<Any>(gameType)
        val scoring = gameTypeRepo.scoringAIs[aiName]
        if (scoring != null) {
            return AIAnalyze().scoring(game, scoring, playerIndex)
        }

        val alphaBetaConfig = gameTypeRepo.alphaBetaAIs.entries.mapNotNull {entry ->
            val factory = entry.value
            val configs = factory.alphaBetaConfigurations()
            val config = configs.find { factory.aiName(it.first, it.second) == aiName }
            if (config != null) {
                AIAlphaBetaConfig(factory, config.first, config.second)
            } else null
        }.firstOrNull()
        if (alphaBetaConfig != null) {
            return AIAnalyze().alphaBeta(game, alphaBetaConfig, playerIndex)
        }
        return null
    }

    fun queryableAIs(gameType: String): List<String> {
        val gameTypeRepo = repositoryForGameType<Any>(gameType)
        val abNames = gameTypeRepo.alphaBetaAIs.flatMap {factory ->
            val abConfig = factory.value.alphaBetaConfigurations()
            abConfig.map { factory.value.aiName(it.first, it.second) }
        }
        return gameTypeRepo.scoringAIs.keys.sorted() + abNames
    }

}
