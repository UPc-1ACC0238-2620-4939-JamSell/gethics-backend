package com.jamsell.gethics.veterinary.application.internal.queryservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClientPatientsLookup;
import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClinicalSummaryLookup;
import com.jamsell.gethics.veterinary.domain.model.exceptions.ClientNotAssignedException;
import com.jamsell.gethics.veterinary.domain.model.queries.GetClientPatientsQuery;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.ClinicalSummary;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientInfo;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.VeterinaryAssignmentStatus;
import com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories.VeterinaryAssignmentRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VeterinaryPatientQueryServiceImplTest {

    private final VeterinaryAssignmentRepository repository = mock(VeterinaryAssignmentRepository.class);
    private final ClientPatientsLookup patientsLookup = mock(ClientPatientsLookup.class);
    private final ClinicalSummaryLookup summaryLookup = mock(ClinicalSummaryLookup.class);
    private final VeterinaryPatientQueryServiceImpl service =
            new VeterinaryPatientQueryServiceImpl(repository, patientsLookup, summaryLookup);

    private final UUID veterinarianId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    private void givenAssignedClient() {
        when(repository.existsByVeterinarianIdAndClientIdAndStatus(
                veterinarianId, clientId, VeterinaryAssignmentStatus.ACTIVE)).thenReturn(true);
    }

    @Test
    void throwsWhenClientIsNotAssigned() {
        when(repository.existsByVeterinarianIdAndClientIdAndStatus(
                veterinarianId, clientId, VeterinaryAssignmentStatus.ACTIVE)).thenReturn(false);

        assertThrows(ClientNotAssignedException.class,
                () -> service.handle(new GetClientPatientsQuery(veterinarianId, clientId)));
    }

    @Test
    void returnsEmptyListWhenClientHasNoAnimals() {
        givenAssignedClient();
        when(patientsLookup.findPatientsByClientId(clientId)).thenReturn(List.of());

        var result = service.handle(new GetClientPatientsQuery(veterinarianId, clientId));

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsPatientsWithClinicalSummary() {
        givenAssignedClient();
        var patientId = UUID.randomUUID();
        var patient = new PatientInfo(patientId, "Lola", "TAG-001", "Holstein", "ACTIVE");
        var summary = new ClinicalSummary(3, LocalDate.of(2026, 9, 20), "VACCINATION");
        when(patientsLookup.findPatientsByClientId(clientId)).thenReturn(List.of(patient));
        when(summaryLookup.findSummaryByPatientId(patientId)).thenReturn(Optional.of(summary));

        var result = service.handle(new GetClientPatientsQuery(veterinarianId, clientId));

        assertEquals(1, result.size());
        assertEquals("Lola", result.get(0).patient().name());
        assertEquals(3, result.get(0).clinicalSummary().totalEvents());
    }

    @Test
    void returnsPatientWithoutSummaryWhenNoClinicalHistory() {
        givenAssignedClient();
        var patientId = UUID.randomUUID();
        var patient = new PatientInfo(patientId, "Rocio", "TAG-002", "Brown Swiss", "ACTIVE");
        when(patientsLookup.findPatientsByClientId(clientId)).thenReturn(List.of(patient));
        when(summaryLookup.findSummaryByPatientId(patientId)).thenReturn(Optional.empty());

        var result = service.handle(new GetClientPatientsQuery(veterinarianId, clientId));

        assertEquals(1, result.size());
        assertNull(result.get(0).clinicalSummary());
    }
}
