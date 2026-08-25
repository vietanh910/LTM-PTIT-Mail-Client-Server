package com.ptit.ltm.mail_application.facade.impl;

import com.ptit.ltm.mail_application.dto.SendMailRequest;
import com.ptit.ltm.mail_application.model.Email;
import com.ptit.ltm.mail_application.service.impl.MailServiceImpl;
import com.ptit.ltm.mail_application.service.impl.SessionServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailFacadeServiceImpl {
  private final SessionServiceImpl sessionService;
  private final MailServiceImpl mailService;

  public void sendMail(SendMailRequest request, MultipartFile file, String username, String password) {
    log.info("(sendMail) start");

    Session session = sessionService.getSendMailSession(username, password);

    Email email = Email.builder()
          .fromAddress(username)
          .toAddress(request.getToAddress())
          .subject(request.getSubject())
          .content(request.getContent())
          .build();

    Message message = mailService.getSendMailMessage(
          email,
          file,
          session
    );

    mailService.copyIntoSent(
          message,
          sessionService.getReceiveMailSession(username),
          username,
          password
    );

    try {
      Transport.send(message);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isAuthenticatedWithHMailServer(String username, String password) {
    log.info("(isAuthenticatedWithHMailServer) start");

    Session session = sessionService.getSendMailSession(username, password);

    try {
      Transport transport = session.getTransport("smtp");
      transport.connect();
      transport.close();

      return true;
    } catch (Exception e) {
      log.error(e.getMessage());

      return false;
    }
  }

  public List<Email> listInboxMail(String username, String password) {
    log.info("(listInboxMail - lay ra thu nhan duoc) username: {}", username);

    Session session = sessionService.getReceiveMailSession(username);
    String mailStoreType = username.contains("gmail") ? "imaps" : "imap";

    return mailService.getInBoxMail(
          username,
          password,
          mailStoreType,
          session
    );
  }

  public List<Email> listSentMail(String username, String password) {
    log.info("(listSentMail - lay ra thu da gui) username: {}", username);

    Session session = sessionService.getReceiveMailSession(username);
    String mailStoreType = username.contains("gmail") ? "imaps" : "imap";

    return mailService.getSentMail(
          username,
          password,
          mailStoreType,
          session
    );
  }

  public List<Email> listSpamMail(String username, String password) {
    log.info("(listSpamMail - lay ra thu spam) username: {}", username);

    Session session = sessionService.getReceiveMailSession(username);
    String mailStoreType = username.contains("gmail") ? "imaps" : "imap";

    return mailService.getSpamMail(
          username,
          password,
          mailStoreType,
          session
    );
  }

  public List<Email> fakeData() {
    List<Email> emails = new ArrayList<>();

    Email email1 = Email.builder()
          .fromAddress("from")
          .toAddress("to")
          .subject("subject")
          .content("content")
          .date("date")
          .build();

    Email email2 = Email.builder()
          .fromAddress("from2")
          .toAddress("to2")
          .subject("subject2")
          .content("content2")
          .date("date2")
          .build();

    emails.add(email1);
    emails.add(email2);

    return emails;

  }
}
