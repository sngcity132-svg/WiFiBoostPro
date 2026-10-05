package com.wifiboost.pro.presentation

import com.wifiboost.pro.domain.Net
import com.wifiboost.pro.wifi.WifiSnapshot
import kotlin.math.sin

/** DONNÉES SIMULÉES : uniquement utilisées en mode Démo, jamais mélangées au réel. */
object DemoData {
    fun snapshot(t: Int) = WifiSnapshot(true, true, true, "DEMO-Maison", (-62 + 9 * sin(t / 3.0)).toInt(), 433, "5 GHz", 36, "192.168.0.1", "DE:MO:00:00:00:01")
    val nets = listOf(
        Net("DEMO-Maison", "DE:MO:00:00:00:01", -52, "5 GHz", 36, "WPA3"),
        Net("DEMO-Maison", "DE:MO:00:00:00:02", -70, "2.4 GHz", 6, "WPA3"),
        Net("DEMO-Voisin", "DE:MO:00:00:00:03", -71, "2.4 GHz", 6, "WPA2"),
        Net("DEMO-Voisin2", "DE:MO:00:00:00:04", -78, "2.4 GHz", 7, "WPA2"),
        Net("DEMO-Cafe", "DE:MO:00:00:00:05", -85, "2.4 GHz", 11, "Ouvert"),
    )
}
