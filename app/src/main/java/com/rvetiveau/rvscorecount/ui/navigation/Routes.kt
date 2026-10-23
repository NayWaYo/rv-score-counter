package com.rvetiveau.rvscorecount.ui.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations (AD-6). Identifiers are passed as [String]
 * (UUID.toString()) so the domain model never leaks into the navigation layer.
 */
@Serializable
sealed interface Routes {
    @Serializable
    @SerialName("home")
    data object Home : Routes

    @Serializable
    @SerialName("setup")
    data class Setup(val gameId: String) : Routes

    @Serializable
    @SerialName("party")
    data class Party(val partyId: String) : Routes

    @Serializable
    @SerialName("roundEntry")
    data class RoundEntry(val partyId: String) : Routes

    /** Saisie de Manche reopened to correct the validated Manche [roundNumber] of a Partie en cours. */
    @Serializable
    @SerialName("roundCorrection")
    data class RoundCorrection(val partyId: String, val roundNumber: Int) : Routes

    @Serializable
    @SerialName("results")
    data class Results(val partyId: String) : Routes

    @Serializable
    @SerialName("history")
    data object History : Routes

    /** Historique of one Jeu, opened from its creation de Partie (Story 8.2). */
    @Serializable
    @SerialName("gameHistory")
    data class GameHistory(val gameId: String) : Routes

    /** The Joueurs of every Jeu, from the Accueil (Story 10.2). */
    @Serializable
    @SerialName("players")
    data object Players : Routes

    /** Export and restore of the whole database, from the Accueil (Epic 12). */
    @Serializable
    @SerialName("backup")
    data object Backup : Routes

    /** A Joueur's profile and records (Story 10.3). */
    @Serializable
    @SerialName("playerProfile")
    data class PlayerProfile(val playerId: String) : Routes

    @Serializable
    @SerialName("partyDetail")
    data class PartyDetail(val partyId: String) : Routes

    // Belote (Story 7.3) : its own screens, the Papayoo ones being per player (AD-8).

    @Serializable
    @SerialName("beloteSetup")
    data object BeloteSetup : Routes

    @Serializable
    @SerialName("beloteParty")
    data class BeloteParty(val partyId: String) : Routes

    /** Saisie of the next Manche, or correction of the validated Manche [correctedRoundNumber]. */
    @Serializable
    @SerialName("beloteRound")
    data class BeloteRound(val partyId: String, val correctedRoundNumber: Int? = null) : Routes

    @Serializable
    @SerialName("beloteResults")
    data class BeloteResults(val partyId: String) : Routes

    @Serializable
    @SerialName("belotePartyDetail")
    data class BelotePartyDetail(val partyId: String) : Routes

    // Jeu libre (Story 11.2, AD-9).

    @Serializable
    @SerialName("freeSetup")
    data object FreeSetup : Routes

    /** A Partie of Jeu libre, in progress or finished (read only). */
    @Serializable
    @SerialName("freeParty")
    data class FreeParty(val partyId: String) : Routes

    /** Saisie of the next Manche of Jeu libre, or correction of the validated Manche [correctedRoundNumber] (Story 11.3). */
    @Serializable
    @SerialName("freeRound")
    data class FreeRound(val partyId: String, val correctedRoundNumber: Int? = null) : Routes

    /** Résultats of a Partie of Jeu libre just ended, with « Rejouer » (Story 11.4). */
    @Serializable
    @SerialName("freeResults")
    data class FreeResults(val partyId: String) : Routes

    // Tarot (Story 13.3, AD-10).

    @Serializable
    @SerialName("tarotSetup")
    data object TarotSetup : Routes

    /** A Partie of Tarot, in progress or finished (read only). */
    @Serializable
    @SerialName("tarotParty")
    data class TarotParty(val partyId: String) : Routes

    /** Saisie of the next Manche of Tarot, or correction of the validated Manche [correctedRoundNumber]. */
    @Serializable
    @SerialName("tarotRound")
    data class TarotRound(val partyId: String, val correctedRoundNumber: Int? = null) : Routes

    /** Statistiques of a Partie of Tarot, in progress or finished (Story 13.4). */
    @Serializable
    @SerialName("tarotStats")
    data class TarotStats(val partyId: String) : Routes

    /** Résultats of a Partie of Tarot just ended, with « Rejouer ». */
    @Serializable
    @SerialName("tarotResults")
    data class TarotResults(val partyId: String) : Routes

    /** Statistiques of a Partie of Belote, in progress or finished. */
    @Serializable
    @SerialName("beloteStats")
    data class BeloteStats(val partyId: String) : Routes

    /** Statistiques of a Partie, in progress or finished. */
    @Serializable
    @SerialName("stats")
    data class Stats(val partyId: String) : Routes
}
