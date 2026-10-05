package com.wifiboost.pro.presentation

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface { App() } } }
    }
}

private val TABS = listOf("🏠 Dashboard", "📶 Wi-Fi", "📊 Monitoring", "⚡ Tests", "🎮 Gaming", "🎬 Streaming",
    "🗺️ Heatmap", "📈 Historique", "🧠 Diagnostic", "⚙️ Paramètres")

@Composable
fun App(vm: WifiViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val hist by vm.history.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }
    LaunchedEffect(Unit) { vm.refresh() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        if (s.demo) Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
            Text("MODE DÉMONSTRATION — données simulées", Modifier.padding(8.dp))
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
            TABS.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> DashboardScreen(vm, s) { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
            1 -> WifiScreen(vm, s)
            2 -> MonitoringScreen(vm, s)
            3 -> TestsScreen(vm, s)
            4 -> GamingScreen(vm, s)
            5 -> StreamingScreen(vm, s)
            6 -> HeatmapScreen(vm, s)
            7 -> HistoryScreen(vm, hist)
            8 -> DiagnosticScreen(vm, s)
            else -> SettingsScreen(vm, s)
        }
    }
}
