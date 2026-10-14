package com.testing.adwarecleaner

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

// Una app que se puso en primer plano (abrió una pantalla) en un momento concreto
data class TraceEntry(
    val packageName: String,
    val name: String,
    val time: Long,
    val isSystem: Boolean,
    val finding: Finding?,   // no nulo si además es sospechosa según las reglas
)

// Modo "¿quién ha sido?": lee el registro de uso de Android para ver qué apps
// han abierto pantallas recientemente. Un anuncio a pantalla completa es una
// pantalla de la app de adware, así que aparece aquí con su paquete real.
object Tracker {

    @Suppress("DEPRECATION")
    fun hasAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    @Suppress("DEPRECATION")
    fun recent(context: Context, minutes: Int): List<TraceEntry> {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(end - minutes * 60_000L, end)
        val ev = UsageEvents.Event()

        // Eventos "pasa a primer plano", agrupando repeticiones seguidas de la misma app
        val raw = ArrayList<Pair<String, Long>>()
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            if (ev.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue
            val pkg = ev.packageName ?: continue
            if (raw.isNotEmpty() && raw.last().first == pkg) raw[raw.lastIndex] = pkg to ev.timeStamp
            else raw.add(pkg to ev.timeStamp)
        }

        val pm = context.packageManager
        val inspector = Inspector(context)
        val cache = HashMap<String, Finding?>()

        return raw.asReversed()                         // más reciente primero
            .filter { it.first != context.packageName } // nosotros no
            .mapNotNull { (pkg, time) ->
                val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                    ?: return@mapNotNull null           // ya desinstalada
                val system = inspector.isSystem(info)
                val finding = if (system) null else cache.getOrPut(pkg) { inspector.inspect(info) }
                TraceEntry(pkg, pm.getApplicationLabel(info).toString(), time, system, finding)
            }
            .take(100)
    }
}
