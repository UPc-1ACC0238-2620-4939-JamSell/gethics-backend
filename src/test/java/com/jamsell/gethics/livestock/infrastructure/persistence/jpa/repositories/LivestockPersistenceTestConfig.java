package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * Los tests @DataJpaTest no cargan las @Configuration de la aplicacion (como SanitaryReminderConfiguration, que define
 * el Clock real), y AnimalFarmCommandServiceImpl necesita uno para fechar los cambios de granja.
 */
@TestConfiguration(proxyBeanMethods = false)
class LivestockPersistenceTestConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
