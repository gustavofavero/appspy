package com.appspy.detector

import android.content.Context
import java.io.File

/**
 * Armazena um log simples de "pacote,timestamp" toda vez que uma
 * notificação é disparada. Usa um arquivo de texto local (não precisa
 * de banco de dados) e mantém só os registros das últimas 48h para
 * o arquivo não crescer indefinidamente.
 */
class NotificationLogger(context: Context) {

    private val logFile: File = File(context.filesDir, "notification_log.csv")

    @Synchronized
    fun logNotification(packageName: String, timestamp: Long = System.currentTimeMillis()) {
        logFile.appendText("$packageName,$timestamp\n")
    }

    /**
     * Retorna um mapa packageName -> quantidade de notificações
     * disparadas nas últimas [windowHours] horas.
     */
    @Synchronized
    fun countLastHours(windowHours: Int = 24): Map<String, Int> {
        if (!logFile.exists()) return emptyMap()
        val cutoff = System.currentTimeMillis() - windowHours * 60L * 60L * 1000L
        val counts = mutableMapOf<String, Int>()
        logFile.forEachLine { line ->
            val parts = line.split(",")
            if (parts.size == 2) {
                val pkg = parts[0]
                val ts = parts[1].toLongOrNull() ?: 0L
                if (ts >= cutoff) {
                    counts[pkg] = (counts[pkg] ?: 0) + 1
                }
            }
        }
        return counts
    }

    /**
     * Limpa entradas com mais de 48h para não deixar o arquivo crescer para sempre.
     * Chame periodicamente (ex: toda vez que o app abre).
     */
    @Synchronized
    fun trimOldEntries(maxAgeHours: Int = 48) {
        if (!logFile.exists()) return
        val cutoff = System.currentTimeMillis() - maxAgeHours * 60L * 60L * 1000L
        val kept = logFile.readLines().filter { line ->
            val parts = line.split(",")
            parts.size == 2 && (parts[1].toLongOrNull() ?: 0L) >= cutoff
        }
        logFile.writeText(kept.joinToString("\n", postfix = if (kept.isNotEmpty()) "\n" else ""))
    }
}
