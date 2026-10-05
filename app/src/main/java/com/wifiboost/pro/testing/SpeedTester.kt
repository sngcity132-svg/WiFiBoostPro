package com.wifiboost.pro.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

const val DEFAULT_DOWN_URL = "https://speed.cloudflare.com/__down?bytes=25000000"
const val DEFAULT_UP_URL = "https://speed.cloudflare.com/__up"

/** Mesures réelles vers un endpoint configurable. Résultat en Mbps. */
class SpeedTester {
    suspend fun download(url: String, maxMs: Long = 8000): Double = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 5000; c.readTimeout = 5000
        val buf = ByteArray(64 * 1024); var total = 0L; val t0 = System.nanoTime()
        try {
            c.inputStream.use { i ->
                while (true) {
                    val n = i.read(buf); if (n < 0) break
                    total += n
                    if ((System.nanoTime() - t0) / 1e6 > maxMs) break
                }
            }
        } finally { c.disconnect() }
        val s = (System.nanoTime() - t0) / 1e9
        if (total == 0L) throw IOException("Aucune donnée reçue")
        total * 8 / s / 1e6
    }

    suspend fun upload(url: String, bytes: Int = 4_000_000): Double = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.setFixedLengthStreamingMode(bytes)
        c.connectTimeout = 5000; c.readTimeout = 10000
        val chunk = ByteArray(64 * 1024); var sent = 0; val t0 = System.nanoTime()
        try {
            c.outputStream.use { o -> while (sent < bytes) { val n = minOf(chunk.size, bytes - sent); o.write(chunk, 0, n); sent += n } }
            c.responseCode
        } finally { c.disconnect() }
        bytes * 8 / ((System.nanoTime() - t0) / 1e9) / 1e6
    }
}
