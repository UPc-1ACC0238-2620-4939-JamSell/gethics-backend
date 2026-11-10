package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotActiveException;
import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.AnimalQrNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalSaleException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.SellAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalByQrCodeQuery;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalSale;
import com.jamsell.gethics.livestock.domain.services.AnimalCommandService;
import com.jamsell.gethics.livestock.domain.services.AnimalQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-29 (QR) y US-30 (venta) por HTTP: codigos de estado y mensajes que ve la app movil. */
@WebMvcTest(AnimalController.class)
@WithMockUser
class AnimalSaleAndQrControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    AnimalCommandService commandService;
    @MockitoBean
    AnimalQueryService queryService;

    private final UUID animalId = UUID.randomUUID();

    private Animal animal() {
        var animal = Animal.register(new RegisterAnimalCommand("A-1", "Lola", "Holstein", null,
                LocalDate.of(2024, 1, 1), null, null, null), LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(animal, "id", animalId);
        return animal;
    }

    private org.springframework.test.web.servlet.ResultActions sale(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/animals/" + animalId + "/sale").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // ---- US-29 ----

    @Test
    void qrScanReturnsTheAnimal() throws Exception {
        var animal = animal();
        when(queryService.handle(new GetAnimalByQrCodeQuery(animal.getQrCode()))).thenReturn(animal);

        mockMvc.perform(get("/api/v1/animals/qr/" + animal.getQrCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(animalId.toString()))
                .andExpect(jsonPath("$.qrCode").value(animal.getQrCode()));
    }

    @Test
    void qrScanOfAnUnknownCodeIs404WithTheNotFoundMessage() throws Exception {
        when(queryService.handle(any(GetAnimalByQrCodeQuery.class))).thenThrow(new AnimalQrNotFoundException());

        mockMvc.perform(get("/api/v1/animals/qr/GTH-NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No se encontro el registro: el codigo QR no corresponde a ningun animal."));
    }

    // ---- US-30 ----

    @Test
    void escenario1SaleIs201WithTheAnimalSoldAndTheIncomeId() throws Exception {
        var animal = animal();
        animal.sell(new BigDecimal("950.00"), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1));
        var incomeId = UUID.randomUUID();
        when(commandService.handle(new SellAnimalCommand(animalId, null, new BigDecimal("950.00"),
                LocalDate.of(2026, 6, 1)))).thenReturn(new AnimalSale(animal, incomeId));

        sale("{\"price\":950.00,\"saleDate\":\"2026-06-01\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.status").value("SOLD"))
                .andExpect(jsonPath("$.price").value(950.00))
                .andExpect(jsonPath("$.saleDate").value("2026-06-01"))
                .andExpect(jsonPath("$.incomeTransactionId").value(incomeId.toString()));
    }

    @Test
    void escenario2ZeroOrNegativePriceIsA400ValidationErrorWithoutCallingTheService() throws Exception {
        sale("{\"price\":0,\"saleDate\":\"2026-06-01\"}").andExpect(status().isBadRequest());
        sale("{\"price\":-5,\"saleDate\":\"2026-06-01\"}").andExpect(status().isBadRequest());

        verifyNoInteractions(commandService);
    }

    @Test
    void missingPriceOrDateIs400() throws Exception {
        sale("{\"saleDate\":\"2026-06-01\"}").andExpect(status().isBadRequest());
        sale("{\"price\":10}").andExpect(status().isBadRequest());

        verifyNoInteractions(commandService);
    }

    @Test
    void domainSaleErrorsAreMappedTo400() throws Exception {
        when(commandService.handle(any(SellAnimalCommand.class)))
                .thenThrow(new InvalidAnimalSaleException("La fecha de venta no puede ser futura."));

        sale("{\"price\":10,\"saleDate\":\"2099-01-01\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha de venta no puede ser futura."));
    }

    @Test
    void sellingAnAnimalThatIsNotActiveIs409() throws Exception {
        when(commandService.handle(any(SellAnimalCommand.class)))
                .thenThrow(new AnimalNotActiveException(com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus.SOLD));

        sale("{\"price\":10,\"saleDate\":\"2026-06-01\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El animal ya fue vendido."));
    }

    @Test
    void sellingAnUnknownAnimalIs404() throws Exception {
        when(commandService.handle(any(SellAnimalCommand.class))).thenThrow(new AnimalNotFoundException(animalId));

        sale("{\"price\":10,\"saleDate\":\"2026-06-01\"}").andExpect(status().isNotFound());
        verify(commandService).handle(any(SellAnimalCommand.class));
    }
}
