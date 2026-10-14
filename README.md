<p align="center">
  <img src="docs/logo.png" width="160" alt="AdWare Cleaner">
</p>

<h1 align="center">AdWare Cleaner</h1>

<p align="center">
  Herramienta Android para detectar y eliminar adware en móviles de forma rápida y controlada.<br>
  Pensada para técnicos que limpian dispositivos de clientes.
</p>

<p align="center">
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Sin root" src="https://img.shields.io/badge/root-no%20necesario-E53935">
</p>

---

## ¿Por qué existe?

El adware típico de Android se instala desde Play Store disfrazado de limpiador, acelerador o lector de PDF. Una vez dentro, llena la pantalla de anuncios a pantalla completa, y cada anuncio lleva a instalar otra app parecida: una cadena que no para.

Limpiar un móvil así a mano significa entrar en **Ajustes > Aplicaciones** y revisar una a una buscando nombres como *Clean*, *Clear*, *Booster* o *PDF*. AdWare Cleaner automatiza exactamente ese proceso: filtra las sospechosas al instante y deja que el técnico las elimine con un toque.

## Funciones

| Pestaña | Qué hace |
|---|---|
| **Detectar** | Escanea las apps instaladas por el usuario y muestra las sospechosas, clasificadas por riesgo, con su icono, paquete y fecha de instalación. Permite eliminarlas una a una o todas las de riesgo alto seguidas. |
| **Rastrear** | Modo *"¿quién ha sido?"*: línea de tiempo de las apps que han abierto pantallas en los últimos 15 min, 1 h o 24 h. Si acaba de salir un anuncio, la app que lo lanzó aparece arriba del todo, aunque tenga un nombre inocente. |
| **Historial** | Registro de todas las apps eliminadas con la herramienta, con fecha y hora. |
| **Informe** | Genera un PDF con los datos del dispositivo, las apps eliminadas y las pendientes de revisar, listo para entregar al cliente. |

## Cómo detecta

La detección replica el método manual de un técnico: **buscar palabras clave en el nombre visible** de las apps.

- **Riesgo alto**: nombres que contienen *clean*, *clear*, *boost*, *junk*, *cooler*, *limpia*, *acelera*, o que empiezan por *#*.
- **Revisar**: *pdf*, *qr*, *scanner*, *escáner*, *linterna*... categorías donde abunda el adware pero también hay apps legítimas.

Además destapa un truco habitual: apps que usan un **nombre de paquete de fabricante** (com.google, com.miui, com.samsung...) para pasar desapercibidas. Si una app así no está firmada por el fabricante ni vino con el móvil, se marca como suplantación.

Para evitar ruido:

- **Nunca se tocan las apps del sistema** ni las preinstaladas por el fabricante.
- Hay una **lista blanca** de apps legítimas (antivirus conocidos, Adobe, Microsoft...) que no se marcan aunque coincidan.
- La herramienta **nunca borra nada sola**: cada eliminación pasa por el diálogo nativo de Android y la confirma el técnico.

Todas las listas están en un único fichero, **Patterns.kt**, para ajustarlas sin tocar el resto del código.

## Instalación

1. Descarga el APK desde [Releases](../../releases).
2. Ábrelo en el móvil y permite la instalación desde origen desconocido.
3. Si Play Protect avisa, elige *Instalar de todos modos*.

Para el modo **Rastrear**, la primera vez hay que conceder el permiso de *acceso de uso*: la propia app lleva a la pantalla de ajustes correspondiente.

Al terminar la limpieza, genera el informe y desinstala la herramienta (el historial se borra con ella).

### Permisos

| Permiso | Para qué |
|---|---|
| QUERY_ALL_PACKAGES | Ver todas las apps instaladas |
| REQUEST_DELETE_PACKAGES | Abrir el diálogo de desinstalación |
| PACKAGE_USAGE_STATS | Modo Rastrear (se concede a mano en ajustes) |

No usa internet ni envía ningún dato fuera del dispositivo.

## Compilar desde el código

Requisitos: Android Studio reciente y JDK incluido.

```bash
git clone https://github.com/bapee0/AdWareCleaner.git
```

Ábrelo en Android Studio, espera a que sincronice Gradle y pulsa **Run**. Para generar un APK instalable: **Build > Generate Signed App Bundle or APK > APK > release**.

## Estructura

```
app/src/main/java/com/testing/adwarecleaner/
├── Patterns.kt      # Listas de palabras, fabricantes y lista blanca
├── Scanner.kt       # Lógica de detección
├── Tracker.kt       # Modo Rastrear (registro de uso de Android)
├── HistoryStore.kt  # Historial de eliminaciones
├── PdfReport.kt     # Generación del informe PDF
├── Screens.kt       # Interfaz (Jetpack Compose)
├── Theme.kt         # Colores y tema
└── MainActivity.kt  # Une todo
```

## Aviso

Proyecto desarrollado con ayuda de IA como herramienta de trabajo personal. Úsala bajo tu responsabilidad y revisa siempre lo que vas a eliminar, sobre todo en la categoría *Revisar*.

---

<p align="center">Hecho por <a href="https://github.com/bapee0">bapee0</a> · BlumeSec</p>
