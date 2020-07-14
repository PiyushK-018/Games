package net.zomis.games.server.games

import net.zomis.games.server2.Client
import net.zomis.games.server2.invites.playerMessage
import net.zomis.games.server2.send

data class ClientList(private val clients: MutableSet<Client> = mutableSetOf()) {
    fun findPlayerId(playerId: String) = clients.firstOrNull { it.playerId.toString() == playerId }
    fun add(client: Client) {
        clients.removeAll(clients.filter { it.playerId == client.playerId })
        clients.add(client)
    }
    fun remove(client: Client) = clients.remove(client)
    fun list(): List<Client> = clients.toList()
    fun broadcast(message: Map<String, Any?>) = this.clients.send(message)
    fun contains(client: Client): Boolean = this.clients.contains(client)
}

class ServerGameLobby {

    val clients = ClientList()

    private fun disconnectedMessage(client: Client): Map<String, Any?> = mapOf(
        "type" to "LobbyChange",
        "player" to playerMessage(client),
        "action" to "left"
    )

    fun removeClient(client: Client) {
        this.clients.remove(client)
        this.clients.broadcast(disconnectedMessage(client))
    }

}