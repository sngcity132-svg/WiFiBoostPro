package com.wifiboost.pro.diagnostics

import com.wifiboost.pro.domain.Quality
import com.wifiboost.pro.domain.classifyRssi

data class Finding(val title: String, val advice: List<String>)

/** Les entrées null (mesure absente) ne produisent jamais de conclusion inventée. */
fun diagnose(rssi: Int?, pingMs: Double?, rssiStdDev: Double?, downMbps: Double? = null): List<Finding> {
    val out = mutableListOf<Finding>()
    val q = rssi?.let(::classifyRssi)
    if (q == Quality.VERY_WEAK || q == Quality.WEAK) out += Finding(
        if (q == Quality.VERY_WEAK) "Signal très faible." else "Signal faible.",
        listOf("Rapprochez-vous du routeur", "Évitez les obstacles importants",
            "Essayez la bande 2.4 GHz si disponible", "Envisagez un répéteur ou un système mesh"))
    if ((q == Quality.EXCELLENT || q == Quality.GOOD) && pingMs != null && pingMs > 150) out += Finding(
        "Le signal Wi-Fi est bon mais la latence Internet est élevée.",
        listOf("Vérifiez la charge du réseau et les téléchargements actifs",
            "Testez la connexion du routeur", "Comparez avec le ping vers la passerelle locale"))
    if (rssiStdDev != null && rssiStdDev > 6) out += Finding("Le signal semble instable.",
        listOf("Éloignez-vous des sources d'interférences (micro-ondes, etc.)", "Évitez de bouger pendant le test"))
    if ((q == Quality.EXCELLENT || q == Quality.GOOD) && (rssiStdDev ?: 0.0) <= 6 && downMbps != null && downMbps < 5)
        out += Finding("Le problème ne semble pas être uniquement la puissance du signal.",
            listOf("Testez un autre serveur de test", "Redémarrez le routeur", "Contactez votre fournisseur Internet"))
    return out
}
