package com.jamsell.gethics.shared.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TS09 / H-03: las migraciones de Flyway (V1 SQL + V2 Java) dejan el esquema alineado con el modelo y nunca borran
 * datos. Usa H2 en memoria con una base distinta por escenario.
 */
class FlywayMigrationsTest {

    private static DriverManagerDataSource newDatabase() {
        var name = "flyway_" + UUID.randomUUID().toString().replace("-", "");
        return new DriverManagerDataSource("jdbc:h2:mem:" + name + ";MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1", "sa", "");
    }

    private static Flyway flyway(DriverManagerDataSource dataSource, boolean baselineExistingSchema) {
        var configuration = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration");
        if (baselineExistingSchema) {
            // Una base que ya existia antes de Flyway: se toma como version 1 y solo corren las migraciones nuevas.
            configuration.baselineOnMigrate(true).baselineVersion("1");
        }
        return configuration.load();
    }

    private static String idType(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject("select data_type from information_schema.columns "
                + "where lower(table_name) = ? and lower(column_name) = 'id'", String.class, table);
    }

    private static boolean tableExists(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject("select count(*) from information_schema.tables where lower(table_name) = ?",
                Integer.class, table) > 0;
    }

    private static boolean columnExists(JdbcTemplate jdbc, String table, String column) {
        return jdbc.queryForObject("select count(*) from information_schema.columns "
                + "where lower(table_name) = ? and lower(column_name) = ?", Integer.class, table, column) > 0;
    }

    @Test
    void aFreshDatabaseEndsWithTheVeterinaryAssignmentsTableOfTheModel() {
        var dataSource = newDatabase();

        var result = flyway(dataSource, false).migrate();

        var jdbc = new JdbcTemplate(dataSource);
        assertEquals(2, result.migrationsExecuted);
        assertEquals("UUID", idType(jdbc, "veterinary_assignments"));
        assertTrue(columnExists(jdbc, "veterinary_assignments", "assigned_at"));
        // La tabla antigua (BIGINT) se conserva con otro nombre, nunca se borra.
        assertTrue(tableExists(jdbc, "veterinary_assignments_legacy_bigint"));
        jdbc.update("insert into veterinary_assignments (id, veterinarian_id, client_id, assigned_at, status) "
                + "values (?, ?, ?, current_timestamp, 'ACTIVE')", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void legacyRowsAreKeptInTheRenamedTableAndTheNewTableStartsEmpty() {
        var dataSource = newDatabase();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table veterinary_assignments (id bigserial primary key, veterinarian_id bigint not null, "
                + "client_id bigint not null, status varchar(50) not null default 'ACTIVE')");
        jdbc.update("insert into veterinary_assignments (veterinarian_id, client_id) values (1, 2)");

        flyway(dataSource, true).migrate();

        assertEquals(1, jdbc.queryForObject("select count(*) from veterinary_assignments_legacy_bigint", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from veterinary_assignments", Integer.class));
        assertEquals("UUID", idType(jdbc, "veterinary_assignments"));
    }

    @Test
    void aTableThatAlreadyHasUuidIdsIsNotTouchedAndItsDataIsKept() {
        var dataSource = newDatabase();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table veterinary_assignments (id uuid primary key, veterinarian_id uuid not null, "
                + "client_id uuid not null, assigned_at timestamp with time zone not null, status varchar(255) not null)");
        jdbc.update("insert into veterinary_assignments values (?, ?, ?, current_timestamp, 'ACTIVE')",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        flyway(dataSource, true).migrate();

        assertEquals(1, jdbc.queryForObject("select count(*) from veterinary_assignments", Integer.class));
        assertFalse(tableExists(jdbc, "veterinary_assignments_legacy_bigint"));
    }

    @Test
    void anExistingAnimalsTableGetsTheSaleColumnsAndKeepsItsRows() {
        var dataSource = newDatabase();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table animals (id uuid primary key, tag varchar(50) not null)");
        jdbc.update("insert into animals values (?, 'A-1')", UUID.randomUUID());

        flyway(dataSource, true).migrate();

        assertTrue(columnExists(jdbc, "animals", "sale_price"));
        assertTrue(columnExists(jdbc, "animals", "sold_on"));
        assertEquals(1, jdbc.queryForObject("select count(*) from animals", Integer.class));
    }

    @Test
    void migratingTwiceChangesNothingTheSecondTime() {
        var dataSource = newDatabase();
        var flyway = flyway(dataSource, false);
        flyway.migrate();

        var second = flyway.migrate();

        assertEquals(0, second.migrationsExecuted);
    }
}
