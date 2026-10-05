package com.wifiboost.pro.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class PingResult(val rttMs: List<Double>, val sent: Int)

/** Latence TCP (connexion) : ICMP n'est pas fiable sur Android et souvent bloqué. */
class PingTester {
    suspend fun run(host: String, port: Int = 443, count: Int = 8, timeoutMs: Int = 2000): PingResult =
        withContext(Dispatchers.IO) {
            val rtts = mutableListOf<Double>()
            repeat(count) {
                try {
                    Socket().use { s ->
                        val t0 = System.nanoTime()
                        s.connect(InetSocketAddress(host, port), timeoutMs)
                        rtts += (System.nanoTime() - t0) / 1e6
                    }
                } catch (_: Exception) { /* paquet compté comme perdu */ }
            }
            PingResult(rtts, count)
        }
}
