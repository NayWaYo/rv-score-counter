package com.rvetiveau.rvscorecount.ui.theme

import androidx.compose.ui.graphics.Color
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import org.junit.Assert.assertEquals
import org.junit.Test

class CardSuitColorsTest {

    @Test
    fun `spade maps to expected color and tint`() {
        assertEquals(Color(0xFF90A4AE), CardSuit.SPADE.color)
        assertEquals(Color(0xFF1A2328), CardSuit.SPADE.tint)
    }

    @Test
    fun `heart maps to expected color and tint`() {
        assertEquals(Color(0xFFEF5350), CardSuit.HEART.color)
        assertEquals(Color(0xFF2D1517), CardSuit.HEART.tint)
    }

    @Test
    fun `club maps to expected color and tint`() {
        assertEquals(Color(0xFF66BB6A), CardSuit.CLUB.color)
        assertEquals(Color(0xFF182A1A), CardSuit.CLUB.tint)
    }

    @Test
    fun `diamond maps to expected color and tint`() {
        assertEquals(Color(0xFF42A5F5), CardSuit.DIAMOND.color)
        assertEquals(Color(0xFF16222E), CardSuit.DIAMOND.tint)
    }
}
