package com.appspy.detector

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Serviço de sistema (NotificationListenerService) que é chamado toda vez
 * que QUALQUER app do aparelho dispara uma notificação.
 *
 * Isso é o que permite estimar quantas "propagandas" (notificações) um
 * app malicioso está gerando, já que a maioria dos adwares abusa de
 * notificações para exibir anúncios.
 *
 * IMPORTANTE: o usuário precisa ativar manualmente esse acesso em:
 * Ajustes > Apps > Acesso especial > Acesso a notificações > AppSpy
 * (o Android não permite conceder essa permissão automaticamente,
 * por questões de privacidade/segurança).
 */
class NotificationCounterService : NotificationListenerService() {

    private lateinit var logger: NotificationLogger

    override fun onCreate() {
        super.onCreate()
        logger = NotificationLogger(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        // Ignora notificações do próprio AppSpy para não distorcer a contagem
        if (sbn.packageName == packageName) return
        logger.logNotification(sbn.packageName, sbn.postTime)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        // Não precisamos fazer nada aqui, mantemos o histórico para a contagem de 24h
    }
}
