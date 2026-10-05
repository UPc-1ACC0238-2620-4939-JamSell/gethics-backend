package com.jamsell.gethics.veterinary.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jamsell.gethics.veterinary.domain.model.commands.RegisterCareCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareRecordTest {

    private static RegisterCareCommand command(String diagnosis, LocalDate nextControl, Instant occurredAt) {
        return new RegisterCareCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                diagnosis, "Antibiotic", nextControl, occurredAt);
    }

    @Test
    void validCommandCreatesRecordNotYetSynced() {
        var record = new CareRecord(command("Mastitis", LocalDate.now().plusDays(7), null));

        assertFalse(record.isHistorySynced());
    }

    @Test
    void blankDiagnosisThrows() {
        assertThrows(IllegalArgumentException.class, () -> new CareRecord(command(" ", null, null)));
    }

    @Test
    void futureCareDateThrows() {
        var future = Instant.now().plus(2, ChronoUnit.DAYS);

        assertThrows(IllegalArgumentException.class, () -> new CareRecord(command("Mastitis", null, future)));
    }

    @Test
    void nextControlBeforeCareDateThrows() {
        var yesterday = LocalDate.now().minusDays(1);

        assertThrows(IllegalArgumentException.class,
                () -> new CareRecord(command("Mastitis", yesterday, Instant.now())));
    }

    @Test
    void matchesDetectsSameAndDifferentContent() {
        var original = command("Mastitis", null, null);
        var record = new CareRecord(original);
        var different = new RegisterCareCommand(original.clientRequestId(), original.veterinarianId(),
                original.patientId(), "Other", original.treatment(), null, null);

        assertTrue(record.matches(original));
        assertFalse(record.matches(different));
    }
}
