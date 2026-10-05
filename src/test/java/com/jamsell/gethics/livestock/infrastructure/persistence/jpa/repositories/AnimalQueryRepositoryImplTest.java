package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimalQueryRepositoryImplTest {

    @Test
    void withoutSearchMatchesEverything() {
        assertEquals("%", AnimalQueryRepositoryImpl.toPattern(null));
        assertEquals("%", AnimalQueryRepositoryImpl.toPattern("   "));
    }

    @Test
    void searchesByContentInLowerCase() {
        assertEquals("%holstein%", AnimalQueryRepositoryImpl.toPattern("  HolStein "));
    }

    @Test
    void likeWildcardsAndTheEscapeCharacterAreLiteral() {
        assertEquals("%100!%%", AnimalQueryRepositoryImpl.toPattern("100%"));
        assertEquals("%mx!_1%", AnimalQueryRepositoryImpl.toPattern("MX_1"));
        assertEquals("%a!!b%", AnimalQueryRepositoryImpl.toPattern("a!b"));
    }
}
