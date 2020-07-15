package net.zomis.games.server2.ais

import net.zomis.games.ais.AIFactory
import net.zomis.games.server2.ais.gamescorers.*

class ServerScoringAIs {
    fun ais(): List<AIFactory<out Any>> {
        return listOf(
            SplendorScorers.ais(),
            AvalonScorers.ais(),
            DungeonMayhemScorers.ais(), SkullScorers.ais(),
            URScorers.ais(),
            ArtaxScorers.ais(),
            LiarsDiceScorer.ais(),
            HanabiScorers.ais()
        ).flatten()
    }

}