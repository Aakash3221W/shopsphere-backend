package com.shopsphere.authservice.service;

import com.shopsphere.authservice.entity.User;
import com.shopsphere.common.exception.ServiceUnavailableException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${shopsphere.mail.from-email}")
    private String fromEmail;

    @Value("${shopsphere.mail.from-name:ShopSphere}")
    private String fromName;

    public void sendWelcomeEmail(User user) {
        sendHtmlEmail(
                user.getEmail(),
                "Welcome to ShopSphere",
                """
                <html>
                  <body style="font-family:Segoe UI,Arial,sans-serif;background:#f8fafc;padding:24px;">
                    <div style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
                      <h1 style="margin:0 0 12px;color:#0f172a;">Welcome to ShopSphere, %s!</h1>
                      <p style="margin:0 0 16px;color:#475569;line-height:1.6;">
                        Your account has been created successfully. You can now explore products, manage orders, and enjoy the full ShopSphere experience.
                      </p>
                      <p style="margin:0;color:#475569;line-height:1.6;">
                        If you did not create this account, please contact our support team immediately.
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(escapeHtml(user.getName()))
        );
    }

    public void sendPasswordResetOtp(String email, String otp) {
        sendHtmlEmail(
                email,
                "Your ShopSphere password reset OTP",
                """
                <html>
                  <body style="font-family:Segoe UI,Arial,sans-serif;background:#f8fafc;padding:24px;">
                    <div style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
                      <h1 style="margin:0 0 12px;color:#0f172a;">Password reset request</h1>
                      <p style="margin:0 0 20px;color:#475569;line-height:1.6;">
                        Use the OTP below to reset your ShopSphere password. This code will expire in 5 minutes.
                      </p>
                      <div style="font-size:32px;font-weight:700;letter-spacing:8px;color:#1d4ed8;margin:0 0 20px;">%s</div>
                      <p style="margin:0;color:#475569;line-height:1.6;">
                        If you did not request this, you can safely ignore this email.
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(otp)
        );
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
            log.error("Failed to send email to {}", to, ex);
            throw new ServiceUnavailableException(
                    "Email service is temporarily unavailable. Please try again.");
        }
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
