package com.testing.adwarecleaner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var historyStore: HistoryStore

    // Todo lo que pinta la interfaz
    private var state by mutableStateOf(UiState())

    // App cuyo diálogo de desinstalar está abierto ahora mismo, y desde qué pestaña
    private var pending: Pair<Finding, Source>? = null
    // Cola para "Eliminar todas las de riesgo alto"
    private val queue = ArrayDeque<Finding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        historyStore = HistoryStore(this)
        state = state.copy(history = historyStore.load())

        val actions = Actions(
            scan = { scan(manual = true) },
            uninstall = ::uninstallOne,
            uninstallAllHigh = ::uninstallAllHigh,
            clearHistory = ::clearHistory,
            writePdf = ::writePdf,
            openUsageSettings = { Tracker.openSettings(this) },
            refreshTrace = ::refreshTrace,
            setTraceMinutes = { m ->
                state = state.copy(traceMinutes = m)
                refreshTrace()
            },
        )

        setContent {
            AdwareTheme {
                AppRoot(state, actions)
            }
        }
    }

    // Al abrir la app y cada vez que volvemos de otra pantalla (desinstalar, ajustes...)
    override fun onResume() {
        super.onResume()

        // ¿Se borró la app que estábamos desinstalando? -> al historial
        pending?.let { (f, source) ->
            if (!Scanner.isInstalled(this, f.packageName)) {
                historyStore.add(
                    RemovedApp(f.name, f.packageName, f.risk, f.reason, System.currentTimeMillis(), source)
                )
                state = state.copy(history = historyStore.load())
            }
            pending = null
        }

        // Si hay cola, lanzamos la siguiente; si no, refrescamos todo
        val next = queue.removeFirstOrNull()
        if (next != null) {
            launchUninstall(next, Source.ESCANEO)
            return
        }

        state = state.copy(usageAccess = Tracker.hasAccess(this))
        if (state.hasScanned) scan(manual = false)
        if (state.usageAccess) refreshTrace()
    }

    private fun scan(manual: Boolean) {
        if (state.scanning) return
        state = state.copy(scanning = true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) { Scanner.scan(this@MainActivity) }
            if (manual) delay(1200) // que se vea el radar girando un momento
            state = state.copy(
                findings = result.findings,
                scannedCount = result.scannedCount,
                lastScan = System.currentTimeMillis(),
                hasScanned = true,
                scanning = false,
            )
        }
    }

    private fun refreshTrace() {
        if (!state.usageAccess || state.traceLoading) return
        state = state.copy(traceLoading = true)
        val minutes = state.traceMinutes
        lifecycleScope.launch {
            val entries = withContext(Dispatchers.Default) {
                runCatching { Tracker.recent(this@MainActivity, minutes) }.getOrDefault(emptyList())
            }
            state = state.copy(trace = entries, traceLoading = false)
        }
    }

    private fun uninstallOne(f: Finding, source: Source) {
        queue.clear()
        launchUninstall(f, source)
    }

    private fun uninstallAllHigh() {
        queue.clear()
        queue.addAll(state.findings.filter { it.risk == Risk.ALTO })
        queue.removeFirstOrNull()?.let { launchUninstall(it, Source.ESCANEO) }
    }

    // Abre el diálogo nativo de Android. El usuario confirma cada borrado.
    private fun launchUninstall(f: Finding, source: Source) {
        pending = f to source
        startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${f.packageName}")))
    }

    private fun clearHistory() {
        historyStore.clear()
        state = state.copy(history = emptyList())
    }

    private fun writePdf(uri: Uri, client: String, notes: String) {
        runCatching {
            PdfReport.write(this, uri, ReportData(client, notes, state.history, state.findings, state.hasScanned))
        }.onSuccess {
            Toast.makeText(this, "Informe guardado", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Error al generar el PDF: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
