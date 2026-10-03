package com.jamsell.gethics.analytics.application.internal.commandservices;

import com.jamsell.gethics.analytics.application.internal.outboundservices.AlertDeliveryException;
import com.jamsell.gethics.analytics.application.internal.outboundservices.AlertNotification;
import com.jamsell.gethics.analytics.application.internal.outboundservices.PushNotificationService;
import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.aggregates.Analytics;
import com.jamsell.gethics.analytics.domain.model.commands.DispatchPendingAlertsCommand;
import com.jamsell.gethics.analytics.domain.model.valueobjects.AlertStatus;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import com.jamsell.gethics.analytics.domain.repositories.AlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertDispatchCommandServiceImplTest {

    private static final DispatchPendingAlertsCommand COMMAND = new DispatchPendingAlertsCommand();

    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final PushNotificationService push = mock(PushNotificationService.class);
    private final AlertDispatchCommandServiceImpl service = new AlertDispatchCommandServiceImpl(alertRepository, push);

    AlertDispatchCommandServiceImplTest() {
        when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Alert pendingAlert(String message) {
        var ownerId = UUID.randomUUID();
        var trend = new Analytics(ownerId, RiskLevel.LOW)
                .registerTrend(TrendType.SANITARY, RiskLevel.HIGH, "d", Instant.now(), RiskLevel.LOW);
        var alert = Alert.create(ownerId, trend, message);
        ReflectionTestUtils.setField(alert, "id", UUID.randomUUID());
        return alert;
    }

    @Test
    void pendingAlertIsPushedAndMarkedSent() {
        var alert = pendingAlert("mensaje de prueba");
        when(alertRepository.findPending()).thenReturn(List.of(alert));

        service.handle(COMMAND);

        verify(push).send(new AlertNotification(alert.getId(), alert.getOwnerId(), "mensaje de prueba"));
        assertEquals(AlertStatus.SENT, alert.getStatus());
        verify(alertRepository).save(alert);
    }

    @Test
    void sentAlertIsNotSentAgain() {
        var alert = pendingAlert("m");
        alert.markSent();
        when(alertRepository.findPending()).thenReturn(List.of(alert));

        service.handle(COMMAND);

        verifyNoInteractions(push);
        verify(alertRepository, never()).save(any());
    }

    @Test
    void secondRunDoesNotResendWhatTheFirstOneSent() {
        var alert = pendingAlert("m");
        when(alertRepository.findPending()).thenReturn(List.of(alert)).thenReturn(List.of());

        service.handle(COMMAND);
        service.handle(COMMAND);

        verify(push, times(1)).send(any());
    }

    @Test
    void pushFailureKeepsTheAlertPendingAndContinuesWithTheOthers() {
        var failing = pendingAlert("falla");
        var working = pendingAlert("funciona");
        when(alertRepository.findPending()).thenReturn(List.of(failing, working));
        doThrow(new AlertDeliveryException("proveedor caido")).doNothing().when(push).send(any());

        service.handle(COMMAND);

        verify(push, times(2)).send(any());
        assertEquals(AlertStatus.PENDING, failing.getStatus());
        assertEquals(AlertStatus.SENT, working.getStatus());
        verify(alertRepository, never()).save(failing);
        verify(alertRepository).save(working);
    }

    @Test
    void failedAlertIsRetriedByALaterRun() {
        var alert = pendingAlert("m");
        when(alertRepository.findPending()).thenReturn(List.of(alert));
        doThrow(new AlertDeliveryException("caido")).doNothing().when(push).send(any());

        service.handle(COMMAND);
        assertEquals(AlertStatus.PENDING, alert.getStatus());
        service.handle(COMMAND);

        assertEquals(AlertStatus.SENT, alert.getStatus());
    }

    @Test
    void nothingPendingDoesNothing() {
        when(alertRepository.findPending()).thenReturn(List.of());

        service.handle(COMMAND);

        verifyNoInteractions(push);
    }
}
