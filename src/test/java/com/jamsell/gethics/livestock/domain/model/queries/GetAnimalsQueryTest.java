package com.jamsell.gethics.livestock.domain.model.queries;

import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalDataException;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetAnimalsQueryTest {

    @Test
    void defaultsToActiveAnimalsWithoutSearch() {
        var query = GetAnimalsQuery.of(null, null);

        assertNull(query.search());
        assertEquals(AnimalStatus.ACTIVE, query.status());
    }

    @Test
    void blankSearchMeansNoSearch() {
        assertNull(GetAnimalsQuery.of("   ", null).search());
        assertNull(GetAnimalsQuery.of("", null).search());
    }

    @Test
    void trimsTheSearchCriterionAndKeepsTheStatus() {
        var query = GetAnimalsQuery.of("  Holstein ", AnimalStatus.SOLD);

        assertEquals("Holstein", query.search());
        assertEquals(AnimalStatus.SOLD, query.status());
    }

    @Test
    void acceptsASearchOfExactlyTheMaximumLength() {
        assertEquals(100, GetAnimalsQuery.of("x".repeat(100), null).search().length());
    }

    @Test
    void rejectsASearchLongerThanTheMaximum() {
        assertThrows(InvalidAnimalDataException.class, () -> GetAnimalsQuery.of("x".repeat(101), null));
    }
}
