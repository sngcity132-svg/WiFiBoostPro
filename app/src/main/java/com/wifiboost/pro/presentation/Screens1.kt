package com.wifiboost.pro.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wifiboost.pro.domain.*

@Composable
fun DashboardScreen(vm: WifiViewModel, s: UiState, requestLoc: () -> Unit) = Screen {
    val snap = s.snap
    val rssi = snap?.rssi
    Section("Connexion actuelle") {
        when {
            snap == null -> CircularProgressIndicator()
            !snap.wifiEnabled -> Text("Wi-Fi désactivé. Activez-le pour mesurer.")
            !snap.connected -> Text("Non connecté à un réseau Wi-Fi.")
            else -> {
                Text("SSID : ${snap.ssid ?: if (s.hasLocation) NA else "$NA (localisation requise)"}")
                Text("BSSID : ${snap.bssid ?: NA}")
                Text("Bande : ${snap.band ?: NA}   Canal : ${snap.channel ?: NA}")
                Text("RSSI : ${rssi?.let { "$it dBm" } ?: NA}")
                Text("Lien Wi-Fi : ${snap.linkMbps?.let { "$it Mbps" } ?: NA}")
                Text("Internet : " + when (snap.internet) { true -> "Connecté"; false -> "Pas d'accès Internet"; null -> NA })
            }
        }
    }
    if (rssi != null) {
        val q = classifyRssi(rssi)
        Section("Qualité (d'après le RSSI réel)") {
            Text("● ${q.label.uppercase()}", color = qColor(q), style = MaterialTheme.typography.titleLarge)
            LinearProgressIndicator(progress = { ((rssi + 95) / 65f).coerceIn(0f, 1f) }, color = qColor(q), modifier = Modifier.fillMaxWidth())
            Text("Seuils indicatifs, pas des lois universelles.", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (!s.hasLocation && !s.demo) OutlinedButton(onClick = requestLoc) { Text("Autoriser la localisation (SSID, BSSID, scan)") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { vm.refresh(true) }) { Text("Actualiser") }
        OutlinedButton(onClick = { vm.save() }) { Text("Enregistrer") }
    }
    s.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}

@Composable
fun MonitoringScreen(vm: WifiViewModel, s: UiState) = Screen {
    Button(onClick = vm::toggleMonitoring) { Text(if (s.monitoring) "STOP" else "START — Surveiller ma connexion") }
    Section("RSSI en temps réel") { RssiChart(s.rssiHistory) }
    s.rssiStats?.let {
        Section("Statistiques RSSI") {
            Text("Actuel : ${s.snap?.rssi ?: NA} dBm")
            Text("Min ${it.min.toInt()} / Max ${it.max.toInt()} / Moyen ${"%.1f".format(it.avg)} dBm")
            Text("Mesures : ${s.rssiHistory.size}")
        }
    }
    Section("Latence (dernière mesure)") {
        Text("Ping : ${fmt(s.pingAvg, "ms")}   Jitter : ${fmt(s.pingJitter, "ms")}   Perte : ${fmt(s.pingLoss, "%")}")
        if (s.pingBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    Text("Le RSSI est lu toutes les 2 s tant que la surveillance est active ; aucun scan permanent.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun WifiScreen(vm: WifiViewModel, s: UiState) = Screen {
    Button(onClick = { vm.scan() }) { Text("Scanner les réseaux") }
    s.scanMsg?.let { Section { Text(it) } }
    if (s.nets.isEmpty() && s.scanMsg == null) Text("Aucune donnée : lancez un scan.")
    s.nets.forEach { n ->
        val q = classifyRssi(n.rssi)
        Section(n.ssid) {
            Text("${n.rssi} dBm · ${n.band ?: NA} · canal ${n.channel ?: NA} · ${n.security}")
            Text("BSSID ${n.bssid ?: NA}", style = MaterialTheme.typography.bodySmall)
            Text("Qualité estimée : ${q.label}", color = qColor(q))
        }
    }
    if (s.nets.isNotEmpty()) {
        Section("Interférences potentielles") {
            Text("Réseaux détectés : ${s.nets.size}")
            Text("Voisin le plus puissant : ${s.nets.maxOf { it.rssi }} dBm")
            listOf("2.4 GHz", "5 GHz", "6 GHz").forEach { band ->
                val c = congestion(s.nets, band)
                if (c.isNotEmpty()) {
                    Text(band, style = MaterialTheme.typography.titleSmall)
                    val max = c.values.max().toFloat()
                    c.forEach { (ch, load) ->
                        Text("Canal $ch : $load réseau(x)")
                        LinearProgressIndicator(progress = { load / max }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            val cur = s.snap
            val load = if (cur?.band != null && cur.channel != null) congestion(s.nets, cur.band)[cur.channel] else null
            if (load != null && load >= 3) Text("Votre routeur utilise probablement un canal chargé. Vérifiez la configuration du routeur.")
            Text("Estimation à partir des réseaux détectés ; Android ne mesure pas directement toutes les interférences radio.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TestsScreen(vm: WifiViewModel, s: UiState) = Screen {
    Section("Test Ping (TCP)") {
        OutlinedTextField(s.pingTarget, { v -> vm.set { it.copy(pingTarget = v) } }, label = { Text("Cible (hôte)") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !s.pingBusy, onClick = { vm.runPing() }) { Text("Cible") }
            OutlinedButton(enabled = !s.pingBusy, onClick = { vm.runGatewayPing() }) { Text("Passerelle") }
        }
        if (s.pingBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        s.pingError?.let { Text(it) }
        if (s.pingLoss != null) {
            Text("Ping : ${fmt(s.pingAvg, "ms")}   Jitter : ${fmt(s.pingJitter, "ms")}   Perte : ${fmt(s.pingLoss, "%")}")
        }
        Text("Mesure par connexion TCP : un échec n'implique pas l'absence d'Internet.", style = MaterialTheme.typography.bodySmall)
    }
    Section("Test de débit") {
        OutlinedTextField(s.downUrl, { v -> vm.set { it.copy(downUrl = v) } }, label = { Text("Endpoint download") })
        OutlinedTextField(s.upUrl, { v -> vm.set { it.copy(upUrl = v) } }, label = { Text("Endpoint upload") })
        Button(enabled = !s.speedBusy, onClick = { vm.runSpeed() }) { Text("Lancer") }
        if (s.speedBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        s.speedError?.let { Text(it) }
        Text("Download : ${fmt(s.down, "Mbps", 1)}   Upload : ${fmt(s.up, "Mbps", 1)}   Ping : ${fmt(s.pingAvg, "ms")}")
        Text("Le débit dépend du serveur de test, du routeur, du fournisseur Internet et de la charge du réseau.", style = MaterialTheme.typography.bodySmall)
    }
}
