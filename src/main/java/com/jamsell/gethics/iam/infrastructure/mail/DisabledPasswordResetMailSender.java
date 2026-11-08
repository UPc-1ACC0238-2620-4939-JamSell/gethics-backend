package com.jamsell.gethics.iam.infrastructure.mail;

import com.jamsell.gethics.iam.application.internal.outboundservices.mail.PasswordResetMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Fallback fuera de dev/test cuando el SMTP esta deshabilitado (app.mail.enabled=false o ausente): no envia nada y,
 * a diferencia de LoggingPasswordResetMailSender, NO registra el enlace (lleva el token de restablecimiento) ni el correo.
 */
@Slf4j
@Service
@Profile("!dev & !test")
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledPasswordResetMailSender implements PasswordResetMailSender {

    @Override
    public void sendPasswordResetLink(String toEmail, String userName, String resetLink, long expiresInMinutes) {
        log.warn("[MAIL DESACTIVADO] No se envio el correo de restablecimiento de contrasena: SMTP deshabilitado "
                + "(MAIL_ENABLED=false).");
    }
}
