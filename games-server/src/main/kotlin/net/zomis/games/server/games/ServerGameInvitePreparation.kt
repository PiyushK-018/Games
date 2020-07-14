package net.zomis.games.server.games

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import klog.KLoggers
import net.zomis.games.dsl.GameEntryPoint
import net.zomis.games.server.IdHandler
import net.zomis.games.server2.Client
import net.zomis.games.server2.ClientJsonMessage
import net.zomis.games.server2.MessageRouter
import net.zomis.games.server2.ServerGames
import net.zomis.games.server2.invites.InviteOptions
import net.zomis.games.server2.invites.InviteTurnOrder

interface ServerGameInvitePreparationCallback<T : Any> {
    fun createInvite(host: Client, options: InviteOptions): ServerGameInvite<T>
    val idHandler: IdHandler<ServerGameWrapper<T>>
    val clients: ClientList
}

class ServerGameInvitePreparation<T : Any>(val callback: ServerGameInvitePreparationCallback<T>, val gameEntryPoint: GameEntryPoint<T>) {

    private val logger = KLoggers.logger(this)

    val router = MessageRouter(this)
        .handler("prepare", this::prepareInvite)
        .handler("start", this::createInviteMessage)
        .handler("invite", this::fullInvite)

    private fun fullInvite(message: ClientJsonMessage) {
        val inviteTargets = message.data.get("invite")
        val defaultConfig = gameEntryPoint.setup().getDefaultConfig()
        val invite = callback.createInvite(message.client,
            InviteOptions(false, InviteTurnOrder.ORDERED, -1, defaultConfig, true)
        )
        val targetClients = inviteTargets.map { it.asText() }.map {playerId ->
            callback.clients.findPlayerId(playerId)
        }.filterIsInstance<Client>().toMutableList()
        logger.info { "Found invite targets: $targetClients" }
        invite.sendInviteTo(targetClients)
    }

    private fun createInviteMessage(message: ClientJsonMessage) {
        val options = createInviteOptions(message.data.get("options"), message.data.get("gameOptions"))
        this.callback.createInvite(message.client, options)
    }

    private fun createInviteOptions(invite: JsonNode, gameOptionsNode: JsonNode): InviteOptions {
        val setup = gameEntryPoint.setup()
        val gameOptions = jacksonObjectMapper().convertValue(gameOptionsNode, setup.configClass().java)
        return InviteOptions(
            publicInvite = false,//invite["publicInvite"].asBoolean(),
            turnOrder = InviteTurnOrder.ORDERED,// InviteTurnOrder.values().first { it.name == invite["turnOrder"].asText() },
            timeLimit = -1,
            gameOptions = gameOptions,
            database = true//invite["database"].asBoolean()
        )
    }

    fun prepareInvite(message: ClientJsonMessage) {
        val setup = gameEntryPoint.setup()
        val gameOptions = setup.getDefaultConfig().takeUnless { it == Unit }
        message.client.send(mapOf(
            "type" to "InvitePrepare",
            "gameType" to gameEntryPoint.gameType,
            "playersMin" to setup.playersCount.min(), "playersMax" to setup.playersCount.max(),
            "config" to gameOptions
        ))
    }


}