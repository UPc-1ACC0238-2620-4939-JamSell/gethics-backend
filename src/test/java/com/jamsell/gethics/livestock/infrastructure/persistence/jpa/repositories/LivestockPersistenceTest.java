package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.application.internal.commandservices.AnimalFarmCommandServiceImpl;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuracion unica de los tests de persistencia de livestock contra el PostgreSQL de application-dev.yaml.
 * Spring cachea un contexto por combinacion distinta de {@code @Import}, y cada contexto abre su propio pool de
 * conexiones: si cada test importara solo lo suyo, el conjunto de tests superaria el limite de conexiones de
 * PostgreSQL y los ultimos contextos fallarian al arrancar. Compartir esta anotacion mantiene un solo contexto.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AnimalRepositoryImpl.class, AnimalQueryRepositoryImpl.class, FarmRepositoryImpl.class,
        FarmQueryRepositoryImpl.class, AnimalFarmAssignmentRepositoryImpl.class, AnimalFarmCommandServiceImpl.class})
@interface LivestockPersistenceTest {
}
