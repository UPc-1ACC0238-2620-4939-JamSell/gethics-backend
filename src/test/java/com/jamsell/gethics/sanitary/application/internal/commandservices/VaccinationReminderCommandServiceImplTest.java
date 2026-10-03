package com.jamsell.gethics.sanitary.application.internal.commandservices;

import com.jamsell.gethics.sanitary.application.internal.outboundservices.NotificationDeliveryException;
import com.jamsell.gethics.sanitary.application.internal.outboundservices.NotificationService;
import com.jamsell.gethics.sanitary.application.internal.outboundservices.VaccinationReminderNotification;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.ProcessVaccinationRemindersCommand;
import com.jamsell.gethics.sanitary.domain.model.entities.Reminder;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.ReminderStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.ReminderCandidateQueryRepository;
import com.jamsell.gethics.sanitary.domain.repositories.ReminderRepository;
import com.jamsell.gethics.sanitary.domain.services.SanitaryScheduleService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VaccinationReminderCommandServiceImplTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    // 08:00 en Lima
    private static final Instant NOW = Instant.parse("2026-10-02T13:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);
    private static final ProcessVaccinationRemindersCommand COMMAND = new ProcessVaccinationRemindersCommand();

    private final ReminderCandidateQueryRepository candidates = mock(ReminderCandidateQueryRepository.class);
    private final ReminderRepository reminders = mock(ReminderRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final SanitaryScheduleService scheduleService = new SanitaryScheduleService();
    private final VaccinationReminderCommandServiceImpl service = new VaccinationReminderCommandServiceImpl(
            candidates, reminders, scheduleService, notifications, Clock.fixed(NOW, LIMA));
    private final ClinicalHistory history = new ClinicalHistory(UUID.randomUUID());

    VaccinationReminderCommandServiceImplTest() {
        when(reminders.createIfAbsent(any())).thenAnswer(i -> Optional.of(i.getArgument(0)));
        when(reminders.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private SanitaryEvent vaccinationIn(int days) {
        return history.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.plusDays(days), "Aftosa");
    }

    private void candidatesAre(SanitaryEvent... events) {
        when(candidates.findVaccinationsAwaitingReminder(any(), any())).thenReturn(List.of(events));
    }

    @Test
    void asksForVaccinationsExactlyThreeDaysAheadOfTheClockDate() {
        service.handle(COMMAND);

        verify(candidates).findVaccinationsAwaitingReminder(LocalDate.of(2026, 10, 5), TODAY.atStartOfDay());
    }

    @Test
    void claimsTheReminderBeforeNotifyingAndMarksItSent() {
        var event = vaccinationIn(3);
        candidatesAre(event);

        service.handle(COMMAND);

        InOrder order = inOrder(reminders, notifications);
        order.verify(reminders).createIfAbsent(any(Reminder.class));
        order.verify(notifications).sendVaccinationReminder(
                new VaccinationReminderNotification(event.getId(), history.getAnimalId(), TODAY.plusDays(3), "Aftosa"));
        var saved = org.mockito.ArgumentCaptor.forClass(Reminder.class);
        order.verify(reminders).save(saved.capture());
        assertEquals(ReminderStatus.SENT, saved.getValue().getStatus());
        assertEquals(NOW, saved.getValue().getSentAt());
        assertEquals(1, saved.getValue().getAttempts());
    }

    @Test
    void doesNothingWithoutCandidates() {
        service.handle(COMMAND);

        verifyNoInteractions(notifications);
        verify(reminders, never()).createIfAbsent(any());
        verify(reminders, never()).save(any());
    }

    @Test
    void alreadyClaimedReminderIsNotNotifiedAgain() {
        candidatesAre(vaccinationIn(3));
        doReturn(Optional.empty()).when(reminders).createIfAbsent(any());

        service.handle(COMMAND);

        verifyNoInteractions(notifications);
        verify(reminders, never()).save(any());
    }

    @Test
    void ignoresCandidatesThatDoNotSatisfyTheDomainRule() {
        var completed = vaccinationIn(3);
        ReflectionTestUtils.setField(completed, "status", SanitaryEventStatus.COMPLETED);
        var cancelled = vaccinationIn(3);
        ReflectionTestUtils.setField(cancelled, "status", SanitaryEventStatus.CANCELLED);
        candidatesAre(vaccinationIn(2), vaccinationIn(4),
                history.scheduleEvent(SanitaryEventType.TREATMENT, TODAY.plusDays(3), null), completed, cancelled);

        service.handle(COMMAND);

        verifyNoInteractions(notifications);
        verify(reminders, never()).createIfAbsent(any());
    }

    @Test
    void deliveryFailureMarksFailedAndContinuesWithTheOtherEvents() {
        var first = vaccinationIn(3);
        var second = vaccinationIn(3);
        candidatesAre(first, second);
        doThrow(new NotificationDeliveryException("proveedor caido")).doNothing()
                .when(notifications).sendVaccinationReminder(any());

        service.handle(COMMAND);

        verify(notifications, times(2)).sendVaccinationReminder(any());
        var saved = org.mockito.ArgumentCaptor.forClass(Reminder.class);
        verify(reminders, times(2)).save(saved.capture());
        assertEquals(ReminderStatus.FAILED, saved.getAllValues().get(0).getStatus());
        assertEquals(1, saved.getAllValues().get(0).getAttempts());
        assertNull(saved.getAllValues().get(0).getSentAt());
        assertEquals(ReminderStatus.SENT, saved.getAllValues().get(1).getStatus());
    }

    @Test
    void reminderThatFailsInThisRunIsNotRetriedInTheSameRun() {
        candidatesAre(vaccinationIn(3));
        doThrow(new NotificationDeliveryException("proveedor caido")).when(notifications).sendVaccinationReminder(any());
        // Aunque el repositorio lo devolviera como reintentable despues de fallar, el snapshot se toma antes.
        when(reminders.findRetryable(TODAY)).thenReturn(List.of());

        service.handle(COMMAND);

        verify(notifications, times(1)).sendVaccinationReminder(any());
        InOrder order = inOrder(reminders);
        order.verify(reminders).findRetryable(TODAY);
        order.verify(reminders).createIfAbsent(any());
    }

    @Test
    void failedReminderIsRetriedAndMarkedSent() {
        var failed = scheduleService.generateReminder(vaccinationIn(2));
        failed.markFailed();
        when(reminders.findRetryable(TODAY)).thenReturn(List.of(failed));

        service.handle(COMMAND);

        verify(notifications).sendVaccinationReminder(any());
        assertEquals(ReminderStatus.SENT, failed.getStatus());
        assertEquals(2, failed.getAttempts());
        verify(reminders).save(failed);
    }

    @Test
    void failedReminderThatFailsAgainStaysFailedWithMoreAttempts() {
        var failed = scheduleService.generateReminder(vaccinationIn(2));
        failed.markFailed();
        when(reminders.findRetryable(TODAY)).thenReturn(List.of(failed));
        doThrow(new NotificationDeliveryException("aun caido")).when(notifications).sendVaccinationReminder(any());

        service.handle(COMMAND);

        assertEquals(ReminderStatus.FAILED, failed.getStatus());
        assertEquals(2, failed.getAttempts());
    }

    @Test
    void failedReminderWhoseEventDatePassedIsNotRetried() {
        var failed = scheduleService.generateReminder(vaccinationIn(-1));
        failed.markFailed();
        when(reminders.findRetryable(TODAY)).thenReturn(List.of(failed));

        service.handle(COMMAND);

        verifyNoInteractions(notifications);
        verify(reminders, never()).save(any());
    }
}
