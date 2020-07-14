package net.zomis.games.server2.invites

enum class InviteTurnOrder { ORDERED, SHUFFLED }
data class InviteOptions(
    val publicInvite: Boolean,
    val turnOrder: InviteTurnOrder,
    val timeLimit: Int,
    val gameOptions: Any?,
    val database: Boolean
)
