package com.jamsell.gethics.iam.application.internal.outboundservices.mail;

public interface PasswordResetMailSender {

    void sendPasswordResetLink(String toEmail, String userName, String resetLink, long expiresInMinutes);
}
