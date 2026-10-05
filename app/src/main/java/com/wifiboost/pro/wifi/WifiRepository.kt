package com.wifiboost.pro.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.wifiboost.pro.domain.bandOf
import com.wifiboost.pro.domain.channelOf

/** Chaque champ nul = information indisponible (jamais une valeur inventée). */
data class WifiSnapshot(
    val wifiEnabled: Boolean, val connected: Boolean, val internet: Boolean?,
    val ssid: String?, val rssi: Int?, val linkMbps: Int?, val band: String?, val channel: Int?,
    val gatewayIp: String?, val bssid: String? = null,
)

class WifiRepository(private val ctx: Context) {
    private val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun hasLocation() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun snapshot(): WifiSnapshot {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val onWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        if (!wm.isWifiEnabled || !onWifi) return WifiSnapshot(wm.isWifiEnabled, false, null, null, null, null, null, null, null)
        val info = wm.connectionInfo
        val rssi = info.rssi.takeIf { it > -127 && it < 0 }
        val ssid = if (hasLocation()) info.ssid?.trim('"')?.takeIf { it != "<unknown ssid>" } else null
        val mhz = info.frequency.takeIf { it > 0 }
        val gw = wm.dhcpInfo?.gateway?.takeIf { it != 0 }?.let {
            "${it and 255}.${it shr 8 and 255}.${it shr 16 and 255}.${it shr 24 and 255}" }
        return WifiSnapshot(true, true, caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            ssid, rssi, info.linkSpeed.takeIf { it > 0 }, mhz?.let(::bandOf), mhz?.let(::channelOf), gw,
            if (hasLocation()) info.bssid?.takeIf { it != "02:00:00:00:00:00" } else null)
    }
}
