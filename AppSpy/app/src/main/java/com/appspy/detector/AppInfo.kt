package com.appspy.detector

/**
 * Representa um app instalado no aparelho, com os dados usados
 * para ajudar a identificar comportamento suspeito (tipo malware
 * de propaganda / adware).
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val installTime: Long,       // timestamp (ms) da primeira instalação
    val updateTime: Long,        // timestamp (ms) da última atualização
    val notificationCount24h: Int = 0, // quantidade de notificações nas últimas 24h
    val hasUnknownSource: Boolean = false // instalado fora da Play Store
) {
    /**
     * Heurística simples de "risco": muitas notificações em pouco tempo
     * é o padrão clássico de adware que dispara propaganda a cada
     * poucos segundos.
     */
    fun riskScore(): Int {
        var score = 0
        if (notificationCount24h >= 200) score += 3       // ~ 1 a cada 7 min ou mais frequente
        else if (notificationCount24h >= 48) score += 2    // ~ 1 a cada 30 min
        else if (notificationCount24h >= 10) score += 1
        if (hasUnknownSource) score += 1
        if (!isSystemApp && notificationCount24h > 0 && notificationCount24h.toFloat() / 24f > 20f) {
            // mais de 20 notificações por hora em média -> muito suspeito
            score += 2
        }
        return score
    }

    fun riskLabel(): String = when {
        riskScore() >= 4 -> "ALTO RISCO"
        riskScore() >= 2 -> "SUSPEITO"
        else -> "Normal"
    }
}
