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
        if (sessionMail.getUsername().contains("gmail")) {
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.ssl.enable", true);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", "465");
        } else if (sessionMail.getUsername().contains("ptit")) {
            props.put("mail.smtp.host", "smtp.office365.com");
            props.put("mail.smtp.socketFactory.port", "587");
            props.put("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", "587");
        } else {
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", "127.0.0.1");
            props.put("mail.smtp.port", "587");
        }
        return props;
    }

    public Properties receiveMailProperties(SessionMail sessionMail) {
        Properties properties = new Properties();

        if (sessionMail.getUsername().contains("gmail")) {
            properties.put("mail.imap.host", "imap.gmail.com");
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imap.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
        } else if (sessionMail.getUsername().contains("ptit")) {
            properties.put("mail.imap.host", "outlook.office365.com");
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imap.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.imap.ssl.enable", "true");
        } else {
            properties.put("mail.imap.host", "127.0.0.1");
        }
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
