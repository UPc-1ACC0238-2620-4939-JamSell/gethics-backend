package com.jamsell.gethics.shared.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TS09, Escenario 1: la aplicacion arranca con Flyway activo, aplica V1 y V2 en orden y el modulo veterinario
 * funciona sobre el esquema resultante. H2 en memoria.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flyway_startup;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class FlywayStartupTest {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    MockMvc mvc;

    @Test
    void bothMigrationsWereAppliedInOrderAtStartup() {
        var versions = jdbc.queryForList(
                "select \"version\" from \"flyway_schema_history\" where \"success\" = true and \"version\" is not null order by \"installed_rank\"",
                String.class);

        assertEquals(List.of("1", "2"), versions);
    }

    @Test
    void aClientCanBeAssignedOnTheMigratedSchema() throws Exception {
        var body = "{\"veterinarianId\":\"" + UUID.randomUUID() + "\",\"clientId\":\"" + UUID.randomUUID() + "\"}";

        mvc.perform(post("/api/v1/vet/assignments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }
}
