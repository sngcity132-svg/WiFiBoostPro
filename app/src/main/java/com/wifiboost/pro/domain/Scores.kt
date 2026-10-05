package com.wifiboost.pro.domain

import kotlin.math.abs
import kotlin.math.max

/** Score calculé uniquement à partir de mesures réelles ; null si mesures absentes. */
fun gamingScore(pingMs: Double?, jitterMs: Double?, lossPct: Double?): Int? {
    if (pingMs == null || lossPct == null) return null
    val s = 100 - max(0.0, pingMs - 20) * 0.5 - (jitterMs ?: 0.0) * 1.5 - lossPct * 8
    return s.coerceIn(0.0, 100.0).toInt()
}

fun gamingLabel(s: Int) = when { s >= 85 -> "Excellent"; s >= 65 -> "Bon"; s >= 40 -> "Moyen"; else -> "Mauvais" }

enum class Tier(val label: String, val minMbps: Double) {
    P480("480p", 3.0), P720("720p", 5.0), P1080("1080p", 8.0), P1440("1440p", 16.0), K4("4K", 25.0)
}

/** Estimation indicative. */
fun streamingTier(downMbps: Double?, lossPct: Double?): Tier? {
    if (downMbps == null) return null
    val eff = if ((lossPct ?: 0.0) > 2) downMbps * 0.7 else downMbps
    return Tier.values().lastOrNull { eff >= it.minMbps }
}

data class Net(val ssid: String, val bssid: String?, val rssi: Int, val band: String?, val channel: Int?, val security: String)
data class HeatPoint(val label: String, val x: Float, val y: Float, val rssi: Int, val ts: Long)

fun securityOf(caps: String): String = when {
    caps.contains("SAE") || caps.contains("WPA3") -> "WPA3"
    caps.contains("WPA2") -> "WPA2"
    caps.contains("WPA") -> "WPA"
    caps.contains("WEP") -> "WEP"
    else -> "Ouvert"
}

/** Nombre de réseaux détectés qui chevauchent chaque canal (2.4 GHz : ±4 canaux). */
fun congestion(nets: List<Net>, band: String): Map<Int, Int> {
    val inBand = nets.filter { it.band == band && it.channel != null }
    return inBand.mapNotNull { it.channel }.distinct().sorted().associateWith { ch ->
        inBand.count { n -> if (band == "2.4 GHz") abs(n.channel!! - ch) <= 4 else n.channel == ch }
    }
}

fun betterOption(ssid: String?, rssi: Int?, nets: List<Net>): Net? {
    if (ssid == null || rssi == null) return null
    return nets.filter { it.ssid == ssid && it.rssi >= rssi + 8 }.maxByOrNull { it.rssi }
}
