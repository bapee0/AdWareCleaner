package com.testing.adwarecleaner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportData(
    val client: String,
    val notes: String,
    val removed: List<RemovedApp>,
    val pending: List<Finding>,
    val scanned: Boolean,
)

fun Long.fmt(pattern: String = "dd/MM/yyyy HH:mm"): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))

fun deviceName(): String {
    val maker = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    return if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) Build.MODEL
    else "$maker ${Build.MODEL}"
}

fun androidVersion(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

// Genera el PDF con la API nativa de Android (sin librerías externas)
object PdfReport {
    fun write(context: Context, uri: Uri, data: ReportData) {
        val doc = PdfDocument()
        try {
            ReportWriter(doc, data).render()
            val out = context.contentResolver.openOutputStream(uri)
                ?: error("No se pudo abrir el archivo")
            out.use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }
}

private class ReportWriter(private val doc: PdfDocument, private val data: ReportData) {

    // A4 en puntos
    private val w = 595
    private val h = 842
    private val m = 40f
    private val bottom = h - m - 20f

    private var pageNo = 0
    private var page: PdfDocument.Page? = null
    private lateinit var canvas: Canvas
    private var y = 0f

    private val accent = Color.rgb(229, 57, 53)
    private val amber = Color.rgb(230, 140, 0)
    private val dark = Color.rgb(30, 30, 34)
    private val text = Color.rgb(35, 35, 35)
    private val grey = Color.rgb(120, 120, 120)
    private val zebra = Color.rgb(244, 244, 246)

    private val pTitle = paint(22f, bold = true, c = Color.WHITE)
    private val pSub = paint(10f, c = Color.argb(220, 255, 255, 255))
    private val pH2 = paint(13f, bold = true)
    private val pBody = paint(10f)
    private val pLabel = paint(10f, bold = true, c = grey)
    private val pTh = paint(9f, bold = true, c = Color.WHITE)
    private val pCell = paint(9f)
    private val pMono = paint(8.5f, mono = true)
    private val pSmall = paint(8f, c = grey)
    private val fill = Paint().apply { style = Paint.Style.FILL }

    // Columnas de las tablas: x inicial y ancho
    private val colX = floatArrayOf(m, m + 165f, m + 375f, m + 440f)
    private val colW = floatArrayOf(160f, 205f, 60f, 75f)

    private fun paint(size: Float, bold: Boolean = false, c: Int = text, mono: Boolean = false) =
        Paint().apply {
            isAntiAlias = true
            textSize = size
            color = c
            typeface = Typeface.create(
                if (mono) Typeface.MONOSPACE else Typeface.DEFAULT,
                if (bold) Typeface.BOLD else Typeface.NORMAL
            )
        }

    fun render() {
        newPage()
        header()
        info()

        section("Apps eliminadas (${data.removed.size})")
        if (data.removed.isEmpty()) {
            line("No se ha eliminado ninguna app con esta herramienta.")
        } else {
            table(
                lastHeader = "Eliminada",
                rows = data.removed.sortedBy { it.removedAt }.map {
                    listOf(it.name, it.packageName, it.risk.label, it.removedAt.fmt("dd/MM HH:mm")) to it.risk
                }
            )
        }

        section("Detectadas sin eliminar (${data.pending.size})")
        when {
            !data.scanned -> line("No se ha realizado ningún análisis en esta sesión.")
            data.pending.isEmpty() -> line("Ninguna. El dispositivo ha quedado limpio según el último análisis.")
            else -> table(
                lastHeader = "Instalada",
                rows = data.pending.map {
                    val date = if (it.installedAt > 0) it.installedAt.fmt("dd/MM/yyyy") else "-"
                    listOf(it.name, it.packageName, it.risk.label, date) to it.risk
                }
            )
        }

        if (data.notes.isNotBlank()) {
            section("Notas")
            paragraph(data.notes)
        }

        page?.let { doc.finishPage(it) }
        page = null
    }

    // ---------- bloques ----------

    private fun header() {
        fill.color = accent
        canvas.drawRect(0f, 0f, w.toFloat(), 92f, fill)
        canvas.drawText("Informe de limpieza de adware", m, 48f, pTitle)
        canvas.drawText("Generado el ${System.currentTimeMillis().fmt()}", m, 70f, pSub)
        y = 125f
    }

    private fun info() {
        kv("Cliente", data.client.ifBlank { "-" })
        kv("Dispositivo", deviceName())
        kv("Sistema", androidVersion())
        kv("Resultado", "${data.removed.size} eliminadas · ${data.pending.size} pendientes de revisar")
        y += 6f
    }

    private fun kv(label: String, value: String) {
        canvas.drawText(label, m, y, pLabel)
        canvas.drawText(fit(value, pBody, w - m * 2 - 95f), m + 95f, y, pBody)
        y += 17f
    }

    private fun section(title: String) {
        ensure(60f)
        y += 16f
        canvas.drawText(title, m, y, pH2)
        y += 7f
        fill.color = accent
        canvas.drawRect(m, y, w - m, y + 1.5f, fill)
        y += 18f
    }

    private fun line(s: String) {
        ensure(16f)
        canvas.drawText(fit(s, pBody, w - m * 2), m, y, pBody)
        y += 16f
    }

    private fun paragraph(s: String) {
        val maxW = w - m * 2
        s.split("\n").forEach { raw ->
            var current = ""
            raw.split(" ").forEach { word ->
                val next = if (current.isEmpty()) word else "$current $word"
                if (pBody.measureText(next) > maxW && current.isNotEmpty()) {
                    line(current)
                    current = word
                } else {
                    current = next
                }
            }
            line(current)
        }
    }

    private fun table(lastHeader: String, rows: List<Pair<List<String>, Risk>>) {
        val headers = listOf("Nombre", "Paquete", "Riesgo", lastHeader)
        ensure(40f)
        tableHeader(headers)
        rows.forEachIndexed { i, (cells, risk) ->
            if (y + 18f > bottom) {
                newPage()
                tableHeader(headers)
            }
            if (i % 2 == 0) {
                fill.color = zebra
                canvas.drawRect(m, y - 12f, w - m, y + 6f, fill)
            }
            canvas.drawText(fit(cells[0], pCell, colW[0]), colX[0] + 4f, y, pCell)
            canvas.drawText(fit(cells[1], pMono, colW[1]), colX[1] + 4f, y, pMono)
            val riskPaint = paint(9f, bold = true, c = if (risk == Risk.ALTO) accent else amber)
            canvas.drawText(cells[2], colX[2] + 4f, y, riskPaint)
            canvas.drawText(cells[3], colX[3] + 4f, y, pCell)
            y += 18f
        }
        y += 4f
    }

    private fun tableHeader(headers: List<String>) {
        fill.color = dark
        canvas.drawRect(m, y - 12f, w - m, y + 6f, fill)
        headers.forEachIndexed { i, s -> canvas.drawText(s, colX[i] + 4f, y, pTh) }
        y += 20f
    }

    // ---------- páginas ----------

    private fun newPage() {
        page?.let { doc.finishPage(it) }
        pageNo++
        val p = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo).create())
        page = p
        canvas = p.canvas
        y = m + 10f
        footer()
    }

    private fun footer() {
        canvas.drawText("Generado con AdWare Cleaner", m, h - 22f, pSmall)
        val s = "Página $pageNo"
        canvas.drawText(s, w - m - pSmall.measureText(s), h - 22f, pSmall)
    }

    private fun ensure(space: Float) {
        if (y + space > bottom) newPage()
    }

    // Recorta el texto con "…" si no cabe en el ancho
    private fun fit(s: String, p: Paint, maxW: Float): String {
        if (p.measureText(s) <= maxW - 8f) return s
        var t = s
        while (t.isNotEmpty() && p.measureText("$t…") > maxW - 8f) t = t.dropLast(1)
        return "$t…"
    }
}
