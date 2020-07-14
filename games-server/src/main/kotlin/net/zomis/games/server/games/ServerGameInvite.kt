package net.zomis.games.server.games

import klog.KLoggers
import net.zomis.games.dsl.GameEntryPoint
import net.zomis.games.server2.Client
import net.zomis.games.server2.ClientJsonMessage
import net.zomis.games.server2.MessageRouter
import net.zomis.games.server2.invites.InviteOptions

interface ServerGameInviteCallback<T: Any>: ServerGameWrapperCallback<T> {
    fun startGame(invite: ServerGameInvite<T>)
}

class ServerGameInvite<T : Any>(
    val callback: ServerGameInviteCallback<T>,
    gameEntryPoint: GameEntryPoint<T>,
    val id: String,
    val host: Client,
    val inviteOptions: InviteOptions
) {
    val gameType = gameEntryPoint.gameType
    val playerRange = gameEntryPoint.setup().playersCount
    var cancelled: Boolean = false
    private val logger = KLoggers.logger(this)

    private val awaiting: MutableList<Client> = mutableListOf()
    val accepted: MutableList<Client> = mutableListOf()

    val router = MessageRouter(this)
        .handler("respond", this::respond)
        .handler("send", this::sendInvite)
        .handler("start", this::startInvite)
        .handler("cancel", this::cancelInvite)
        .handler("view", this::sendInviteView)

    fun sendInvite(message: ClientJsonMessage) {
        if (cancelled) throw IllegalStateException("Invite is cancelled")
        val inviteTargets = message.data.get("invite")
        val targetClients = inviteTargets.map { it.asText() }.map {playerId ->
            callback.clients.findPlayerId(playerId)
        }.filterIsInstance<Client>().toMutableList()
        this.sendInviteTo(targetClients)
        broadcastInviteView()
    }

    fun sendInviteTo(targetClients: List<Client>) { // It is possible to invite the same AI twice, therefore a list
        if (cancelled) throw IllegalStateException("Invite is cancelled")
        logger.info { "Sending invite $this to $targetClients" }
        this.awaiting.addAll(targetClients)
        targetClients.forEach {
            this.host.send(mapOf("type" to "InviteStatus", "playerId" to it.playerId.toString(), "status" to "pending", "inviteId" to this.id))
            it.send(mapOf("type" to "Invite", "host" to this.host.name, "game" to this.gameType, "inviteId" to this.id))
        }
        this.broadcastInviteView()
    }

    fun sendInviteView(message: ClientJsonMessage) = message.client.send(inviteViewMessage())

    fun inviteViewMessage(): Map<String, Any?> = mapOf(
        "type" to "InviteView",
        "inviteId" to this.id,
        "gameType" to this.gameType,
        "cancelled" to this.cancelled,
        "minPlayers" to this.playerRange.first,
        "maxPlayers" to this.playerRange.last,
        "options" to null,
        "gameOptions" to this.inviteOptions.gameOptions,
        "host" to this.host.toMessage(),
        "players" to (listOf(this.host) + this.accepted).map { it.toMessage().plus("playerOptions" to null) },
        "invited" to this.awaiting.map { it.toMessage() }
    )

    fun broadcastInviteView() {
        val clients = listOf(this.host) + this.accepted + this.awaiting
        val message = inviteViewMessage()
        clients.forEach { it.send(message) }
    }

    fun startInvite(message: ClientJsonMessage) {
        if (message.client != this.host) throw IllegalArgumentException("Only invite host can start game")

        this.startCheck()
    }

    fun startCheck() {
        if (cancelled) throw IllegalStateException("Invite is cancelled")
        if (playerCount() in playerRange) {
            callback.startGame(this)
        } else {
            throw IllegalStateException("Expecting $playerRange players but current is ${playerCount()}")
        }
    }

    fun cancelInvite(message: ClientJsonMessage) {
        if (message.client != this.host) throw IllegalArgumentException("Only invite host can cancel invite")

        logger.info { "Cancelling invite $this" }
        this.cancelled = true
        val inviteCancelledMessage = mapOf("type" to "InviteCancelled", "inviteId" to this.id)
        this.awaiting.forEach {cl ->
            cl.send(inviteCancelledMessage)
        }
        this.accepted.forEach {cl ->
            cl.send(inviteCancelledMessage)
        }
        this.host.send(inviteCancelledMessage)
        this.broadcastInviteView()

        callback.remove(this.id)
    }

    fun respond(message: ClientJsonMessage) {
        if (cancelled) throw IllegalStateException("Invite is cancelled")
        val response = message.data.get("accepted").asBoolean()
        this.respond(message.client, response)
    }

    fun respond(client: Client, accepted: Boolean) {
        if (cancelled) throw IllegalStateException("Invite is cancelled")
        logger.info { "Client $client responding to invite $this: $accepted" }
        this.host.send(mapOf(
            "type" to "InviteResponse",
            "inviteId" to this.id,
            "playerId" to client.playerId,
            "accepted" to accepted
        ))
        this.awaiting.remove(client)
        if (accepted) {
            this.accepted.add(client)
        }
        broadcastInviteView()
        if (accepted && playerCount() >= playerRange.last) { // Ignore host in this check
            this.startCheck()
        }
    }

    fun playerCount(): Int = 1 + this.accepted.size
    fun clients(): List<Client> = listOf(this.host) + this.accepted

}
