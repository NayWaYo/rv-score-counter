package com.rvetiveau.rvscorecount.domain.engine

/**
 * Résultat de validation d'une manche. `difference` = somme réelle - total attendu.
 */
sealed interface RoundValidation {
    data object Success : RoundValidation
    data class Error(val difference: Int) : RoundValidation
}
