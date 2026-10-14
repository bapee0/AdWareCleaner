package com.testing.adwarecleaner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

// Una app sospechosa encontrada en el escaneo
data class Finding(
    val name: String,
    val packageName: String,
    val risk: Risk,
    val reason: String,
    val installedAt: Long,
    val tags: List<String> = emptyList(),   // información extra, NO cambia el riesgo
)

data class ScanResult(val findings: List<Finding>, val scannedCount: Int)

object Scanner {

    fun scan(context: Context): ScanResult {
        val inspector = Inspector(context)
        val userApps = context.packageManager.getInstalledApplications(0)
            .filter { !inspector.isSystem(it) && it.packageName != context.packageName }
        val findings = userApps
            .mapNotNull { inspector.inspect(it) }
            .sortedWith(compareBy<Finding> { it.risk }.thenBy { it.name.lowercase() })
        return ScanResult(findings, userApps.size)
    }

    fun isInstalled(context: Context, pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)
}

private enum class Origin { SYSTEM, USER, FAKE_VENDOR }

private val SYSTEM_DIRS = listOf("/system/", "/product/", "/vendor/", "/system_ext/", "/odm/", "/oem/", "/preload/")

private const val SETUP_WINDOW_MS = 24 * 60 * 60 * 1000L   // 24 h desde el primer arranque

// Analiza apps. Se usa tanto en el escaneo como en el rastreo.
// Decide por NOMBRE y PAQUETE contra las listas de Patterns.kt, y además
// destapa apps que se disfrazan con un paquete de fabricante.
class Inspector(private val context: Context) {

    private val pm = context.packageManager

    // Apps de sistema "de verdad" (no se pueden falsificar sin root)
    private val realSystemApps: List<String> = pm.getInstalledApplications(0)
        .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 }
        .map { it.packageName }

    // Momento aproximado de la configuración inicial del móvil: la instalación más
    // antigua con fecha real entre las apps de sistema (algunos móviles ponen 2009
    // como fecha ficticia a las de sistema, esas se ignoran).
    private val firstBoot: Long = realSystemApps
        .mapNotNull { runCatching { pm.getPackageInfo(it, 0).firstInstallTime }.getOrNull() }
        .filter { it > 1_262_304_000_000L }   // posterior a 2010
        .minOrNull() ?: 0L

    private val originCache = HashMap<String, Origin>()

    fun isSystem(info: ApplicationInfo): Boolean = origin(info) == Origin.SYSTEM

    fun inspect(info: ApplicationInfo): Finding? {
        val pkg = info.packageName
        val origin = origin(info)
        if (origin == Origin.SYSTEM || pkg == context.packageName || pkg in Patterns.SAFE_PACKAGES) return null

        val name = pm.getApplicationLabel(info).toString()
        val (risk, reason) = when (origin) {
            Origin.FAKE_VENDOR -> Risk.ALTO to "Se hace pasar por app del fabricante"
            else -> classify(name, pkg) ?: return null
        }

        return Finding(name, pkg, risk, reason, installTime(pkg))
    }

    // ¿Quién es realmente esta app?
    private fun origin(info: ApplicationInfo): Origin = originCache.getOrPut(info.packageName) {
        val pkg = info.packageName

        // 1. Marcas de sistema y particiones del sistema: no se pueden falsificar
        if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return@getOrPut Origin.SYSTEM
        if ((info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) return@getOrPut Origin.SYSTEM
        if (SYSTEM_DIRS.any { (info.sourceDir ?: "").startsWith(it) }) return@getOrPut Origin.SYSTEM

        // 2. Sin prefijo de fabricante -> app normal del usuario
        val prefix = Patterns.VENDOR_PREFIXES.firstOrNull { pkg.startsWith(it) }
            ?: return@getOrPut Origin.USER

        // 3. Tiene prefijo de fabricante: hay que demostrarlo con algo infalsificable
        if (sameSignatureAsVendor(pkg, prefix)) return@getOrPut Origin.SYSTEM
        val installed = installTime(pkg)
        if (firstBoot > 0 && installed > 0 && installed - firstBoot < SETUP_WINDOW_MS) return@getOrPut Origin.SYSTEM

        // 4. Viene de una tienda pero sin la firma del fabricante: el adware también
        //    está en las tiendas, así que NO se da por buena; se analiza por nombre.
        if (installer(pkg) in Patterns.TRUSTED_STORES) return@getOrPut Origin.USER

        // 5. Ni firma ni tienda: se está disfrazando
        Origin.FAKE_VENDOR
    }

    // ¿Está firmada con la misma clave que alguna app de sistema de ese fabricante
    // (o con la clave de la propia plataforma)?
    private fun sameSignatureAsVendor(pkg: String, prefix: String): Boolean {
        val match = PackageManager.SIGNATURE_MATCH
        if (runCatching { pm.checkSignatures(pkg, "android") }.getOrNull() == match) return true
        return realSystemApps
            .filter { it.startsWith(prefix) && it != pkg }
            .take(10)
            .any { other -> runCatching { pm.checkSignatures(pkg, other) }.getOrNull() == match }
    }

    // Devuelve (riesgo, motivo) según nombre/paquete, o null si no coincide nada.
    private fun classify(name: String, pkg: String): Pair<Risk, String>? {
        val n = name.trim()

        Patterns.NAME_PREFIXES.firstOrNull { n.startsWith(it) }
            ?.let { return Risk.ALTO to "El nombre empieza por \"$it\"" }

        Patterns.HIGH.firstOrNull { hit(n, pkg, it) }
            ?.let { return Risk.ALTO to "Coincide con \"$it\"" }

        Patterns.MEDIUM.firstOrNull { hit(n, pkg, it) }
            ?.let { return Risk.MEDIO to "Categoría habitual de adware: \"$it\"" }

        return null
    }

    // Solo el nombre visible: el paquete da falsos positivos
    @Suppress("UNUSED_PARAMETER")
    private fun hit(name: String, pkg: String, pattern: String): Boolean =
        name.contains(pattern, ignoreCase = true)

    private fun installTime(pkg: String): Long =
        runCatching { pm.getPackageInfo(pkg, 0).firstInstallTime }.getOrDefault(0L)

    @Suppress("DEPRECATION")
    private fun installer(pkg: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= 30) pm.getInstallSourceInfo(pkg).installingPackageName
        else pm.getInstallerPackageName(pkg)
    }.getOrNull()


}
