package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.payload.HistoryOrderResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.text.DecimalFormat;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String from;

    @Value("${app.email.subject}")
    private String subject;

    public void sendOrderConfirmationEmail(HistoryOrderResponse order) {
        logger.info("Preparing to send order confirmation email for order ID: {}", order.getOrderId());

        try {
            // Create a new MimeMessage
            MimeMessage message = emailSender.createMimeMessage();

            // Use MimeMessageHelper for multipart message capabilities (HTML + attachments if needed)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email metadata
            helper.setFrom(from);
            helper.setTo(order.getEmail());
            helper.setSubject(subject + " - Order " + order.getOrderId());

            // Prepare the Thymeleaf template context
            Context context = new Context();

            // Format currency for display
            DecimalFormat df = new DecimalFormat("#0.00");

            // Add order details to the context
            context.setVariable("order", order);

            // Process the HTML template with Thymeleaf
            String htmlContent = templateEngine.process("order-confirmation", context);

            // Set the email content as HTML
            helper.setText(htmlContent, true);

            // Send the email
            logger.info("Sending order confirmation email to: {}", order.getEmail());
            emailSender.send(message);
            logger.info("Email sent successfully");

        } catch (MailException | MessagingException e) {
            logger.error("Failed to send order confirmation email: {}", e.getMessage());
//            throw e;
        }
    }

    public void sendPasswordResetEmail(String to, String resetUrl){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText("Click the link to reset your password: " + resetUrl);
        emailSender.send(message);
    }

}
