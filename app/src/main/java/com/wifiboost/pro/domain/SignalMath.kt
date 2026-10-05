package com.wifiboost.pro.domain

import kotlin.math.abs
import kotlin.math.sqrt

enum class Quality(val label: String) {
    EXCELLENT("Excellent"), GOOD("Bon"), FAIR("Moyen"), WEAK("Faible"), VERY_WEAK("Très faible")
}

/** Seuils indicatifs, pas des lois universelles. */
fun classifyRssi(rssi: Int): Quality = when {
    rssi >= -55 -> Quality.EXCELLENT
    rssi >= -65 -> Quality.GOOD
    rssi >= -70 -> Quality.FAIR
    rssi > -80 -> Quality.WEAK
    else -> Quality.VERY_WEAK
}

fun bandOf(mhz: Int): String? = when (mhz) {
    in 2400..2500 -> "2.4 GHz"
    in 4900..5900 -> "5 GHz"
    in 5925..7125 -> "6 GHz"
    else -> null
}

fun channelOf(mhz: Int): Int? = when (mhz) {
    2484 -> 14
    in 2412..2472 -> (mhz - 2407) / 5
    in 5160..5885 -> (mhz - 5000) / 5
    in 5955..7115 -> (mhz - 5950) / 5
    else -> null
}

/** Jitter approximatif = moyenne des écarts absolus entre mesures consécutives. */
fun jitter(samples: List<Double>): Double? =
    if (samples.size < 2) null else samples.zipWithNext { a, b -> abs(b - a) }.average()

fun packetLossPercent(sent: Int, received: Int): Double? =
    if (sent <= 0) null else (sent - received) * 100.0 / sent

data class Stats(val min: Double, val max: Double, val avg: Double, val stdDev: Double)

fun statsOf(v: List<Double>): Stats? {
    if (v.isEmpty()) return null
    val avg = v.average()
    return Stats(v.min(), v.max(), avg, sqrt(v.map { (it - avg) * (it - avg) }.average()))
}
