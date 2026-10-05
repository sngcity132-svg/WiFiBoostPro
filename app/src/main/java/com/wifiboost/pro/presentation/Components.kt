package com.wifiboost.pro.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.wifiboost.pro.domain.Quality

const val NA = "Information non disponible"
fun fmt(v: Double?, unit: String, d: Int = 0): String = if (v == null) NA else String.format("%.${d}f %s", v, unit)

fun qColor(q: Quality) = when (q) {
    Quality.EXCELLENT, Quality.GOOD -> Color(0xFF2ECC71)
    Quality.FAIR -> Color(0xFFF1C40F)
    Quality.WEAK -> Color(0xFFE67E22)
    Quality.VERY_WEAK -> Color(0xFFE74C3C)
}

@Composable
fun Screen(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
fun Section(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            content()
        }
    }
}

@Composable
fun RssiChart(values: List<Int>) {
    val color = MaterialTheme.colorScheme.primary
    if (values.size < 2) { Text("Pas assez de mesures (lancez la surveillance).", style = MaterialTheme.typography.bodySmall); return }
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height * (-30f - v.coerceIn(-95, -30)) / 65f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 4f))
    }
    Text("Échelle : -30 dBm (haut) à -95 dBm (bas)", style = MaterialTheme.typography.bodySmall)
}
