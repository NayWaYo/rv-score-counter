package com.rvetiveau.rvscorecount.ui.theme

import androidx.compose.ui.graphics.Color
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CardSuitColorsTest {

    @Test
    fun `dark theme maps each suit to its colour and tint`() {
        assertEquals(Color(0xFFD5DCE2), DarkRvColors.suitColor(CardSuit.SPADE))
        assertEquals(Color(0xFF1C2024), DarkRvColors.suitTint(CardSuit.SPADE))
        assertEquals(Color(0xFFEF5350), DarkRvColors.suitColor(CardSuit.HEART))
        assertEquals(Color(0xFF2D1517), DarkRvColors.suitTint(CardSuit.HEART))
        assertEquals(Color(0xFF66BB6A), DarkRvColors.suitColor(CardSuit.CLUB))
        assertEquals(Color(0xFF182A1A), DarkRvColors.suitTint(CardSuit.CLUB))
        assertEquals(Color(0xFF42A5F5), DarkRvColors.suitColor(CardSuit.DIAMOND))
        assertEquals(Color(0xFF0E2440), DarkRvColors.suitTint(CardSuit.DIAMOND))
    }

    @Test
    fun `light theme maps each suit to its colour and tint`() {
        assertEquals(Color(0xFF37474F), LightRvColors.suitColor(CardSuit.SPADE))
        assertEquals(Color(0xFFDADDE0), LightRvColors.suitTint(CardSuit.SPADE))
        assertEquals(Color(0xFFC62828), LightRvColors.suitColor(CardSuit.HEART))
        assertEquals(Color(0xFFF8D9D9), LightRvColors.suitTint(CardSuit.HEART))
        assertEquals(Color(0xFF2E7D32), LightRvColors.suitColor(CardSuit.CLUB))
        assertEquals(Color(0xFFD8EED9), LightRvColors.suitTint(CardSuit.CLUB))
        assertEquals(Color(0xFF1565C0), LightRvColors.suitColor(CardSuit.DIAMOND))
        assertEquals(Color(0xFFC2D9FA), LightRvColors.suitTint(CardSuit.DIAMOND))
    }

    @Test
    fun `both themes give eight chart series and every suit its own tint`() {
        listOf(DarkRvColors, LightRvColors).forEach { colors ->
            assertEquals(8, colors.chartSeries.distinct().size)
            assertEquals(4, CardSuit.entries.map { colors.suitTint(it) }.distinct().size)
        }
        assertNotEquals(DarkRvColors, LightRvColors)
    }
}
