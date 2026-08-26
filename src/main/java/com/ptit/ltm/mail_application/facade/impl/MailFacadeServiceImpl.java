package com.ptit.ltm.mail_application.facade.impl;

import com.ptit.ltm.mail_application.dto.SendMailRequest;
import com.ptit.ltm.mail_application.entity.EmailLog;
import com.ptit.ltm.mail_application.entity.MailType;
import com.ptit.ltm.mail_application.model.Email;
import com.ptit.ltm.mail_application.repository.EmailLogRepository;
import com.ptit.ltm.mail_application.service.SpamFilterService;
import com.ptit.ltm.mail_application.service.impl.MailServiceImpl;
import com.ptit.ltm.mail_application.service.impl.SessionServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailFacadeServiceImpl {
  private final SessionServiceImpl sessionService;
  private final MailServiceImpl mailService;
  private final SpamFilterService spamFilterService;
  private final EmailLogRepository emailLogRepository;

  public void sendMail(SendMailRequest request, MultipartFile file, String username, String password) {
    log.info("(sendMail) start: from={}, to={}", username, request.getToAddress());

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
      log.info("(sendMail) success: to={}", request.getToAddress());

      // Ghi log vào MySQL Database
      EmailLog logEntry = EmailLog.builder()
              .sender(username)
              .recipient(request.getToAddress())
              .subject(request.getSubject())
              .content(request.getContent())
              .mailType(MailType.SENT)
              .hasAttachment(file != null && !file.isEmpty())
              .attachmentName(file != null && !file.isEmpty() ? file.getOriginalFilename() : null)
              .build();
      emailLogRepository.save(logEntry);

    } catch (MessagingException e) {
      log.error("Failed to send email: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public boolean isAuthenticatedWithHMailServer(String username, String password) {
    log.info("(isAuthenticatedWithHMailServer) start: {}", username);

    Session session = sessionService.getSendMailSession(username, password);

    try {
      Transport transport = session.getTransport("smtp");
      transport.connect();
      transport.close();

      return true;
    } catch (Exception e) {
      log.warn("SMTP authentication check: {}", e.getMessage());
      return false;
    }
  }

  public List<Email> listInboxMail(String username, String password) {
    log.info("(listInboxMail) username: {}", username);

    Session session = sessionService.getReceiveMailSession(username);
    String mailStoreType = username.contains("gmail") ? "imaps" : "imap";

    List<Email> rawEmails = mailService.getInBoxMail(username, password, mailStoreType, session);
    
    // Tự động lọc ra những email không phải spam dựa trên rules trong MySQL
    return rawEmails.stream()
            .filter(email -> !spamFilterService.isSpam(email))
            .collect(Collectors.toList());
  }

  public List<Email> listSentMail(String username, String password) {
    log.info("(listSentMail) username: {}", username);

    Session session = sessionService.getReceiveMailSession(username);
    String mailStoreType = username.contains("gmail") ? "imaps" : "imap";

    return mailService.getSentMail(username, password, mailStoreType, session);
  }

  public List<Email> listSpamMail(String username, String password) {
    log.info("(listSpamMail) username: {}", username);

    Session session = sessionService.getReceiveMailSession(username);
    String mailStoreType = username.contains("gmail") ? "imaps" : "imap";

    // 1. Thư từ thư mục SPAM của Mail Server (nếu có)
    List<Email> serverSpam = mailService.getSpamMail(username, password, mailStoreType, session);

    // 2. Thư từ INBOX nhưng vi phạm quy tắc lọc spam trong MySQL
    List<Email> rawInbox = mailService.getInBoxMail(username, password, mailStoreType, session);
    List<Email> filteredSpam = rawInbox.stream()
            .filter(spamFilterService::isSpam)
            .collect(Collectors.toList());

    List<Email> allSpam = new ArrayList<>(serverSpam);
    for (Email email : filteredSpam) {
      if (allSpam.stream().noneMatch(e -> e.getSubject().equals(email.getSubject()) && e.getFromAddress().equals(email.getFromAddress()))) {
        allSpam.add(email);
      }
    }
    return allSpam;
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
