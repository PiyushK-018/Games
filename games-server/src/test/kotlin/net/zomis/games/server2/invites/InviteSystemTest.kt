package net.zomis.games.server2.invites

import net.zomis.core.events.EventSystem
import net.zomis.games.server2.ClientsByName
import net.zomis.games.server2.games.GameStartedEvent
import net.zomis.games.server2.games.GameSystem
import net.zomis.games.server2.games.GameType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InviteSystemTest {

    lateinit var events: EventSystem
    val host = FakeClient().apply { name = "Host" }
    val invitee = FakeClient().apply { name = "Invited" }

    lateinit var games: GameSystem

    val expect: EventsExpect = EventsExpect()

    @BeforeEach
    fun before() {
        events = EventSystem()
        games = GameSystem(events)
        val clientLookup = ClientsByName()
        events.with(clientLookup::register)
        InviteSystem(games).register(events, clientLookup)
    }

    @Test
    fun inviteAccepted() {
        val invite = Invite(host, mutableListOf(), GameType("MyGame"), "inv-1")
        events.execute(InviteEvent(host, invite, listOf(invitee)))
        Assertions.assertEquals("""{"type":"Invite","host":"Host","game":"MyGame","inviteId":"inv-1"}""", invitee.nextMessage())
        Assertions.assertEquals("""{"type":"InviteWaiting","inviteId":"inv-1"}""", host.nextMessage())

        expect.event(events to GameStartedEvent::class).condition { true }.after {
            events.execute(InviteResponseEvent(invitee, invite, true))
        }
        Assertions.assertEquals("""{"type":"InviteResponse","user":"Invited","accepted":true,"inviteId":"inv-1"}""", host.nextMessage())
        Assertions.assertEquals("""{"type":"GameStarted","gameType":"MyGame","gameId":"1","yourIndex":0,"players":["Host","Invited"]}""", host.nextMessage())
        Assertions.assertEquals("""{"type":"GameStarted","gameType":"MyGame","gameId":"1","yourIndex":1,"players":["Host","Invited"]}""", invitee.nextMessage())
    }

    @Test
    fun inviteDeclined() {
        val invite = Invite(host, mutableListOf(), GameType("MyGame"), "inv-1")
        events.execute(InviteEvent(host, invite, listOf(invitee)))
        Assertions.assertEquals("""{"type":"Invite","host":"Host","game":"MyGame","inviteId":"inv-1"}""", invitee.nextMessage())
        Assertions.assertEquals("""{"type":"InviteWaiting","inviteId":"inv-1"}""", host.nextMessage())

        events.execute(InviteResponseEvent(invitee, invite, false))
        Assertions.assertEquals("""{"type":"InviteResponse","user":"Invited","accepted":false,"inviteId":"inv-1"}""", host.nextMessage())

    }

}