package com.jamsell.gethics.livestock;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Livestock + Veterinary + Sanitary + Finance de punta a punta con la aplicacion completa (REST, servicios, eventos,
 * JPA) sobre H2 en memoria: US-08, US-29, US-30, US-32, US-34 y TS07. Las propiedades fuerzan H2 para que NUNCA se
 * use otra base (el primer test lo comprueba).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:livestock_vet_it;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class LivestockVeterinaryIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Value("${spring.datasource.url}")
    String datasourceUrl;

    private final UUID owner = UUID.randomUUID();
    private final UUID vet = UUID.randomUUID();

    // ---- helpers -------------------------------------------------------------------------------------------

    private ResultActions json(MockHttpServletRequestBuilder builder, String body) throws Exception {
        return mvc.perform(builder.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String read(ResultActions actions, String path) throws Exception {
        return JsonPath.read(actions.andReturn().getResponse().getContentAsString(), path);
    }

    private UUID farm(UUID ownerId, String name, String location) throws Exception {
        var result = json(post("/api/v1/farms"), "{\"ownerId\":\"" + ownerId + "\",\"name\":\"" + name
                + "\",\"location\":\"" + location + "\"}").andExpect(status().isCreated());
        return UUID.fromString(read(result, "$.id"));
    }

    private UUID animal(String tag, UUID farmId) throws Exception {
        return UUID.fromString(read(animalRequest(tag, farmId).andExpect(status().isCreated()), "$.id"));
    }

    private ResultActions animalRequest(String tag, UUID farmId) throws Exception {
        var farmPart = farmId == null ? "" : ",\"farmId\":\"" + farmId + "\"";
        return json(post("/api/v1/animals"), "{\"tag\":\"" + tag + "\",\"name\":\"Lola\",\"breed\":\"Holstein\","
                + "\"birthDate\":\"2024-01-01\"" + farmPart + "}");
    }

    private UUID assign(UUID vetId, UUID clientId) throws Exception {
        var result = json(post("/api/v1/vet/assignments"),
                "{\"veterinarianId\":\"" + vetId + "\",\"clientId\":\"" + clientId + "\"}")
                .andExpect(status().isCreated());
        return UUID.fromString(read(result, "$.id"));
    }

    private String care(UUID requestId, UUID vetId, String diagnosis, String extra) {
        return "{\"clientRequestId\":\"" + requestId + "\",\"veterinarianId\":\"" + vetId + "\",\"diagnosis\":\""
                + diagnosis + "\",\"treatment\":\"Antibiotico\"" + extra + "}";
    }

    private String care(UUID requestId, UUID vetId, String diagnosis) {
        return care(requestId, vetId, diagnosis, "");
    }

    private ResultActions registerCare(UUID patient, String body) throws Exception {
        return json(post("/api/v1/vet/patients/" + patient + "/care"), body);
    }

    /** Un cliente con granja, un animal en ella y el veterinario ya asignado. */
    private UUID patientOfAnAssignedClient() throws Exception {
        var farmId = farm(owner, "Granja-" + tag(), "Piura");
        var patient = animal(tag(), farmId);
        assign(vet, owner);
        return patient;
    }

    private String tag() {
        return "T-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void scheduledEvent(UUID animalId, LocalDate date) throws Exception {
        json(post("/api/v1/animals/" + animalId + "/sanitary-events/scheduled"),
                "{\"type\":\"VACCINATION\",\"scheduledDate\":\"" + date + "\",\"description\":\"Aftosa\"}")
                .andExpect(status().isCreated());
    }

    private ResultActions calendar(LocalDate date) throws Exception {
        return mvc.perform(get("/api/v1/sanitary-calendar").param("year", "" + date.getYear())
                .param("month", "" + date.getMonthValue()));
    }

    // ---- seguridad del propio test -------------------------------------------------------------------------

    @Test
    void theTestsRunOnTheInMemoryDatabaseAndNeverOnTheSharedOne() {
        assertTrue(datasourceUrl.startsWith("jdbc:h2:mem:"), datasourceUrl);
    }

    // ---- US-08: baja logica --------------------------------------------------------------------------------

    @Test
    void deactivatedAnimalLeavesTheActiveListButKeepsItsRecord() throws Exception {
        var id = animal(tag(), null);

        mvc.perform(delete("/api/v1/animals/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/animals")).andExpect(status().isOk())
                .andExpect(jsonPath("$.animals[?(@.id=='" + id + "')]").isEmpty());
        mvc.perform(get("/api/v1/animals").param("status", "INACTIVE")).andExpect(status().isOk())
                .andExpect(jsonPath("$.animals[?(@.id=='" + id + "')]").isNotEmpty());
        mvc.perform(get("/api/v1/animals/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deactivatedAnimalKeepsItsClinicalHistory() throws Exception {
        var id = animal(tag(), null);
        json(post("/api/v1/animals/" + id + "/sanitary-events"), "{\"type\":\"VACCINATION\",\"occurredAt\":\""
                + LocalDateTime.now().minusDays(1).withNano(0) + "\",\"description\":\"Aftosa\"}")
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/v1/animals/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/animals/" + id + "/clinical-history")).andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1));
    }

    @Test
    void deactivatedAnimalKeepsItsFarmHistory() throws Exception {
        var farmId = farm(owner, "Norte-" + tag(), "Cajamarca");
        var id = animal(tag(), farmId);

        mvc.perform(delete("/api/v1/animals/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/animals/" + id + "/farm-history")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/farms/" + farmId + "/animals")).andExpect(status().isOk())
                .andExpect(jsonPath("$.animals[?(@.id=='" + id + "')]").isEmpty());
    }

    @Test
    void deactivatingTwiceIsAConflictAndAnUnknownAnimalIsNotFound() throws Exception {
        var id = animal(tag(), null);

        mvc.perform(delete("/api/v1/animals/" + id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/animals/" + id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El animal ya esta dado de baja."));
        mvc.perform(delete("/api/v1/animals/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void deactivationCancelsTheScheduledEventsSoTheyLeaveTheCalendarButStayInTheHistory() throws Exception {
        var id = animal(tag(), null);
        var date = LocalDate.now().plusDays(20);
        scheduledEvent(id, date);
        calendar(date).andExpect(status().isOk()).andExpect(content().string(containsString(id.toString())));

        mvc.perform(delete("/api/v1/animals/" + id)).andExpect(status().isNoContent());

        calendar(date).andExpect(status().isOk()).andExpect(content().string(not(containsString(id.toString()))));
        mvc.perform(get("/api/v1/animals/" + id + "/clinical-history"))
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].status").value("CANCELLED"));
    }

    @Test
    void aDeactivatedAnimalCannotBeEditedNorMovedToAnotherFarm() throws Exception {
        var farmA = farm(owner, "A-" + tag(), "Lima");
        var farmB = farm(owner, "B-" + tag(), "Lima");
        var id = animal(tag(), farmA);
        mvc.perform(delete("/api/v1/animals/" + id)).andExpect(status().isNoContent());

        json(put("/api/v1/animals/" + id), "{\"breed\":\"Jersey\",\"birthDate\":\"2024-01-01\"}")
                .andExpect(status().isConflict());
        json(put("/api/v1/animals/" + id + "/farm"), "{\"farmId\":\"" + farmB + "\"}").andExpect(status().isConflict());
    }

    // ---- US-29: QR -----------------------------------------------------------------------------------------

    @Test
    void qrCodeOfARegisteredAnimalFindsItEvenInLowercaseAndAfterDeactivation() throws Exception {
        var created = animalRequest(tag(), null).andExpect(status().isCreated());
        var id = read(created, "$.id");
        var qr = read(created, "$.qrCode");

        mvc.perform(get("/api/v1/animals/qr/" + qr)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/v1/animals/qr/" + qr.toLowerCase())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(delete("/api/v1/animals/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/animals/qr/" + qr)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void eachAnimalGetsAUniqueQr() throws Exception {
        var first = read(animalRequest(tag(), null).andExpect(status().isCreated()), "$.qrCode");
        var second = read(animalRequest(tag(), null).andExpect(status().isCreated()), "$.qrCode");

        assertTrue(!first.equals(second));
    }

    @Test
    void anUnknownQrIs404WithTheNotFoundMessage() throws Exception {
        mvc.perform(get("/api/v1/animals/qr/GTH-000000000000")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No se encontro el registro: el codigo QR no corresponde a ningun animal."));
    }

    // ---- US-30: venta --------------------------------------------------------------------------------------

    private ResultActions sale(UUID animalId, String body) throws Exception {
        return json(post("/api/v1/animals/" + animalId + "/sale"), body);
    }

    @Test
    void saleMarksTheAnimalSoldKeepsItsHistoryAndRegistersTheIncomeInFinance() throws Exception {
        var ownerId = UUID.randomUUID();
        var farmId = farm(ownerId, "Venta-" + tag(), "Ica");
        var id = animal(tag(), farmId);
        var today = LocalDate.now();

        var created = sale(id, "{\"price\":1200.50,\"saleDate\":\"" + today + "\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SOLD"))
                .andExpect(jsonPath("$.price").value(1200.50));
        var incomeId = read(created, "$.incomeTransactionId");

        mvc.perform(get("/api/v1/animals/" + id)).andExpect(jsonPath("$.status").value("SOLD"));
        mvc.perform(get("/api/v1/animals")).andExpect(jsonPath("$.animals[?(@.id=='" + id + "')]").isEmpty());
        mvc.perform(get("/api/v1/animals/" + id + "/clinical-history")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/finances").param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1200.50))
                .andExpect(jsonPath("$.balance").value(1200.50))
                .andExpect(jsonPath("$.transactions[0].id").value(incomeId));
    }

    @Test
    void anInvalidPriceIsA400AndNeitherTheAnimalNorTheBalanceChange() throws Exception {
        var ownerId = UUID.randomUUID();
        var id = animal(tag(), farm(ownerId, "Venta-" + tag(), "Ica"));
        var today = LocalDate.now();

        sale(id, "{\"price\":0,\"saleDate\":\"" + today + "\"}").andExpect(status().isBadRequest());
        sale(id, "{\"price\":-10,\"saleDate\":\"" + today + "\"}").andExpect(status().isBadRequest());
        sale(id, "{\"price\":10,\"saleDate\":\"" + today.plusDays(2) + "\"}").andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/animals/" + id)).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(get("/api/v1/finances").param("ownerId", ownerId.toString()))
                .andExpect(jsonPath("$.totalIncome").value(0));
    }

    @Test
    void anAnimalWithoutFarmNeedsTheOwnerToRegisterTheIncome() throws Exception {
        var id = animal(tag(), null);
        var today = LocalDate.now();

        sale(id, "{\"price\":10,\"saleDate\":\"" + today + "\"}").andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/animals/" + id)).andExpect(jsonPath("$.status").value("ACTIVE"));

        var ownerId = UUID.randomUUID();
        sale(id, "{\"price\":10,\"saleDate\":\"" + today + "\",\"ownerId\":\"" + ownerId + "\"}")
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/finances").param("ownerId", ownerId.toString()))
                .andExpect(jsonPath("$.totalIncome").value(10));
    }

    @Test
    void aSoldAnimalCannotBeSoldDeactivatedEditedOrAttendedAgainAndItsScheduledEventsAreCancelled() throws Exception {
        var ownerId = UUID.randomUUID();
        var id = animal(tag(), farm(ownerId, "Venta-" + tag(), "Ica"));
        var date = LocalDate.now().plusDays(25);
        scheduledEvent(id, date);
        sale(id, "{\"price\":10,\"saleDate\":\"" + LocalDate.now() + "\"}").andExpect(status().isCreated());

        sale(id, "{\"price\":10,\"saleDate\":\"" + LocalDate.now() + "\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El animal ya fue vendido."));
        mvc.perform(delete("/api/v1/animals/" + id)).andExpect(status().isConflict());
        json(put("/api/v1/animals/" + id), "{\"breed\":\"Jersey\",\"birthDate\":\"2024-01-01\"}")
                .andExpect(status().isConflict());
        calendar(date).andExpect(content().string(not(containsString(id.toString()))));
        mvc.perform(get("/api/v1/finances").param("ownerId", ownerId.toString()))
                .andExpect(jsonPath("$.totalIncome").value(10));
    }

    @Test
    void saleOfAnUnknownAnimalIs404() throws Exception {
        sale(UUID.randomUUID(), "{\"price\":10,\"saleDate\":\"" + LocalDate.now() + "\"}")
                .andExpect(status().isNotFound());
    }

    // ---- US-32: editar granja ------------------------------------------------------------------------------

    @Test
    void editingAFarmUpdatesItAndItIsListedWithTheNewData() throws Exception {
        var ownerId = UUID.randomUUID();
        var id = farm(ownerId, "Vieja-" + tag(), "Lima");

        json(put("/api/v1/farms/" + id), "{\"name\":\"Nueva\",\"location\":\"Cusco\",\"sizeHectares\":15.5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nueva"))
                .andExpect(jsonPath("$.location").value("Cusco"))
                .andExpect(jsonPath("$.sizeHectares").value(15.5))
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()));

        mvc.perform(get("/api/v1/farms").param("ownerId", ownerId.toString()))
                .andExpect(jsonPath("$.farms[0].name").value("Nueva"));
    }

    @Test
    void renamingToTheNameOfAnotherFarmOfTheSameOwnerIsRejectedButOtherOwnersMayReuseIt() throws Exception {
        var ownerId = UUID.randomUUID();
        var unique = "Dup-" + tag();
        farm(ownerId, unique, "Lima");
        var other = farm(ownerId, "Otra-" + tag(), "Lima");

        json(put("/api/v1/farms/" + other), "{\"name\":\"" + unique.toUpperCase() + "\",\"location\":\"Lima\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya tienes una granja llamada " + unique.toUpperCase() + "."));

        var someoneElse = farm(UUID.randomUUID(), "X-" + tag(), "Lima");
        json(put("/api/v1/farms/" + someoneElse), "{\"name\":\"" + unique + "\",\"location\":\"Lima\"}")
                .andExpect(status().isOk());
    }

    @Test
    void aFarmCanKeepItsOwnNameChangingOnlyTheCase() throws Exception {
        var name = "Casa-" + tag();
        var id = farm(UUID.randomUUID(), name, "Lima");

        json(put("/api/v1/farms/" + id), "{\"name\":\"" + name.toUpperCase() + "\",\"location\":\"Otra\"}")
                .andExpect(status().isOk());
    }

    @Test
    void editingAnUnknownFarmIs404AndInvalidDataIs400() throws Exception {
        json(put("/api/v1/farms/" + UUID.randomUUID()), "{\"name\":\"X\",\"location\":\"Y\"}")
                .andExpect(status().isNotFound());
        var id = farm(UUID.randomUUID(), "Val-" + tag(), "Lima");
        json(put("/api/v1/farms/" + id), "{\"name\":\"\",\"location\":\"Y\"}").andExpect(status().isBadRequest());
        json(put("/api/v1/farms/" + id), "{\"name\":\"X\",\"location\":\"Y\",\"sizeHectares\":0}")
                .andExpect(status().isBadRequest());
    }

    // ---- TS07: veterinario + animales + sanidad -----------------------------------------------------------

    @Test
    void vetSeesTheRealActiveAnimalsOfAnAssignedClient() throws Exception {
        var farmId = farm(owner, "Sur-" + tag(), "Arequipa");
        var active = animal(tag(), farmId);
        var removed = animal(tag(), farmId);
        mvc.perform(delete("/api/v1/animals/" + removed)).andExpect(status().isNoContent());
        assign(vet, owner);

        mvc.perform(get("/api/v1/vet/clients/" + owner + "/animals").param("veterinarianId", vet.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patients[?(@.patientId=='" + active + "')]").isNotEmpty())
                .andExpect(jsonPath("$.patients[?(@.patientId=='" + removed + "')]").isEmpty());
    }

    @Test
    void aClientWithoutAnimalsReturnsAnEmptyListWithAMessage() throws Exception {
        var client = UUID.randomUUID();
        assign(vet, client);

        mvc.perform(get("/api/v1/vet/clients/" + client + "/animals").param("veterinarianId", vet.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patients.length()").value(0))
                .andExpect(jsonPath("$.message").value("No animals registered"));
    }

    @Test
    void anUnassignedVetCannotSeeTheClientsAnimals() throws Exception {
        mvc.perform(get("/api/v1/vet/clients/" + owner + "/animals").param("veterinarianId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void theClientListShowsTheLocationOfTheFirstFarm() throws Exception {
        var client = UUID.randomUUID();
        var vetId = UUID.randomUUID();
        farm(client, "Zeta-" + tag(), "Tarma");
        assign(vetId, client);

        mvc.perform(get("/api/v1/vet/clients").param("veterinarianId", vetId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients[0].location").value("Tarma"));
    }

    @Test
    void registeredCareAppearsInTheOwnersClinicalHistoryAndInThePatientSummary() throws Exception {
        var patient = patientOfAnAssignedClient();

        registerCare(patient, care(UUID.randomUUID(), vet, "Mastitis"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.historySynced").value(true));

        mvc.perform(get("/api/v1/animals/" + patient + "/clinical-history")).andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].type").value("TREATMENT"));
        mvc.perform(get("/api/v1/vet/clients/" + owner + "/animals").param("veterinarianId", vet.toString()))
                .andExpect(jsonPath("$.patients[?(@.patientId=='" + patient + "')].totalClinicalEvents").value(1))
                .andExpect(jsonPath("$.patients[?(@.patientId=='" + patient + "')].lastEventType").value("TREATMENT"));
    }

    @Test
    void retryingTheSameCareDoesNotDuplicateTheHistoryAndADifferentPayloadIsAConflict() throws Exception {
        var patient = patientOfAnAssignedClient();
        var requestId = UUID.randomUUID();

        registerCare(patient, care(requestId, vet, "Cojera")).andExpect(status().isCreated());
        registerCare(patient, care(requestId, vet, "Cojera")).andExpect(status().isOk());
        registerCare(patient, care(requestId, vet, "Otra cosa")).andExpect(status().isConflict());

        mvc.perform(get("/api/v1/animals/" + patient + "/clinical-history"))
                .andExpect(jsonPath("$.events.length()").value(1));
    }

    @Test
    void careOnAnUnknownAnimalIs404AndCreatesNoHistory() throws Exception {
        var unknown = UUID.randomUUID();

        registerCare(unknown, care(UUID.randomUUID(), vet, "Mastitis")).andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/animals/" + unknown + "/clinical-history"))
                .andExpect(jsonPath("$.events.length()").value(0));
    }

    @Test
    void careByAVetNotAssignedToTheAnimalsClientIs403() throws Exception {
        var patient = patientOfAnAssignedClient();
        var intruder = UUID.randomUUID();

        registerCare(patient, care(UUID.randomUUID(), intruder, "Mastitis")).andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/animals/" + patient + "/clinical-history"))
                .andExpect(jsonPath("$.events.length()").value(0));
    }

    @Test
    void careOnADeactivatedOrSoldAnimalIs409() throws Exception {
        var patient = patientOfAnAssignedClient();
        mvc.perform(delete("/api/v1/animals/" + patient)).andExpect(status().isNoContent());

        registerCare(patient, care(UUID.randomUUID(), vet, "Mastitis")).andExpect(status().isConflict());
    }

    @Test
    void careOnAnAnimalWithoutFarmIs409BecauseItsClientIsUnknown() throws Exception {
        var patient = animal(tag(), null);

        registerCare(patient, care(UUID.randomUUID(), vet, "Mastitis")).andExpect(status().isConflict());
    }

    @Test
    void aNextControlBecomesACheckupInTheCalendarAndRetriesDoNotDuplicateIt() throws Exception {
        var patient = patientOfAnAssignedClient();
        var control = LocalDate.now().plusDays(12);
        var requestId = UUID.randomUUID();
        var body = care(requestId, vet, "Mastitis", ",\"nextControlDate\":\"" + control + "\"");

        registerCare(patient, body).andExpect(status().isCreated());
        registerCare(patient, body).andExpect(status().isOk());

        calendar(control).andExpect(status().isOk()).andExpect(content().string(containsString(patient.toString())));
        mvc.perform(get("/api/v1/animals/" + patient + "/clinical-history"))
                .andExpect(jsonPath("$.events.length()").value(2))
                .andExpect(jsonPath("$.events[?(@.status=='SCHEDULED')].type").value("CHECKUP"));
    }

    @Test
    void offlineBatchSyncReportsEachItemAndWritesEachCareOnce() throws Exception {
        var patient = patientOfAnAssignedClient();
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var item1 = "{\"patientId\":\"" + patient + "\",\"care\":" + care(first, vet, "Mastitis") + "}";
        var item2 = "{\"patientId\":\"" + patient + "\",\"care\":" + care(second, vet, "Cojera") + "}";
        var unknown = "{\"patientId\":\"" + UUID.randomUUID() + "\",\"care\":" + care(UUID.randomUUID(), vet, "X") + "}";
        var batch = "{\"items\":[" + item1 + "," + item2 + "," + item1 + "," + unknown + "]}";

        json(post("/api/v1/vet/care/sync"), batch).andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("CREATED"))
                .andExpect(jsonPath("$.results[1].status").value("CREATED"))
                .andExpect(jsonPath("$.results[2].status").value("DUPLICATE"))
                .andExpect(jsonPath("$.results[3].status").value("REJECTED"));

        mvc.perform(get("/api/v1/animals/" + patient + "/clinical-history"))
                .andExpect(jsonPath("$.events.length()").value(2));
    }

    // ---- US-34: desvincular --------------------------------------------------------------------------------

    @Test
    void unassigningRemovesTheClientFromTheActiveListAndKeepsTheClinicalHistory() throws Exception {
        var client = UUID.randomUUID();
        var vetId = UUID.randomUUID();
        var patient = animal(tag(), farm(client, "U-" + tag(), "Lima"));
        var assignmentId = assign(vetId, client);
        registerCare(patient, care(UUID.randomUUID(), vetId, "Mastitis")).andExpect(status().isCreated());

        mvc.perform(delete("/api/v1/vet/assignments/" + assignmentId)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/vet/clients").param("veterinarianId", vetId.toString()))
                .andExpect(jsonPath("$.clients.length()").value(0))
                .andExpect(jsonPath("$.message").value("No linked clients"));
        mvc.perform(get("/api/v1/vet/clients/" + client + "/animals").param("veterinarianId", vetId.toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/animals/" + patient + "/clinical-history"))
                .andExpect(jsonPath("$.events.length()").value(1));
    }

    @Test
    void unassigningAnUnknownOrAlreadyUnassignedClientIs404AndTheClientCanBeAssignedAgain() throws Exception {
        var client = UUID.randomUUID();
        var vetId = UUID.randomUUID();
        var assignmentId = assign(vetId, client);

        mvc.perform(delete("/api/v1/vet/assignments/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/vet/assignments/" + assignmentId)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/vet/assignments/" + assignmentId)).andExpect(status().isNotFound());

        assign(vetId, client);
        mvc.perform(get("/api/v1/vet/clients").param("veterinarianId", vetId.toString()))
                .andExpect(jsonPath("$.clients.length()").value(1));
    }

    @Test
    void assigningTheSameClientTwiceIsAConflictAndSelfAssignmentIsRejected() throws Exception {
        var client = UUID.randomUUID();
        var vetId = UUID.randomUUID();
        assign(vetId, client);

        json(post("/api/v1/vet/assignments"), "{\"veterinarianId\":\"" + vetId + "\",\"clientId\":\"" + client + "\"}")
                .andExpect(status().isConflict());
        json(post("/api/v1/vet/assignments"), "{\"veterinarianId\":\"" + vetId + "\",\"clientId\":\"" + vetId + "\"}")
                .andExpect(status().isBadRequest());
    }
}
