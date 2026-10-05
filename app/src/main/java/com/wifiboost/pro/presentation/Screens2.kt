package com.wifiboost.pro.presentation

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wifiboost.pro.database.Measurement
import com.wifiboost.pro.domain.*
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun GamingScreen(vm: WifiViewModel, s: UiState) = Screen {
    Button(enabled = !s.pingBusy, onClick = { vm.runPing() }) { Text("🎮 Analyser (ping vers ${s.pingTarget})") }
    if (s.pingBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
    s.pingError?.let { Text(it) }
    Section("Mesures") {
        Text("Ping : ${fmt(s.pingAvg, "ms")}   Jitter : ${fmt(s.pingJitter, "ms")}   Perte : ${fmt(s.pingLoss, "%")}")
        Text("Stabilité du signal (écart-type RSSI) : ${s.rssiStats?.let { "%.1f dB".format(it.stdDev) } ?: "$NA (lancez la surveillance)"}")
    }
    val g = gamingScore(s.pingAvg, s.pingJitter, s.pingLoss)
    Section("Gaming Score") { Text(if (g == null) NA else "$g / 100 — ${gamingLabel(g)}", style = MaterialTheme.typography.headlineSmall) }
    Text("Score calculé à partir des mesures réelles ; aucune réduction de ping n'est promise.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun StreamingScreen(vm: WifiViewModel, s: UiState) = Screen {
    Button(enabled = !s.speedBusy, onClick = { vm.runSpeed() }) { Text("🎬 Analyser (test de débit)") }
    if (s.speedBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
    s.speedError?.let { Text(it) }
    Section("Mesures") {
        Text("Download : ${fmt(s.down, "Mbps", 1)}   Ping : ${fmt(s.pingAvg, "ms")}")
        Text("Stabilité RSSI : ${s.rssiStats?.let { "%.1f dB".format(it.stdDev) } ?: NA}")
    }
    Section("Qualité estimée (indicatif)") { Text(streamingTier(s.down, s.pingLoss)?.label ?: NA, style = MaterialTheme.typography.headlineSmall) }
}

@Composable
fun HeatmapScreen(vm: WifiViewModel, s: UiState) = Screen {
    var label by remember { mutableStateOf("Salon") }
    OutlinedTextField(label, { label = it }, label = { Text("Nom de la pièce / point") }, singleLine = true)
    Text("Touchez le plan à l'endroit où vous vous trouvez : le RSSI réel est enregistré à ce point.")
    Canvas(Modifier.fillMaxWidth().height(320.dp).pointerInput(label) {
        detectTapGestures { o -> vm.addPoint(label, o.x / size.width, o.y / size.height) }
    }) {
        drawRect(Color(0x22FFFFFF))
        s.points.forEach { p ->
            val c = when (classifyRssi(p.rssi)) {
                Quality.EXCELLENT -> Color(0xFF2ECC71); Quality.GOOD -> Color(0xFFF1C40F)
                Quality.FAIR, Quality.WEAK -> Color(0xFFE67E22); Quality.VERY_WEAK -> Color(0xFFE74C3C)
            }
            drawCircle(c.copy(alpha = 0.8f), radius = 36f, center = Offset(p.x * size.width, p.y * size.height))
        }
    }
    Text("🟢 Excellent  🟡 Bon  🟠 Faible  🔴 Zone morte (position relative, pas de GPS)", style = MaterialTheme.typography.bodySmall)
    s.message?.let { Text(it) }
    s.points.forEach { Text("${it.label} : ${it.rssi} dBm (${classifyRssi(it.rssi).label})") }
    if (s.points.isNotEmpty()) OutlinedButton(onClick = { vm.set { it.copy(points = emptyList()) } }) { Text("Effacer les points") }
}

@Composable
fun HistoryScreen(vm: WifiViewModel, items: List<Measurement>) = Screen {
    var filter by remember { mutableStateOf<String?>(null) }
    var sel by remember { mutableStateOf(setOf<Long>()) }
    val fmtD = remember { SimpleDateFormat("dd/MM/yyyy HH:mm") }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(filter == null, { filter = null }, { Text("Tous") })
        Quality.values().forEach { q -> FilterChip(filter == q.label, { filter = q.label }, { Text(q.label) }) }
    }
    val shown = items.filter { filter == null || it.quality == filter }
    if (shown.isEmpty()) Text("Aucune mesure enregistrée.")
    val two = items.filter { it.id in sel }
    if (two.size == 2) Section("Comparaison") {
        @Composable fun row(n: String, f: (Measurement) -> Double?) = Text("$n : ${fmt(f(two[0]), "", 1)} → ${fmt(f(two[1]), "", 1)}")
        Text("${fmtD.format(Date(two[0].ts))}  →  ${fmtD.format(Date(two[1].ts))}")
        row("RSSI (dBm)") { it.rssi?.toDouble() }; row("Ping (ms)") { it.ping }; row("Jitter (ms)") { it.jitter }
        row("Perte (%)") { it.loss }; row("Download (Mbps)") { it.down }; row("Upload (Mbps)") { it.up }
    }
    shown.forEach { m ->
        Section(fmtD.format(Date(m.ts)) + (m.ssid?.let { " · $it" } ?: "")) {
            Text("RSSI ${m.rssi ?: "n/d"} dBm · ${m.band ?: "n/d"} · ${m.quality ?: "n/d"}")
            Text("Ping ${fmt(m.ping, "ms")} · Jitter ${fmt(m.jitter, "ms")} · Perte ${fmt(m.loss, "%")}")
            Text("Down ${fmt(m.down, "Mbps", 1)} · Up ${fmt(m.up, "Mbps", 1)}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(m.id in sel, { sel = if (m.id in sel) sel - m.id else (sel.toList() + m.id).takeLast(2).toSet() }, { Text("Comparer") })
                TextButton(onClick = { vm.delete(m.id) }) { Text("Supprimer") }
            }
        }
    }
}

@Composable
fun DiagnosticScreen(vm: WifiViewModel, s: UiState) = Screen {
    val ctx = LocalContext.current
    Button(onClick = { vm.optimize() }) { Text("Optimiser ma connexion") }
    Text("N'augmente pas la puissance de l'antenne : analyse, détecte et vous guide.", style = MaterialTheme.typography.bodySmall)
    if (s.pingBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
    val rssi = s.snap?.rssi
    if (rssi != null) Section { Text("Votre signal : $rssi dBm (${classifyRssi(rssi).label}).") }
    if (s.findings.isEmpty()) Text("Aucun problème détecté d'après les mesures disponibles (lancez surveillance, ping, débit).")
    s.findings.forEach { f -> Section(f.title) { f.advice.forEach { Text("• $it") } } }
    val better = betterOption(s.snap?.ssid, rssi, s.nets)
    if (better != null) Section("Meilleure option détectée") {
        Text("Votre réseau est aussi reçu en ${better.band ?: "?"} (canal ${better.channel ?: "?"}) à ${better.rssi} dBm.")
        Text("Android ne permet pas à cette application de changer automatiquement de réseau dans cette situation.")
        Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text("Ouvrir les paramètres Wi-Fi") }
    }
    OutlinedButton(onClick = {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, vm.report())
        ctx.startActivity(Intent.createChooser(i, "Partager le rapport").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }) { Text("Générer un rapport (partager)") }
}

@Composable
fun SettingsScreen(vm: WifiViewModel, s: UiState) = Screen {
    Section("Mode Démo") {
        Row { Switch(s.demo, { vm.setDemo(it) }); Spacer(Modifier.width(8.dp)); Text("Données simulées (interface sans Wi-Fi réel)") }
    }
    Section("Historique") {
        Row { Switch(s.saveSsid, { v -> vm.set { it.copy(saveSsid = v) } }); Spacer(Modifier.width(8.dp)); Text("Enregistrer le nom du réseau (sinon anonymisé)") }
        OutlinedButton(onClick = { vm.clearHistory() }) { Text("Supprimer tout l'historique") }
    }
    Section("Confidentialité") {
        Text("Données collectées : signal, bande, canal, latence et débit mesurés, et (si vous l'autorisez) le nom du réseau. Tout reste sur l'appareil. Seuls les tests de ping/débit contactent les serveurs que vous configurez. L'app ne lit aucun mot de passe, n'intercepte aucun trafic et ne scanne pas agressivement. La localisation n'est demandée que parce qu'Android l'exige pour lire le SSID et scanner.")
    }
}
