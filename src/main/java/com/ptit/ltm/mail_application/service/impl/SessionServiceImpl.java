package com.ptit.ltm.mail_application.service.impl;

import com.ptit.ltm.mail_application.configuration.MailApplicationConfiguration;
import com.ptit.ltm.mail_application.dto.SessionMail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl {
    private final MailApplicationConfiguration mailApplicationConfiguration;

    public Session getSendMailSession(String username, String password) {
        log.info("(getSendMailSession) start");

        Properties properties = mailApplicationConfiguration.sendMailProperties(
                SessionMail.of(username)
        );

        return Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    public Session getReceiveMailSession(String username) {
        log.info("(getReceiveMailSession) start");

        Properties properties = mailApplicationConfiguration.receiveMailProperties(
                SessionMail.of(username)
        );

        return Session.getDefaultInstance(properties, null);
    }

}
