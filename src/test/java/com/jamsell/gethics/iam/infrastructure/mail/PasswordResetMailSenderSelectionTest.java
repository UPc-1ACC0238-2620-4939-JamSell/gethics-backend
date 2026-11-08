package com.jamsell.gethics.iam.infrastructure.mail;

import com.jamsell.gethics.iam.application.internal.outboundservices.mail.PasswordResetMailSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.*;

/** En cada combinacion de profile y app.mail.enabled debe existir exactamente un PasswordResetMailSender. */
class PasswordResetMailSenderSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(
                    SmtpPasswordResetMailSender.class,
                    LoggingPasswordResetMailSender.class,
                    DisabledPasswordResetMailSender.class)
            .withBean(JavaMailSender.class, JavaMailSenderImpl::new)
            .withPropertyValues("app.mail.from=no-reply@test.local");

    private void assertSelected(Class<?> expected, String... properties) {
        runner.withPropertyValues(properties).run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(1, context.getBeansOfType(PasswordResetMailSender.class).size());
            assertInstanceOf(expected, context.getBean(PasswordResetMailSender.class));
        });
    }

    @Test
    void prodWithoutMailEnabledUsesTheDisabledFallback() {
        assertSelected(DisabledPasswordResetMailSender.class, "spring.profiles.active=prod");
    }

    @Test
    void prodWithMailDisabledUsesTheDisabledFallback() {
        assertSelected(DisabledPasswordResetMailSender.class, "spring.profiles.active=prod", "app.mail.enabled=false");
    }

    @Test
    void devWithMailDisabledKeepsTheLoggingSender() {
        assertSelected(LoggingPasswordResetMailSender.class, "spring.profiles.active=dev", "app.mail.enabled=false");
    }

    @Test
    void mailEnabledUsesSmtpInProdAndDev() {
        assertSelected(SmtpPasswordResetMailSender.class, "spring.profiles.active=prod", "app.mail.enabled=true");
        assertSelected(SmtpPasswordResetMailSender.class, "spring.profiles.active=dev", "app.mail.enabled=true");
    }
}
