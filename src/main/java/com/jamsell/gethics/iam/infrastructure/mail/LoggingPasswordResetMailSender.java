package com.jamsell.gethics.iam.infrastructure.mail;

import com.jamsell.gethics.iam.application.internal.outboundservices.mail.PasswordResetMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPasswordResetMailSender implements PasswordResetMailSender {

    @Override
    public void sendPasswordResetLink(String toEmail, String userName, String resetLink, long expiresInMinutes) {
        log.info("[MAIL DESACTIVADO] Enlace de restablecimiento para {} (vence en {} min): {}",
                toEmail, expiresInMinutes, resetLink);
    }
}
