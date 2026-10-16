package com.testing.adwarecleaner

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.cos
import kotlin.math.sin

// =====================================================================
//  Estado y acciones que la Activity pasa a la interfaz
// =====================================================================

data class UiState(
    val findings: List<Finding> = emptyList(),
    val scannedCount: Int = 0,
    val history: List<RemovedApp> = emptyList(),
    val hasScanned: Boolean = false,
    val scanning: Boolean = false,
    val lastScan: Long = 0L,
    val usageAccess: Boolean = false,
    val trace: List<TraceEntry> = emptyList(),
    val traceMinutes: Int = 15,
    val traceLoading: Boolean = false,
)

class Actions(
    val scan: () -> Unit,
    val uninstall: (Finding, Source) -> Unit,
    val uninstallAllHigh: () -> Unit,
    val clearHistory: () -> Unit,
    val writePdf: (Uri, String, String) -> Unit,
    val openUsageSettings: () -> Unit,
    val refreshTrace: () -> Unit,
    val setTraceMinutes: (Int) -> Unit,
)

enum class Tab(val label: String, val icon: Int) {
    DETECTAR("Detectar", R.drawable.ic_tab_scan),
    RASTREAR("Rastrear", R.drawable.ic_tab_trace),
    HISTORIAL("Historial", R.drawable.ic_tab_history),
    INFORME("Informe", R.drawable.ic_tab_report),
}

private fun plural(n: Int, one: String, many: String) = if (n == 1) "$n $one" else "$n $many"

// =====================================================================
//  Raíz
// =====================================================================

@Composable
fun AppRoot(state: UiState, actions: Actions) {
    var tab by rememberSaveable { mutableStateOf(Tab.DETECTAR) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopBar() },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = {
                            tab = t
                            if (t == Tab.RASTREAR) actions.refreshTrace()
                        },
                        icon = { Icon(painterResource(t.icon), contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = Accent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.DETECTAR -> DetectScreen(state, actions)
                Tab.RASTREAR -> TraceScreen(state, actions)
                Tab.HISTORIAL -> HistoryScreen(state.history, actions.clearHistory)
                Tab.INFORME -> ReportScreen(state, actions.writePdf)
            }
        }
    }
}

@Composable
private fun TopBar() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_logo), contentDescription = null, Modifier.size(40.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            buildAnnotatedString {
                append("AdWare ")
                withStyle(SpanStyle(color = Accent)) { append("Cleaner") }
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
    }
}

// =====================================================================
//  DETECTAR
// =====================================================================

@Composable
private fun DetectScreen(state: UiState, actions: Actions) {
    val high = state.findings.count { it.risk == Risk.ALTO }
    val medium = state.findings.count { it.risk == Risk.MEDIO }
    var filter by rememberSaveable { mutableStateOf<Risk?>(null) }
    val visible = state.findings.filter { filter == null || it.risk == filter }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Hero(state, high, medium, actions.scan) }

        if (state.hasScanned) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Alto", high, RiskHigh, Modifier.weight(1f))
                    StatTile("Revisar", medium, RiskMedium, Modifier.weight(1f))
                    StatTile("Analizadas", state.scannedCount, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                }
            }

            if (high > 1) {
                item {
                    Button(
                        onClick = actions.uninstallAllHigh,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RiskHigh.copy(alpha = 0.18f), contentColor = RiskHigh),
                    ) {
                        Icon(painterResource(R.drawable.ic_delete), contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Eliminar las $high de riesgo alto", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (state.findings.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Todas · ${state.findings.size}", filter == null) { filter = null }
                        Pill("Alto · $high", filter == Risk.ALTO, RiskHigh) { filter = Risk.ALTO }
                        Pill("Revisar · $medium", filter == Risk.MEDIO, RiskMedium) { filter = Risk.MEDIO }
                    }
                }
            }

            items(visible, key = { it.packageName }) { f -> FindingCard(f) { actions.uninstall(it, Source.ESCANEO) } }
        }
    }
}

