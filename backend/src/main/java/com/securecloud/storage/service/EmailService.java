package com.securecloud.storage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendShareLinkEmail(String recipientEmail, String fileName, String ownerEmail, String shareLink) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required");
        }

        if (fromAddress == null || fromAddress.isBlank()) {
        return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject("File shared with you: " + fileName);

            String htmlBody = """
                <html>
                  <body style=\"font-family: Arial, sans-serif; color: #1f2937;\">
                    <h2 style=\"margin-bottom: 8px;\">A file has been shared with you</h2>
                    <p><strong>File:</strong> %s</p>
                    <p><strong>Shared by:</strong> %s</p>
                    <p style=\"margin: 20px 0;\">
                      <a href=\"%s\" style=\"background:#0ea5e9;color:#ffffff;padding:10px 16px;border-radius:8px;text-decoration:none;font-weight:600;\">
                        Download File
                      </a>
                    </p>
                    <p style=\"font-size: 12px; color:#6b7280;\">If the button does not work, copy and paste this link:</p>
                    <p style=\"font-size: 12px; color:#2563eb; word-break: break-all;\">%s</p>
                  </body>
                </html>
                """.formatted(fileName, ownerEmail, shareLink, shareLink);

            helper.setText(htmlBody, true);
            mailSender.send(message);
      return true;
        } catch (Exception ex) {
      return false;
        }
    }
}
