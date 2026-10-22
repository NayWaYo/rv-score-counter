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

    /** Statistiques of a Partie of Belote, in progress or finished. */
    @Serializable
    @SerialName("beloteStats")
    data class BeloteStats(val partyId: String) : Routes

    /** Statistiques of a Partie, in progress or finished. */
    @Serializable
    @SerialName("stats")
    data class Stats(val partyId: String) : Routes
}