@Composable
private fun Hero(state: UiState, high: Int, medium: Int, onScan: () -> Unit) {
    val target = when {
        state.scanning || !state.hasScanned -> Accent
        high > 0 -> RiskHigh
        medium > 0 -> RiskMedium
        else -> Ok
    }
    val color by animateColorAsState(target, tween(700), label = "status")

    val title = when {
        state.scanning -> "Analizando…"
        !state.hasScanned -> "Listo para cazar"
        high > 0 -> plural(high, "amenaza detectada", "amenazas detectadas")
        medium > 0 -> plural(medium, "app por revisar", "apps por revisar")
        else -> "Dispositivo limpio"
    }
    val subtitle = when {
        state.scanning -> "Revisando las apps instaladas por el usuario"
        !state.hasScanned -> "Pulsa escanear para buscar adware en este móvil"
        else -> "Último análisis a las ${state.lastScan.fmt("HH:mm:ss")}"
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
            Radar(color, state.scanning, state.hasScanned, Modifier.fillMaxSize())
            Image(painterResource(R.drawable.ic_logo), contentDescription = null, Modifier.size(150.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (state.hasScanned && !state.scanning) color else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onScan,
            enabled = !state.scanning,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
        ) {
            if (state.scanning) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Color.White)
            } else {
                Icon(painterResource(R.drawable.ic_tab_scan), contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (state.hasScanned) "Volver a escanear" else "Escanear dispositivo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

// Anillo tipo radar: gira mientras escanea y toma el color del estado
@Composable
private fun Radar(color: Color, scanning: Boolean, done: Boolean, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "angle",
    )
    val breath by transition.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "breath",
    )

    Canvas(modifier) {
        val stroke = 7.dp.toPx()
        val r = size.minDimension / 2 - stroke * 2
        val arcTopLeft = Offset(center.x - r, center.y - r)
        val arcSize = Size(r * 2, r * 2)

        // Halo de color que "respira"
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = 0.30f * breath), Color.Transparent),
                center = center, radius = r * 1.25f,
            ),
            radius = r * 1.25f,
        )

        // Marcas alrededor, como un dial
        for (i in 0 until 72) {
            val a = Math.toRadians(i * 5.0)
            val long = i % 6 == 0
            val r1 = r + stroke * 1.2f
            val r2 = r1 + if (long) stroke * 0.9f else stroke * 0.45f
            drawLine(
                color = color.copy(alpha = if (long) 0.45f else 0.2f),
                start = Offset(center.x + r1 * cos(a).toFloat(), center.y + r1 * sin(a).toFloat()),
                end = Offset(center.x + r2 * cos(a).toFloat(), center.y + r2 * sin(a).toFloat()),
                strokeWidth = 1.2.dp.toPx(),
            )
        }

        // Pista del anillo
        drawCircle(color.copy(alpha = 0.14f), radius = r, style = Stroke(stroke))
        drawCircle(color.copy(alpha = 0.08f), radius = r * 0.72f, style = Stroke(1.dp.toPx()))

        when {
            scanning -> rotate(angle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(0f to Color.Transparent, 0.75f to color, 1f to color, center = center),
                    startAngle = 0f, sweepAngle = 270f, useCenter = false,
                    topLeft = arcTopLeft, size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            done -> drawArc(
                color = color, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = arcTopLeft, size = arcSize, style = Stroke(stroke),
            )
            else -> drawArc(
                color = color.copy(alpha = 0.55f), startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = arcTopLeft, size = arcSize, style = Stroke(stroke * 0.5f),
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FindingCard(f: Finding, onUninstall: (Finding) -> Unit) {
    val riskColor = if (f.risk == Risk.ALTO) RiskHigh else RiskMedium
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(riskColor))
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(f.packageName, f.name)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            f.name.ifBlank { "(sin nombre)" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        PackageText(f.packageName)
                    }
                    Spacer(Modifier.width(8.dp))
                    RiskBadge(f.risk)
                }

                Spacer(Modifier.height(10.dp))
                Text(f.reason, style = MaterialTheme.typography.bodyMedium)
                if (f.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        f.tags.forEach { Tag(it) }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (f.installedAt > 0) "Instalada el ${f.installedAt.fmt("dd/MM/yyyy")}" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    DeleteButton(f.risk == Risk.ALTO) { onUninstall(f) }
                }
            }
        }
    }
}

