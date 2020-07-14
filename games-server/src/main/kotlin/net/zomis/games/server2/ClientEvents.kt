package net.zomis.games.server2

import com.fasterxml.jackson.databind.JsonNode

data class ClientMessage(val client: Client, val message: String)
data class ClientConnected(val client: Client)
data class ClientDisconnected(val client: Client)

data class ClientJsonMessage(val client: Client, val data: JsonNode) {
    fun invalidRequest(error: String): Unit = client.send(mapOf("type" to "ClientError", "message" to error))
}
