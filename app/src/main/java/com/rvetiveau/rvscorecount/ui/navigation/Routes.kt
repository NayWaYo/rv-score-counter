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

    @Serializable
    @SerialName("partyDetail")
    data class PartyDetail(val partyId: String) : Routes

    /** Statistiques of a Partie, in progress or finished. */
    @Serializable
    @SerialName("stats")
    data class Stats(val partyId: String) : Routes
}
