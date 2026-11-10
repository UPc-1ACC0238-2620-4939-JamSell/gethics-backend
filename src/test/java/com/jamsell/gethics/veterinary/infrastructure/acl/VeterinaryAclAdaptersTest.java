package com.jamsell.gethics.veterinary.infrastructure.acl;

import com.jamsell.gethics.livestock.interfaces.acl.AnimalAccessInfo;
import com.jamsell.gethics.livestock.interfaces.acl.AnimalSummary;
import com.jamsell.gethics.livestock.interfaces.acl.LivestockContextFacade;
import com.jamsell.gethics.sanitary.interfaces.acl.ClinicalSummaryView;
import com.jamsell.gethics.sanitary.interfaces.acl.SanitaryContextFacade;
import com.jamsell.gethics.veterinary.domain.model.aggregates.CareRecord;
import com.jamsell.gethics.veterinary.domain.model.commands.RegisterCareCommand;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.ClinicalSummary;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientAccess;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientInfo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VeterinaryAclAdaptersTest {

    // 10:00 en Lima del 2026-05-02.
    private static final Instant NOW = Instant.parse("2026-05-02T15:00:00Z");

    private final LivestockContextFacade livestock = mock(LivestockContextFacade.class);
    private final SanitaryContextFacade sanitary = mock(SanitaryContextFacade.class);
    private final Clock clock = Clock.fixed(NOW, ZoneId.of("America/Lima"));
    private final SanitaryClinicalHistoryAdapter historyAdapter = new SanitaryClinicalHistoryAdapter(sanitary, clock);

    private static RegisterCareCommand care(Instant occurredAt, LocalDate nextControl) {
        return new RegisterCareCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Mastitis",
                "Antibiotico", nextControl, occurredAt);
    }

    @Test
    void patientsAreTheActiveAnimalsOfTheClient() {
        var clientId = UUID.randomUUID();
        var animalId = UUID.randomUUID();
        when(livestock.findActiveAnimalsByOwnerId(clientId))
                .thenReturn(List.of(new AnimalSummary(animalId, "Lola", "A-1", "Holstein", "ACTIVE")));

        var patients = new LivestockPatientsAdapter(livestock).findPatientsByClientId(clientId);

        assertEquals(List.of(new PatientInfo(animalId, "Lola", "A-1", "Holstein", "ACTIVE")), patients);
    }

    @Test
    void patientAccessComesFromLivestock() {
        var animalId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        when(livestock.findAnimalAccess(animalId)).thenReturn(Optional.of(new AnimalAccessInfo(animalId, true, ownerId)));

        assertEquals(Optional.of(new PatientAccess(animalId, true, ownerId)),
                new LivestockPatientAccessAdapter(livestock).findByPatientId(animalId));
    }

    @Test
    void unknownPatientHasNoAccessInfo() {
        var animalId = UUID.randomUUID();
        when(livestock.findAnimalAccess(animalId)).thenReturn(Optional.empty());

        assertTrue(new LivestockPatientAccessAdapter(livestock).findByPatientId(animalId).isEmpty());
    }

    @Test
    void clientLocationComesFromLivestock() {
        var clientId = UUID.randomUUID();
        when(livestock.findLocationByOwnerId(clientId)).thenReturn(Optional.of("Cajamarca"));

        assertEquals(Optional.of("Cajamarca"),
                new LivestockClientLocationAdapter(livestock).findLocationByClientId(clientId));
    }

    @Test
    void clinicalSummaryComesFromSanitary() {
        var patientId = UUID.randomUUID();
        when(sanitary.findClinicalSummary(patientId))
                .thenReturn(Optional.of(new ClinicalSummaryView(3, LocalDate.of(2026, 4, 2), "CHECKUP")));

        var summary = new SanitaryClinicalSummaryAdapter(sanitary).findSummaryByPatientId(patientId);

        assertEquals(Optional.of(new ClinicalSummary(3, LocalDate.of(2026, 4, 2), "CHECKUP")), summary);
    }

    @Test
    void clinicalSummaryIsEmptyWhenSanitaryHasNothing() {
        var patientId = UUID.randomUUID();
        when(sanitary.findClinicalSummary(patientId)).thenReturn(Optional.empty());

        assertTrue(new SanitaryClinicalSummaryAdapter(sanitary).findSummaryByPatientId(patientId).isEmpty());
    }

    @Test
    void summariesOfAllThePatientsAreRequestedTogether() {
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        when(sanitary.findClinicalSummaries(Set.of(a, b)))
                .thenReturn(Map.of(a, new ClinicalSummaryView(2, LocalDate.of(2026, 4, 2), "TREATMENT")));

        var summaries = new SanitaryClinicalSummaryAdapter(sanitary).findSummariesByPatientIds(Set.of(a, b));

        assertEquals(Map.of(a, new ClinicalSummary(2, LocalDate.of(2026, 4, 2), "TREATMENT")), summaries);
    }

    @Test
    void careIsAppendedToTheClinicalHistoryWithTheRequestIdAsIdempotencyKey() {
        var command = care(Instant.parse("2026-05-01T15:30:00Z"), null);

        historyAdapter.append(new CareRecord(command));

        // 15:30Z en America/Lima (UTC-5) son las 10:30 locales.
        verify(sanitary).registerVeterinaryCare(command.patientId(), command.clientRequestId(),
                LocalDateTime.of(2026, 5, 1, 10, 30),
                "Atencion veterinaria. Diagnostico: Mastitis. Tratamiento: Antibiotico.");
        verify(sanitary, never()).scheduleVeterinaryFollowUp(any(), any(), any(), anyString());
    }

    @Test
    void aCareDatedSlightlyInTheFutureIsClampedToNowSoSanitaryDoesNotRejectIt() {
        var command = care(NOW.plusSeconds(180), null);

        historyAdapter.append(new CareRecord(command));

        verify(sanitary).registerVeterinaryCare(command.patientId(), command.clientRequestId(),
                LocalDateTime.of(2026, 5, 2, 10, 0), "Atencion veterinaria. Diagnostico: Mastitis. Tratamiento: Antibiotico.");
    }

    @Test
    void aFutureNextControlIsScheduledAsACheckupWithAStableId() {
        var command = care(Instant.parse("2026-05-01T15:30:00Z"), LocalDate.of(2026, 6, 15));
        var record = new CareRecord(command);

        historyAdapter.append(record);
        historyAdapter.append(record);

        var expectedId = SanitaryClinicalHistoryAdapter.followUpRequestId(record);
        assertNotEquals(command.clientRequestId(), expectedId);
        verify(sanitary, org.mockito.Mockito.times(2)).scheduleVeterinaryFollowUp(command.patientId(), expectedId,
                LocalDate.of(2026, 6, 15), "Control programado en la atencion veterinaria. Diagnostico: Mastitis.");
    }

    @Test
    void aNextControlThatAlreadyPassedIsNotScheduled() {
        var command = care(Instant.parse("2026-04-20T15:30:00Z"), LocalDate.of(2026, 4, 25));

        historyAdapter.append(new CareRecord(command));

        verify(sanitary, never()).scheduleVeterinaryFollowUp(any(), any(), any(), anyString());
    }

    @Test
    void aNextControlOfTodayIsStillScheduled() {
        var command = care(Instant.parse("2026-05-02T14:00:00Z"), LocalDate.of(2026, 5, 2));

        historyAdapter.append(new CareRecord(command));

        verify(sanitary).scheduleVeterinaryFollowUp(any(), any(), any(), anyString());
    }

    @Test
    void longDescriptionsAreTruncatedToTheSanitaryColumnSize() {
        var command = new RegisterCareCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "d".repeat(2000), "Antibiotico", null, Instant.parse("2026-05-01T15:30:00Z"));

        historyAdapter.append(new CareRecord(command));

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sanitary).registerVeterinaryCare(any(), any(), any(), captor.capture());
        assertEquals(SanitaryClinicalHistoryAdapter.DESCRIPTION_MAX_LENGTH, captor.getValue().length());
        assertTrue(captor.getValue().endsWith("..."));
    }

    @Test
    void theFollowUpIdIsADeterministicNameBasedUuidOfTheCareRequest() {
        var record = new CareRecord(care(Instant.parse("2026-05-01T15:30:00Z"), null));

        var expected = UUID.nameUUIDFromBytes(("veterinary-care-follow-up:" + record.getClientRequestId())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertEquals(expected, SanitaryClinicalHistoryAdapter.followUpRequestId(record));
    }

    @Test
    void aDescriptionOfExactlyTheMaximumLengthIsKeptWhole() {
        var base = SanitaryClinicalHistoryAdapter.describe(new CareRecord(new RegisterCareCommand(UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "x", "Antibiotico", null, Instant.parse("2026-05-01T15:30:00Z"))));
        var diagnosis = "d".repeat(SanitaryClinicalHistoryAdapter.DESCRIPTION_MAX_LENGTH - base.length() + 1);
        var command = new RegisterCareCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), diagnosis,
                "Antibiotico", null, Instant.parse("2026-05-01T15:30:00Z"));

        historyAdapter.append(new CareRecord(command));

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sanitary).registerVeterinaryCare(any(), any(), any(), captor.capture());
        assertEquals(SanitaryClinicalHistoryAdapter.DESCRIPTION_MAX_LENGTH, captor.getValue().length());
        assertTrue(!captor.getValue().endsWith("..."));
    }
}
