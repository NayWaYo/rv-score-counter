package com.rvetiveau.rvscorecount.ui.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {

    private val json = Json

    @Test
    fun `Party route round-trips and preserves the partyId`() {
        val route: Routes = Routes.Party(partyId = "9c6f1e3e-1111-4b2a-9c1a-000000000001")

        val encoded = json.encodeToString(Routes.serializer(), route)
        val decoded = json.decodeFromString(Routes.serializer(), encoded)

        assertTrue(decoded is Routes.Party)
        assertEquals(route, decoded)
        assertEquals("9c6f1e3e-1111-4b2a-9c1a-000000000001", (decoded as Routes.Party).partyId)
    }

    @Test
    fun `Home and History routes round-trip as singletons`() {
        val homeEncoded = json.encodeToString(Routes.serializer(), Routes.Home)
        val historyEncoded = json.encodeToString(Routes.serializer(), Routes.History)

        assertEquals(Routes.Home, json.decodeFromString(Routes.serializer(), homeEncoded))
        assertEquals(Routes.History, json.decodeFromString(Routes.serializer(), historyEncoded))
    }

    @Test
    fun `each argument route preserves its identifier through round-trip`() {
        val cases: List<Routes> = listOf(
            Routes.Setup(gameId = "papayoo"),
            Routes.RoundEntry(partyId = "party-42"),
            Routes.RoundCorrection(partyId = "party-42", roundNumber = 3),
            Routes.Results(partyId = "party-42"),
            Routes.PartyDetail(partyId = "party-42")
        )

        cases.forEach { route ->
            val decoded = json.decodeFromString(Routes.serializer(), json.encodeToString(Routes.serializer(), route))
            assertEquals(route, decoded)
        }
    }
}
