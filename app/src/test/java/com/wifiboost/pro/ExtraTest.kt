package com.wifiboost.pro

import com.wifiboost.pro.domain.*
import org.junit.Assert.*
import org.junit.Test

class ExtraTest {
    @Test fun gaming() {
        assertEquals(100, gamingScore(20.0, 0.0, 0.0)); assertTrue(gamingScore(200.0, 30.0, 5.0)!! < 40)
        assertNull(gamingScore(null, null, null)); assertEquals("Excellent", gamingLabel(90))
    }
    @Test fun streaming() {
        assertEquals(Tier.K4, streamingTier(100.0, 0.0)); assertEquals(Tier.P480, streamingTier(3.5, 0.0)); assertNull(streamingTier(null, 0.0))
    }
    @Test fun security() { assertEquals("WPA3", securityOf("[WPA2-PSK][SAE]")); assertEquals("Ouvert", securityOf("[ESS]")) }
    @Test fun channels() {
        val n = listOf(Net("a", null, -50, "2.4 GHz", 1, "WPA2"), Net("b", null, -60, "2.4 GHz", 3, "WPA2"), Net("c", null, -70, "2.4 GHz", 11, "WPA2"))
        val c = congestion(n, "2.4 GHz"); assertEquals(2, c[1]); assertEquals(1, c[11])
    }
    @Test fun stats() {
        val s = statsOf(listOf(-60.0, -70.0))!!; assertEquals(-65.0, s.avg, 1e-9); assertEquals(-70.0, s.min, 1e-9); assertNull(statsOf(emptyList()))
    }
    @Test fun better() {
        val n = listOf(Net("X", null, -50, "5 GHz", 36, "WPA2")); assertNotNull(betterOption("X", -70, n)); assertNull(betterOption("X", -52, n))
    }
}
