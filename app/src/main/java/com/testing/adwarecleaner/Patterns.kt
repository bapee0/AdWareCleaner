package com.testing.adwarecleaner

// =====================================================================
//  LISTAS DE DETECCIÓN — el único fichero que tendrás que tocar para
//  afinar qué se detecta. Todo se compara sin distinguir mayúsculas.
//
//  Solo se miran apps instaladas por el usuario; las del sistema y las
//  preinstaladas por el fabricante nunca se tocan.
// =====================================================================

object Patterns {

    // Se busca SOLO en el NOMBRE VISIBLE de la app (lo que ves en Ajustes > Aplicaciones),
    // sin distinguir mayúsculas y en cualquier posición: "clean" pilla "Super Cleaner",
    // "CleanBoost" o "Phone Clean". Añade aquí las que te vayas encontrando.

    // RIESGO ALTO: casi siempre adware. Son las que borras sin pensarlo.
    val HIGH = listOf(
        "clean",        // cleaner, clean master, phone clean...
        "clear",        // clear cache, clear phone...
        "boost",        // booster, phone boost...
        "junk",
        "cooler",
        "limpia",       // limpiador, limpieza...
        "acelera",      // acelerador...
    )

    // REVISAR: muchas son adware, pero hay alguna legítima. Míralas antes de borrar.
    val MEDIUM = listOf(
        "pdf",
        "qr",
        "scanner",
        "escáner",
        "escaner",
        "flashlight",
        "linterna",
    )

    // Prefijos: solo cuentan si el NOMBRE VISIBLE empieza por ellos (riesgo alto).
    val NAME_PREFIXES = listOf(
        "#",
    )

    // Fabricantes: apps preinstaladas de Xiaomi, Samsung, Google... que se pueden
    // desinstalar. Un paquete con estos prefijos solo se da por bueno si su firma
    // o su origen lo confirman; si no, se marca como suplantación (ver Scanner.kt).
    val VENDOR_PREFIXES = listOf(
        "com.android.", "android.", "com.google.",
        "com.miui.", "com.xiaomi.", "com.mi.", "com.mfashiongallery.",
        "com.samsung.", "com.sec.", "com.sem.",
        "com.huawei.", "com.hihonor.", "com.honor.",
        "com.oppo.", "com.coloros.", "com.heytap.", "com.oplus.", "com.realme.",
        "com.oneplus.", "com.vivo.", "com.iqoo.", "com.bbk.",
        "com.motorola.", "com.lenovo.", "com.zui.",
        "com.sony.", "com.sonymobile.", "com.lge.", "com.nokia.", "com.hmdglobal.",
        "com.asus.", "com.tcl.", "com.transsion.", "com.qualcomm.", "com.mediatek.",
        "com.facebook.appmanager", "com.facebook.services", "com.facebook.system",
    )

    // Tiendas oficiales. Una app con prefijo de fabricante instalada desde aquí pero SIN
    // su firma no se marca como suplantación, pero tampoco se da por buena: se analiza
    // por nombre como cualquier otra (en las tiendas también hay adware).
    // OJO: los instaladores de APK (packageinstaller) NO van aquí, eso es instalar a mano.
    val TRUSTED_STORES = setOf(
        "com.android.vending",                   // Google Play
        "com.xiaomi.market", "com.xiaomi.mipicks", "com.xiaomi.discover",
        "com.sec.android.app.samsungapps",       // Galaxy Store
        "com.huawei.appmarket",                  // AppGallery
        "com.heytap.market", "com.oppo.market",  // OPPO / Realme
        "com.bbk.appstore",                      // vivo
    )

    // Apps legítimas que nunca se marcan aunque coincidan con algo de arriba.
    // Añade aquí cualquier falso positivo que te encuentres (nombre de paquete exacto).
    val SAFE_PACKAGES = setOf(
        "com.piriform.ccleaner",                 // CCleaner
        "com.avast.android.cleaner",             // Avast Cleanup
        "com.avast.android.mobilesecurity",      // Avast Antivirus
        "com.avg.cleaner",                       // AVG Cleaner
        "com.antivirus",                         // AVG Antivirus
        "com.bitdefender.security",              // Bitdefender
        "com.kaspersky.security.cloud",          // Kaspersky
        "com.kms.free",                          // Kaspersky (antigua)
        "com.eset.ems2.gp",                      // ESET
        "org.malwarebytes.antimalware",          // Malwarebytes
        "com.mcafee.android",                    // McAfee
        "com.adobe.reader",                      // Adobe Acrobat
        "com.adobe.scan.android",                // Adobe Scan
        "com.microsoft.office.officehubrow",     // Microsoft 365
        "com.microsoft.office.lens",             // Microsoft Lens
        "com.microsoft.launcher",                // Microsoft Launcher
        "com.microsoft.translator",              // Microsoft Translator
        "com.teslacoilsw.launcher",              // Nova Launcher
        "bitpit.launcher",                       // Niagara Launcher
        "com.duckduckgo.mobile.android",
        "com.cloudflare.onedotonedotonedotone",  // 1.1.1.1
        "com.nordvpn.android",
        "com.expressvpn.vpn",
        "ch.protonvpn.android",
    )
}

enum class Risk(val label: String) {
    ALTO("Alto"),
    MEDIO("Revisar"),
}
