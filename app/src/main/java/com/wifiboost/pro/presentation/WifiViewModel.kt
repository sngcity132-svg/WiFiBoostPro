package com.wifiboost.pro.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wifiboost.pro.database.AppDb
import com.wifiboost.pro.database.Measurement
import com.wifiboost.pro.diagnostics.Finding
import com.wifiboost.pro.diagnostics.diagnose
import com.wifiboost.pro.domain.*
import com.wifiboost.pro.network.PingTester
import com.wifiboost.pro.testing.*
import com.wifiboost.pro.wifi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class UiState(
    val demo: Boolean = false, val snap: WifiSnapshot? = null, val hasLocation: Boolean = false,
    val rssiHistory: List<Int> = emptyList(), val rssiStats: Stats? = null, val monitoring: Boolean = false,
    val pingBusy: Boolean = false, val pingAvg: Double? = null, val pingJitter: Double? = null,
    val pingLoss: Double? = null, val pingError: String? = null,
    val speedBusy: Boolean = false, val down: Double? = null, val up: Double? = null, val speedError: String? = null,
    val nets: List<Net> = emptyList(), val scanMsg: String? = null, val points: List<HeatPoint> = emptyList(),
    val findings: List<Finding> = emptyList(), val saveSsid: Boolean = false,
    val pingTarget: String = "1.1.1.1", val downUrl: String = DEFAULT_DOWN_URL, val upUrl: String = DEFAULT_UP_URL,
    val message: String? = null,
)

class WifiViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WifiRepository(app)
    private val scanRepo = ScanRepository(app)
    private val dao = AppDb.get(app).dao()
    private val _s = MutableStateFlow(UiState())
    val state = _s.asStateFlow()
    val history = dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private var job: Job? = null

    fun set(f: (UiState) -> UiState) = _s.update(f)
    private fun fin(s: UiState) = s.copy(findings = diagnose(s.snap?.rssi, s.pingAvg, s.rssiStats?.stdDev, s.down))

    fun refresh(append: Boolean = false) {
        val snap = if (_s.value.demo) DemoData.snapshot(_s.value.rssiHistory.size) else repo.snapshot()
        _s.update { st ->
            val h = if (append && snap.rssi != null) (st.rssiHistory + snap.rssi).takeLast(120) else st.rssiHistory
            fin(st.copy(snap = snap, hasLocation = repo.hasLocation(), rssiHistory = h, rssiStats = statsOf(h.map { it.toDouble() })))
        }
    }

    fun toggleMonitoring() {
        if (job?.isActive == true) { job?.cancel(); _s.update { it.copy(monitoring = false) }; return }
        _s.update { it.copy(monitoring = true) }
        // Lecture périodique du RSSI courant, sans scan Wi-Fi permanent ; ping léger toutes les ~20 s.
        job = viewModelScope.launch {
            var i = 0
            while (true) {
                refresh(true)
                if (i++ % 10 == 0) launch { pingNow(_s.value.snap?.gatewayIp ?: _s.value.pingTarget, 3) }
                delay(2000)
            }
        }
    }

    private suspend fun pingNow(host: String, count: Int = 8) {
        if (_s.value.pingBusy) return
        _s.update { it.copy(pingBusy = true, pingError = null) }
        if (_s.value.demo) {
            delay(800); _s.update { fin(it.copy(pingBusy = false, pingAvg = 24.0, pingJitter = 4.0, pingLoss = 0.0)) }; return
        }
        val r = PingTester().run(host.trim(), count = count)
        _s.update {
            fin(it.copy(pingBusy = false, pingAvg = r.rttMs.takeIf { x -> x.isNotEmpty() }?.average(),
                pingJitter = jitter(r.rttMs), pingLoss = packetLossPercent(r.sent, r.rttMs.size),
                pingError = if (r.rttMs.isEmpty()) "Aucune réponse de $host (injoignable ou port 443 filtré)." else null))
        }
    }

    fun runPing(host: String? = null) = viewModelScope.launch { pingNow(host ?: _s.value.pingTarget) }
    fun runGatewayPing() {
        val gw = _s.value.snap?.gatewayIp
        if (gw == null) _s.update { it.copy(pingError = "Passerelle non identifiable.") } else runPing(gw)
    }

    fun runSpeed() = viewModelScope.launch {
        _s.update { it.copy(speedBusy = true, speedError = null, down = null, up = null) }
        if (_s.value.demo) { delay(1200); _s.update { fin(it.copy(speedBusy = false, down = 85.0, up = 20.0)) }; return@launch }
        val st = _s.value; val t = SpeedTester()
        val d = runCatching { t.download(st.downUrl) }
        val u = runCatching { t.upload(st.upUrl) }
        val err = listOfNotNull(
            d.exceptionOrNull()?.let { "Download : ${it.message ?: it.javaClass.simpleName}" },
            u.exceptionOrNull()?.let { "Upload : ${it.message ?: it.javaClass.simpleName}" }).joinToString("\n")
        _s.update { fin(it.copy(speedBusy = false, down = d.getOrNull(), up = u.getOrNull(), speedError = err.ifEmpty { null })) }
    }

    fun scan() = viewModelScope.launch {
        fun msg(m: String) = _s.update { it.copy(scanMsg = m) }
        if (_s.value.demo) { _s.update { it.copy(nets = DemoData.nets, scanMsg = null) }; return@launch }
        if (!repo.hasLocation()) return@launch msg("Permission de localisation refusée : Android l'exige pour scanner.")
        if (!scanRepo.wifiOn()) return@launch msg("Wi-Fi désactivé.")
        if (!scanRepo.locationOn()) return@launch msg("Activez la localisation de l'appareil (exigé par Android pour scanner).")
        val started = scanRepo.start()
        delay(3000)
        val n = scanRepo.results()
        _s.update {
            it.copy(nets = n, scanMsg = when {
                n.isEmpty() -> "Aucun réseau retourné (scan limité par Android ou permission manquante)."
                !started -> "Scan limité par Android (trop fréquent) : derniers résultats connus."
                else -> null
            })
        }
    }

    fun addPoint(label: String, x: Float, y: Float) {
        refresh()
        val r = _s.value.snap?.rssi
        if (r == null) { _s.update { it.copy(message = "RSSI indisponible : point non enregistré.") }; return }
        _s.update { it.copy(points = it.points + HeatPoint(label.ifBlank { "Point" }, x, y, r, System.currentTimeMillis()), message = null) }
    }

    fun save() = viewModelScope.launch {
        val s = _s.value
        if (s.demo) { set { it.copy(message = "Enregistrement désactivé en mode démo.") }; return@launch }
        dao.insert(Measurement(ts = System.currentTimeMillis(), ssid = if (s.saveSsid) s.snap?.ssid else null,
            rssi = s.snap?.rssi, ping = s.pingAvg, jitter = s.pingJitter, loss = s.pingLoss, down = s.down, up = s.up,
            band = s.snap?.band, quality = s.snap?.rssi?.let { classifyRssi(it).label }))
        set { it.copy(message = "Mesure enregistrée.") }
    }
    fun delete(id: Long) = viewModelScope.launch { dao.delete(id) }
    fun clearHistory() = viewModelScope.launch { dao.clear() }

    fun optimize() = viewModelScope.launch {
        refresh(); scan().join(); pingNow(_s.value.snap?.gatewayIp ?: _s.value.pingTarget)
    }

    fun setDemo(on: Boolean) {
        job?.cancel()
        val o = _s.value
        _s.value = UiState(demo = on, saveSsid = o.saveSsid, pingTarget = o.pingTarget, downUrl = o.downUrl, upUrl = o.upUrl)
        refresh()
    }

    fun report(): String {
        val s = _s.value; val sn = s.snap
        fun f(v: Double?, u: String) = v?.let { String.format("%.0f %s", it, u) } ?: "n/d"
        return buildString {
            appendLine("Rapport WiFiBoost Pro" + if (s.demo) " (MODE DÉMONSTRATION — données simulées)" else "")
            appendLine("Qualité : ${sn?.rssi?.let { classifyRssi(it).label } ?: "n/d"}")
            appendLine("RSSI : ${sn?.rssi?.let { "$it dBm" } ?: "n/d"}  Bande : ${sn?.band ?: "n/d"}  Canal : ${sn?.channel ?: "n/d"}")
            appendLine("Ping : ${f(s.pingAvg, "ms")}  Jitter : ${f(s.pingJitter, "ms")}  Perte : ${f(s.pingLoss, "%")}")
            appendLine("Download : ${f(s.down, "Mbps")}  Upload : ${f(s.up, "Mbps")}")
            appendLine("Problèmes détectés :")
            if (s.findings.isEmpty()) appendLine("- aucun d'après les mesures disponibles")
            s.findings.forEach { fd -> appendLine("- ${fd.title}"); fd.advice.forEach { appendLine("    • $it") } }
        }
    }
}
