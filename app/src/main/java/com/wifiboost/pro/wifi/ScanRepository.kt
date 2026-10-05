package com.wifiboost.pro.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
import androidx.core.location.LocationManagerCompat
import com.wifiboost.pro.domain.*

class ScanRepository(private val ctx: Context) {
    private val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    fun wifiOn() = wm.isWifiEnabled
    fun locationOn() = LocationManagerCompat.isLocationEnabled(ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager)

    /** false = scan refusé/limité par Android (throttling). */
    @Suppress("DEPRECATION")
    fun start(): Boolean = try { wm.startScan() } catch (e: SecurityException) { false }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun results(): List<Net> = try {
        wm.scanResults.map { r ->
            Net(r.SSID.ifEmpty { "(réseau masqué)" }, r.BSSID, r.level, bandOf(r.frequency), channelOf(r.frequency), securityOf(r.capabilities))
        }.sortedByDescending { it.rssi }
    } catch (e: SecurityException) { emptyList() }
}
