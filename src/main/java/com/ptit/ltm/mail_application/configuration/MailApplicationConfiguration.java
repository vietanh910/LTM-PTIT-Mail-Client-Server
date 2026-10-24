package com.ptit.ltm.mail_application.configuration;

import com.ptit.ltm.mail_application.dto.SessionMail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailApplicationConfiguration {

    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }

    public Properties sendMailProperties(SessionMail sessionMail) {
        Properties props = new Properties();
        String user = sessionMail.getUsername() != null ? sessionMail.getUsername().toLowerCase() : "";
        if (user.endsWith("@gmail.com")) {
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.ssl.enable", true);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", "465");
        } else if (user.endsWith("@ptit.edu.vn") || user.endsWith("@stu.ptit.edu.vn")) {
            props.put("mail.smtp.host", "smtp.office365.com");
            props.put("mail.smtp.socketFactory.port", "587");
            props.put("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", "587");
        } else {
            // hMailServer local - dùng cổng 25 (SMTP thuần, không SSL)
            props.put("mail.smtp.host", "127.0.0.1");
            props.put("mail.smtp.port", "25");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.ssl.trust", "127.0.0.1");
            props.put("mail.smtp.connectiontimeout", "30000");
            props.put("mail.smtp.timeout", "60000");
            props.put("mail.smtp.writetimeout", "60000");
        }
        return props;
    }

    public Properties receiveMailProperties(SessionMail sessionMail) {
        Properties properties = new Properties();
        String user = sessionMail.getUsername() != null ? sessionMail.getUsername().toLowerCase() : "";

        if (user.endsWith("@gmail.com")) {
            properties.put("mail.imap.host", "imap.gmail.com");
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imap.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
        } else if (user.endsWith("@ptit.edu.vn") || user.endsWith("@stu.ptit.edu.vn")) {
            properties.put("mail.imap.host", "outlook.office365.com");
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imap.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.imap.ssl.enable", "true");
        } else {
            properties.put("mail.imap.host", "127.0.0.1");
        }
        properties.put("mail.imap.connectiontimeout", "30000");
        properties.put("mail.imap.timeout", "60000");
        return properties;
    }

    public Properties sendGMailConfig() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "127.0.0.1");
        props.put("mail.smtp.port", "587");
        return props;
    }
}
