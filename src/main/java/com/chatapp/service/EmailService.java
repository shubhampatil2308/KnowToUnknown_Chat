package com.chatapp.service;

import com.chatapp.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async("emailExecutor")
    public void sendRegistrationEmail(User user) {
        if (user == null) {
            System.out.println("ERROR: Cannot send registration email - User is null");
            return;
        }
        
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            System.out.println("ERROR: Cannot send registration email - Email not provided for user: " + user.getUsername());
            return;
        }

        try {
            System.out.println("INFO: Attempting to send registration email to: " + user.getEmail());
            
            String subject = "Welcome to ChatApp - Registration Successful!";
            String text = "Hi " + user.getName() + ",\n\n"
                    + "Thank you for registering with ChatApp!\n\n"
                    + "Your account has been successfully created.\n"
                    + "Username: " + user.getUsername() + "\n"
                    + "Email: " + user.getEmail() + "\n\n"
                    + "You can now login to ChatApp using your username and password.\n"
                    + "Start chatting with your friends and join groups to connect with others!\n\n"
                    + "If you did not create this account, please ignore this email.\n\n"
                    + "Happy chatting!\n"
                    + "Best regards,\n"
                    + "ChatApp Team";

            sendSimpleMail(user.getEmail(), subject, text);
            System.out.println("SUCCESS: Registration email sent successfully to: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("ERROR: Failed to send registration email to: " + user.getEmail());
            System.err.println("ERROR Details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async("emailExecutor")
    public void sendLoginEmail(User user) {
        if (user == null) {
            System.out.println("ERROR: Cannot send login email - User is null");
            return;
        }
        
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            System.out.println("ERROR: Cannot send login email - Email not provided for user: " + user.getUsername());
            return;
        }

        try {
            System.out.println("INFO: Attempting to send login email to: " + user.getEmail());
            
            String subject = "ChatApp - You Have Successfully Logged In";
            String text = "Hi " + user.getName() + ",\n\n"
                    + "You have successfully logged in to ChatApp.\n"
                    + "Login Time: " + java.time.LocalDateTime.now() + "\n\n"
                    + "If this wasn't you, please secure your account immediately by changing your password.\n\n"
                    + "Stay safe!\n"
                    + "Best regards,\n"
                    + "ChatApp Team";

            sendSimpleMail(user.getEmail(), subject, text);
            System.out.println("SUCCESS: Login email sent successfully to: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("ERROR: Failed to send login email to: " + user.getEmail());
            System.err.println("ERROR Details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendSimpleMail(String to, String subject, String text) {
        try {
            System.out.println("DEBUG: Preparing email to: " + to);
            
            if (fromEmail == null || fromEmail.isEmpty()) {
                System.err.println("ERROR: From email is not configured!");
                throw new RuntimeException("Email sender not configured");
            }
            
            if (mailSender == null) {
                System.err.println("ERROR: JavaMailSender is not initialized!");
                throw new RuntimeException("Mail sender not initialized");
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            System.out.println("DEBUG: Sending email from: " + fromEmail + " to: " + to);
            System.out.println("DEBUG: Email subject: " + subject);
            
            mailSender.send(message);
            System.out.println("SUCCESS: Email sent successfully from: " + fromEmail + " to: " + to);
        } catch (org.springframework.mail.MailAuthenticationException e) {
            System.err.println("ERROR: Email authentication failed. Check your email credentials in application.properties");
            System.err.println("ERROR Details: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Email authentication failed. Please check email configuration.", e);
        } catch (org.springframework.mail.MailSendException e) {
            System.err.println("ERROR: Failed to send email to: " + to);
            System.err.println("ERROR Details: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send email. Please check email configuration.", e);
        } catch (Exception e) {
            System.err.println("ERROR: Unexpected error sending email to: " + to);
            System.err.println("ERROR Details: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}



