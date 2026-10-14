package com.testing.adwarecleaner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Una app que se ha eliminado desde esta herramienta
data class RemovedApp(
    val name: String,
    val packageName: String,
    val risk: Risk,
    val reason: String,
    val removedAt: Long,
)

// Guarda el historial en las preferencias de la app (JSON).
// Ojo: se borra al desinstalar esta app, así que genera el PDF antes.
class HistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("historial", Context.MODE_PRIVATE)

    fun load(): List<RemovedApp> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                RemovedApp(
                    name = o.getString("name"),
                    packageName = o.getString("pkg"),
                    risk = Risk.valueOf(o.getString("risk")),
                    reason = o.optString("reason"),
                    removedAt = o.getLong("at"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun add(item: RemovedApp) = save(load() + item)

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private fun save(list: List<RemovedApp>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("name", it.name)
                    .put("pkg", it.packageName)
                    .put("risk", it.risk.name)
                    .put("reason", it.reason)
                    .put("at", it.removedAt)
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private companion object {
        const val KEY = "items"
    }
}
