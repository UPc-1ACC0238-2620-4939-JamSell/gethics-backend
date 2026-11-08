package com.jamsell.gethics.iam.infrastructure.mail;

import com.jamsell.gethics.iam.application.internal.outboundservices.mail.PasswordResetMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpPasswordResetMailSender implements PasswordResetMailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpPasswordResetMailSender(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendPasswordResetLink(String toEmail, String userName, String resetLink, long expiresInMinutes) {
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Restablece tu contraseña de Gethics");
        message.setText("""
                Hola %s,

                Recibimos una solicitud para restablecer tu contraseña de Gethics.
                Ingresa al siguiente enlace para crear una nueva (vence en %d minutos):

                %s

                Si no solicitaste este cambio, puedes ignorar este correo.
                """.formatted(userName, expiresInMinutes, resetLink));
        mailSender.send(message);
    }
}
