package com.jamsell.gethics.livestock.application.internal.commandservices;

import com.jamsell.gethics.livestock.application.internal.outboundservices.SaleIncomeRegistrar;
import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotActiveException;
import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.FarmNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalSaleException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.DeactivateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.commands.SellAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.UpdateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.events.AnimalLeftActiveHerdEvent;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.repositories.AnimalFarmAssignmentRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** US-30 (venta + ingreso), los eventos que avisan a otros contextos y la edicion bloqueada de animales no activos. */
class AnimalSaleServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2042, 6, 7);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2042-06-07T13:00:00Z"), ZoneId.of("America/Lima"));

    private final AnimalRepository repository = mock(AnimalRepository.class);
    private final FarmRepository farms = mock(FarmRepository.class);
    private final AnimalFarmAssignmentRepository assignments = mock(AnimalFarmAssignmentRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final SaleIncomeRegistrar income = mock(SaleIncomeRegistrar.class);
    private final AnimalCommandServiceImpl service =
            new AnimalCommandServiceImpl(repository, farms, assignments, CLOCK, events, income);

    private Animal animal(UUID farmId) {
        var animal = Animal.register(new RegisterAnimalCommand("A-1", null, "Holstein", null,
                TODAY.minusYears(2), null, null, farmId), TODAY);
        ReflectionTestUtils.setField(animal, "id", UUID.randomUUID());
        when(repository.findById(animal.getId())).thenReturn(Optional.of(animal));
        when(repository.save(animal)).thenReturn(animal);
        return animal;
    }

    private Farm farm(UUID ownerId) {
        var farm = Farm.register(new RegisterFarmCommand(ownerId, "Fundo", "Jauja", null));
        ReflectionTestUtils.setField(farm, "id", UUID.randomUUID());
        return farm;
    }

    @Test
    void saleMarksTheAnimalSoldRegistersTheIncomeForTheFarmOwnerAndNotifies() {
        var owner = UUID.randomUUID();
        var farm = farm(owner);
        var animal = animal(farm.getId());
        var incomeId = UUID.randomUUID();
        when(farms.findById(farm.getId())).thenReturn(Optional.of(farm));
        when(income.registerIncome(eq(owner), eq(new BigDecimal("900")), eq(TODAY), any())).thenReturn(incomeId);

        var sale = service.handle(new SellAnimalCommand(animal.getId(), null, new BigDecimal("900"), TODAY));

        assertEquals(AnimalStatus.SOLD, sale.animal().getStatus());
        assertEquals(incomeId, sale.incomeTransactionId());
        verify(income).registerIncome(owner, new BigDecimal("900"), TODAY, "Venta del animal A-1");
        verify(events).publishEvent(new AnimalLeftActiveHerdEvent(animal.getId(), AnimalStatus.SOLD));
    }

    @Test
    void theOwnerOfTheFarmWinsOverAnOwnerSentInTheRequest() {
        var owner = UUID.randomUUID();
        var farm = farm(owner);
        var animal = animal(farm.getId());
        when(farms.findById(farm.getId())).thenReturn(Optional.of(farm));

        service.handle(new SellAnimalCommand(animal.getId(), UUID.randomUUID(), BigDecimal.TEN, TODAY));

        verify(income).registerIncome(eq(owner), any(), any(), any());
    }

    @Test
    void anAnimalWithoutFarmUsesTheOwnerSentInTheRequest() {
        var animal = animal(null);
        var owner = UUID.randomUUID();

        service.handle(new SellAnimalCommand(animal.getId(), owner, BigDecimal.TEN, TODAY));

        verify(income).registerIncome(eq(owner), any(), any(), any());
    }

    @Test
    void anAnimalWithoutFarmAndWithoutOwnerCannotBeSoldBecauseTheIncomeHasNoOwner() {
        var animal = animal(null);

        var ex = assertThrows(InvalidAnimalSaleException.class,
                () -> service.handle(new SellAnimalCommand(animal.getId(), null, BigDecimal.TEN, TODAY)));

        assertEquals("El animal no tiene granja: indica el dueno (ownerId) para registrar el ingreso.", ex.getMessage());
        verifyNoInteractions(income);
        verifyNoInteractions(events);
    }

    @Test
    void aFarmThatNoLongerExistsAbortsTheSale() {
        var animal = animal(UUID.randomUUID());
        when(farms.findById(animal.getFarmId())).thenReturn(Optional.empty());

        assertThrows(FarmNotFoundException.class,
                () -> service.handle(new SellAnimalCommand(animal.getId(), null, BigDecimal.TEN, TODAY)));

        verifyNoInteractions(income);
    }

    @Test
    void invalidPriceOrDateNeverReachesFinanceNorTheRepository() {
        var animal = animal(null);
        var owner = UUID.randomUUID();

        assertThrows(InvalidAnimalSaleException.class,
                () -> service.handle(new SellAnimalCommand(animal.getId(), owner, BigDecimal.ZERO, TODAY)));
        assertThrows(InvalidAnimalSaleException.class,
                () -> service.handle(new SellAnimalCommand(animal.getId(), owner, BigDecimal.TEN, TODAY.plusDays(1))));

        verify(repository, never()).save(any());
        verifyNoInteractions(income);
        verifyNoInteractions(events);
    }

    @Test
    void aSoldOrDeactivatedAnimalCannotBeSoldAgain() {
        var animal = animal(null);
        animal.deactivate();

        assertThrows(AnimalNotActiveException.class,
                () -> service.handle(new SellAnimalCommand(animal.getId(), UUID.randomUUID(), BigDecimal.TEN, TODAY)));

        verifyNoInteractions(income);
    }

    @Test
    void sellingAnUnknownAnimalIsNotFound() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(AnimalNotFoundException.class,
                () -> service.handle(new SellAnimalCommand(id, UUID.randomUUID(), BigDecimal.TEN, TODAY)));
    }

    @Test
    void ifFinanceFailsTheExceptionPropagatesSoTheTransactionRollsBackTheSale() {
        var animal = animal(null);
        when(income.registerIncome(any(), any(), any(), any())).thenThrow(new IllegalArgumentException("finance down"));

        assertThrows(IllegalArgumentException.class,
                () -> service.handle(new SellAnimalCommand(animal.getId(), UUID.randomUUID(), BigDecimal.TEN, TODAY)));

        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void deactivationNotifiesOtherContextsSoTheyStopTreatingTheAnimalAsActive() {
        var animal = animal(null);

        service.handle(new DeactivateAnimalCommand(animal.getId()));

        verify(events).publishEvent(new AnimalLeftActiveHerdEvent(animal.getId(), AnimalStatus.INACTIVE));
    }

    @Test
    void aFailedDeactivationPublishesNothing() {
        var animal = animal(null);
        animal.deactivate();

        assertThrows(AnimalNotActiveException.class, () -> service.handle(new DeactivateAnimalCommand(animal.getId())));

        verifyNoInteractions(events);
    }

    @Test
    void anInactiveAnimalCannotBeEditedThroughTheService() {
        var animal = animal(null);
        animal.deactivate();

        assertThrows(AnimalNotActiveException.class, () -> service.handle(new UpdateAnimalCommand(animal.getId(),
                null, "Jersey", null, TODAY.minusYears(1), null, null, null)));

        verify(repository, never()).save(any());
    }
}