// =====================================================================
//  RASTREAR
// =====================================================================

@Composable
private fun TraceScreen(state: UiState, actions: Actions) {
    if (!state.usageAccess) {
        AccessRequest(actions.openUsageSettings)
        return
    }

    var showSystem by rememberSaveable { mutableStateOf(false) }
    val entries = state.trace.filter { showSystem || !it.isSystem }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Accent.copy(alpha = 0.22f), Color.Transparent)))
                    .border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                Text("¿Quién ha sido?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cuando salga un anuncio a pantalla completa, ciérralo y abre esta pestaña: " +
                        "la app que lo lanzó aparecerá arriba del todo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15 to "15 min", 60 to "1 h", 1440 to "24 h").forEach { (m, label) ->
                        Pill(label, state.traceMinutes == m) { actions.setTraceMinutes(m) }
                    }
                }
                IconButton(onClick = actions.refreshTrace) {
                    if (state.traceLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(painterResource(R.drawable.ic_refresh), contentDescription = "Actualizar")
                    }
                }
            }
        }

        item {
            Pill(if (showSystem) "Ocultar apps del sistema" else "Mostrar apps del sistema", showSystem) {
                showSystem = !showSystem
            }
        }

        if (entries.isEmpty() && !state.traceLoading) {
            item {
                EmptyState(
                    "Sin actividad",
                    "No hay apps de usuario que se hayan abierto en este intervalo.",
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        itemsIndexed(entries, key = { i, e -> "$i-${e.packageName}" }) { i, e ->
            TimelineRow(e, first = i == 0, last = i == entries.lastIndex, onUninstall = { actions.uninstall(it, Source.RASTREO) })
        }
    }
}

@Composable
private fun AccessRequest(onOpen: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(Accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_tab_trace), contentDescription = null, Modifier.size(48.dp), tint = Accent)
        }
        Spacer(Modifier.height(20.dp))
        Text("Activa el acceso de uso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Para saber qué app lanza los anuncios, Android tiene que dejar que esta herramienta " +
                "vea qué apps se han abierto. En la lista que aparece, entra en AdWareCleaner y activa " +
                "\"Permitir acceso de uso\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
        ) { Text("Abrir ajustes", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun TimelineRow(e: TraceEntry, first: Boolean, last: Boolean, onUninstall: (Finding) -> Unit) {
    val dotColor = when {
        e.finding?.risk == Risk.ALTO -> RiskHigh
        e.finding != null -> RiskMedium
        e.isSystem -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Hora
        Column(Modifier.width(62.dp).padding(top = 14.dp)) {
            Text(
                e.time.fmt("HH:mm"),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (first) Accent else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                e.time.fmt(":ss"),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Línea de tiempo
        Box(Modifier.width(22.dp).fillMaxHeight()) {
            val lineMod = when {
                first && last -> Modifier.height(0.dp)
                first -> Modifier.padding(top = 20.dp).fillMaxHeight()
                last -> Modifier.height(20.dp)
                else -> Modifier.fillMaxHeight()
            }
            Box(Modifier.align(Alignment.TopCenter).then(lineMod).width(2.dp).background(lineColor))
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }

        // Tarjeta
        Card(
            modifier = Modifier.weight(1f).padding(bottom = 10.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (e.finding != null) dotColor.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceContainer
            ),
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(e.packageName, e.name, size = 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            e.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        PackageText(e.packageName)
                    }
                    Spacer(Modifier.width(6.dp))
                    when {
                        e.finding != null -> RiskBadge(e.finding.risk)
                        e.isSystem -> Tag("Sistema")
                    }
                }
                if (!e.isSystem) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            e.finding?.reason ?: "App de usuario",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                        DeleteButton(e.finding?.risk == Risk.ALTO) {
                            onUninstall(
                                e.finding ?: Finding(
                                    e.name, e.packageName, Risk.MEDIO,
                                    "Detectada con el rastreo de actividad", 0L,
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
//  HISTORIAL
// =====================================================================

@Composable
private fun HistoryScreen(history: List<RemovedApp>, onClear: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Historial", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(
                        plural(history.size, "app eliminada", "apps eliminadas") + " con esta herramienta",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (history.isNotEmpty()) {
                    TextButton(onClick = { confirm = true }) { Text("Vaciar", color = RiskHigh) }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                EmptyState(
                    "Sin eliminaciones",
                    "Las apps que borres desde Detectar o Rastrear aparecerán aquí.",
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(history.sortedByDescending { it.removedAt }, key = { "${it.packageName}-${it.removedAt}" }) { r ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Letter(r.name, color = Ok)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PackageText(r.packageName)
                    Text(
                        "Eliminada el ${r.removedAt.fmt()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                SourceBadge(r.source)
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("¿Vaciar historial?") },
            text = { Text("Se borrará la lista de apps eliminadas. Si necesitas el informe, genéralo antes.") },
            confirmButton = { TextButton(onClick = { confirm = false; onClear() }) { Text("Vaciar", color = RiskHigh) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } },
        )
    }
}

// =====================================================================
//  INFORME
// =====================================================================

@Composable
private fun ReportScreen(state: UiState, onWritePdf: (Uri, String, String) -> Unit) {
    var client by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    // Selector de Android para elegir dónde guardar el PDF
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> if (uri != null) onWritePdf(uri, client, notes) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Informe", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Eliminadas", state.history.size, Ok, Modifier.weight(1f))
            StatTile(
                if (state.hasScanned) "Pendientes" else "Sin analizar",
                state.findings.size,
                if (state.findings.isEmpty()) MaterialTheme.colorScheme.onSurface else RiskMedium,
                Modifier.weight(1f),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InfoRow("Dispositivo", deviceName())
            InfoRow("Sistema", androidVersion())
            InfoRow("Fecha", System.currentTimeMillis().fmt())
        }

        OutlinedTextField(
            value = client, onValueChange = { client = it },
            label = { Text("Cliente (opcional)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        )
        OutlinedTextField(
            value = notes, onValueChange = { notes = it },
            label = { Text("Notas (opcional)") }, minLines = 3,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        )

        Button(
            onClick = { launcher.launch("informe_adware_${System.currentTimeMillis().fmt("yyyyMMdd_HHmm")}.pdf") },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
        ) {
            Icon(painterResource(R.drawable.ic_tab_report), contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Generar PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Text(
            "Te pedirá dónde guardarlo (por ejemplo, Descargas). Genera el informe antes de " +
                "desinstalar esta app: el historial se borra con ella.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// =====================================================================
//  Piezas reutilizables
// =====================================================================

@Composable
private fun Pill(text: String, selected: Boolean, color: Color = Accent, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, if (selected) color.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun RiskBadge(risk: Risk) {
    val c = if (risk == Risk.ALTO) RiskHigh else RiskMedium
    Text(
        risk.label.uppercase(),
        color = c,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

// Desde dónde se eliminó: Escaneo (pestaña Detectar) o Rastreo
@Composable
private fun SourceBadge(source: Source) {
    val c = if (source == Source.ESCANEO) Accent else Color(0xFF64B5F6)
    Text(
        source.label.uppercase(),
        color = c,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun DeleteButton(strong: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        colors = if (strong) ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
        else ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(painterResource(R.drawable.ic_delete), contentDescription = null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Eliminar", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PackageText(pkg: String) {
    Text(
        pkg,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(label, Modifier.width(100.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

// Icono real de la app (si sigue instalada) o una letra de respaldo
@Composable
private fun AppIcon(pkg: String, name: String, size: Dp = 44.dp) {
    val context = LocalContext.current
    val bmp = remember(pkg) {
        runCatching {
            context.packageManager.getApplicationIcon(pkg).toBitmap(128, 128).asImageBitmap()
        }.getOrNull()
    }
    if (bmp != null) {
        Image(bmp, contentDescription = null, Modifier.size(size).clip(RoundedCornerShape(12.dp)))
    } else {
        Letter(name, size)
    }
}

@Composable
private fun Letter(name: String, size: Dp = 40.dp, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun EmptyState(title: String, text: String, color: Color) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
