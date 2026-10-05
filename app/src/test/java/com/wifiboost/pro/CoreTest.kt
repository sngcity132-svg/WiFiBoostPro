package com.wifiboost.pro

import com.wifiboost.pro.diagnostics.diagnose
import com.wifiboost.pro.domain.*
import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    @Test fun rssiClasses() {
        assertEquals(Quality.EXCELLENT, classifyRssi(-50)); assertEquals(Quality.GOOD, classifyRssi(-61))
        assertEquals(Quality.WEAK, classifyRssi(-75)); assertEquals(Quality.VERY_WEAK, classifyRssi(-82))
    }
    @Test fun bandChannel() {
        assertEquals("2.4 GHz", bandOf(2437)); assertEquals(6, channelOf(2437))
        assertEquals("5 GHz", bandOf(5180)); assertEquals(36, channelOf(5180))
    }
    @Test fun jitterAndLoss() {
        assertEquals(10.0, jitter(listOf(40.0, 50.0, 40.0))!!, 1e-9); assertNull(jitter(listOf(1.0)))
        assertEquals(25.0, packetLossPercent(8, 6)!!, 1e-9); assertNull(packetLossPercent(0, 0))
    }
    @Test fun diagnostics() {
        assertEquals("Signal très faible.", diagnose(-82, null, null).first().title)
        assertTrue(diagnose(-50, 250.0, 1.0).first().title.contains("latence"))
        assertTrue(diagnose(-60, null, 9.0).any { it.title.contains("instable") })
        assertTrue(diagnose(null, null, null).isEmpty())
    }
}
