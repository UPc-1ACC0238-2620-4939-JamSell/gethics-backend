package com.jamsell.gethics.analytics.application.internal.commandservices;

import com.jamsell.gethics.analytics.application.internal.outboundservices.AlertDeliveryException;
import com.jamsell.gethics.analytics.application.internal.outboundservices.AlertNotification;
import com.jamsell.gethics.analytics.application.internal.outboundservices.PushNotificationService;
import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.commands.DispatchPendingAlertsCommand;
import com.jamsell.gethics.analytics.domain.model.valueobjects.AlertStatus;
import com.jamsell.gethics.analytics.domain.repositories.AlertRepository;
import com.jamsell.gethics.analytics.domain.services.AlertDispatchCommandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Envia las alertas PENDING por push: SENT si funciona; si el push falla la alerta permanece PENDING y se continua con
 * las demas. Una alerta SENT no vuelve a enviarse. Garantia de entrega: <b>al menos una vez</b>: si el proceso cae
 * despues del push y antes de guardar SENT, la alerta se reenviara en la siguiente ejecucion.
 * <p>
 * Deliberadamente NO es {@code @Transactional}: cada guardado confirma por separado y el envio no ocurre dentro de una
 * transaccion de base de datos. Lo invoca el analisis periodico ({@code TrendAnalysisCommandServiceImpl}) al final de cada
 * ejecucion. No hay limite de reintentos: una alerta cuyo envio falla siempre se reintenta en cada ejecucion.
 */
@Slf4j
@Service
public class AlertDispatchCommandServiceImpl implements AlertDispatchCommandService {

    private final AlertRepository alertRepository;
    private final PushNotificationService pushNotificationService;

    public AlertDispatchCommandServiceImpl(AlertRepository alertRepository, PushNotificationService pushNotificationService) {
        this.alertRepository = alertRepository;
        this.pushNotificationService = pushNotificationService;
    }

    @Override
    public void handle(DispatchPendingAlertsCommand command) {
        int sent = 0;
        int failed = 0;
        for (Alert alert : alertRepository.findPending()) {
            if (alert.getStatus() != AlertStatus.PENDING) {
                continue;
            }
            try {
                pushNotificationService.send(new AlertNotification(alert.getId(), alert.getOwnerId(), alert.getMessage()));
            } catch (AlertDeliveryException e) {
                log.warn("Fallo el envio de la alerta {}: {}", alert.getId(), e.getMessage());
                failed++;
                continue;
            }
            alert.markSent();
            alertRepository.save(alert);
            sent++;
        }
        log.info("Alertas despachadas: {} enviadas, {} siguen pendientes por fallo", sent, failed);
    }
}
