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
import jakarta.mail.Store;
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

    try {
      // Gửi thư qua SMTP trước
      Transport transport = session.getTransport("smtp");
      transport.connect();
      transport.sendMessage(message, message.getAllRecipients());
      transport.close();
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

      // Copy vào thư mục Sent sau khi gửi thành công (không throw nếu lỗi)
      mailService.copyIntoSent(
              message,
              sessionService.getReceiveMailSession(username),
              username,
              password
      );

    } catch (MessagingException e) {
      log.error("Failed to send email: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public boolean isAuthenticatedWithHMailServer(String username, String password) {
    log.info("(isAuthenticatedWithHMailServer) start: {}", username);

    String fullEmail = username;
    if (!fullEmail.contains("@")) {
      fullEmail = username + "@domain1.com";
    }

    // 1. Thử xác thực qua IMAP (port 143)
    try {
      Session receiveSession = sessionService.getReceiveMailSession(fullEmail);
      String mailStoreType = fullEmail.contains("gmail") ? "imaps" : "imap";
      Store store = receiveSession.getStore(mailStoreType);
      store.connect("127.0.0.1", 143, fullEmail, password);
      store.close();
      log.info("(isAuthenticatedWithHMailServer) IMAP auth success for: {}", fullEmail);
      return true;
    } catch (Exception e) {
      log.warn("IMAP auth failed for {}: {}", fullEmail, e.getMessage());
    }

    // 2. Thử xác thực qua SMTP (port 25 hoặc 587)
    try {
      Session sendSession = sessionService.getSendMailSession(fullEmail, password);
      Transport transport = sendSession.getTransport("smtp");
      try {
        transport.connect("127.0.0.1", 25, fullEmail, password);
      } catch (Exception e1) {
        transport.connect("127.0.0.1", 587, fullEmail, password);
      }
      transport.close();
      log.info("(isAuthenticatedWithHMailServer) SMTP auth success for: {}", fullEmail);
      return true;
    } catch (Exception e) {
      log.warn("SMTP auth failed for {}: {}", fullEmail, e.getMessage());
    }

    return false;
  }

  public List<Email> listInboxMail(String username, String password) {
    log.info("(listInboxMail) username: {}", username);

    Session session = sessionService.getReceiveMailSession(username);
    String mailStoreType = username.contains("gmail") ? "imaps" : "imap";

    List<Email> emails = new ArrayList<>();
    try {
      List<Email> rawEmails = mailService.getInBoxMail(username, password, mailStoreType, session);
      if (rawEmails != null && !rawEmails.isEmpty()) {
        emails.addAll(rawEmails.stream()
                .filter(email -> !spamFilterService.isSpam(email))
                .collect(Collectors.toList()));
      }
    } catch (Exception e) {
      log.warn("IMAP getInbox failed or empty: {}", e.getMessage());
    }

    // Nếu IMAP chưa có thư (hòm thư mới), nạp thêm các thư mẫu từ MySQL Database
    if (emails.isEmpty()) {
      List<EmailLog> dbLogs = emailLogRepository.findByRecipientAndDeletedFalseOrderByCreatedAtDesc(username);
      if (dbLogs.isEmpty()) {
        dbLogs = emailLogRepository.findByMailTypeOrderByCreatedAtDesc(MailType.INBOX);
      }
      for (EmailLog dbLog : dbLogs) {
        if (!dbLog.isSpam() && !dbLog.isDeleted()) {
          emails.add(convertLogToEmail(dbLog));
        }
      }
    }

    return emails;
  }

  public List<Email> listSentMail(String username, String password) {
    log.info("(listSentMail) username: {}", username);

    Session session = sessionService.getReceiveMailSession(username);
    String mailStoreType = username.contains("gmail") ? "imaps" : "imap";

    List<Email> emails = new ArrayList<>();
    try {
      List<Email> sent = mailService.getSentMail(username, password, mailStoreType, session);
      if (sent != null && !sent.isEmpty()) {
        emails.addAll(sent);
      }
    } catch (Exception e) {
      log.warn("IMAP getSent failed or empty: {}", e.getMessage());
    }

    // Nếu IMAP chưa có thư đã gửi, nạp từ bảng email_logs trong MySQL
    if (emails.isEmpty()) {
      List<EmailLog> dbLogs = emailLogRepository.findBySenderAndDeletedFalseOrderByCreatedAtDesc(username);
      if (dbLogs.isEmpty()) {
        dbLogs = emailLogRepository.findByMailTypeOrderByCreatedAtDesc(MailType.SENT);
      }
      for (EmailLog dbLog : dbLogs) {
        if (!dbLog.isDeleted()) {
          emails.add(convertLogToEmail(dbLog));
        }
      }
    }

    return emails;
  }

  public List<Email> listSpamMail(String username, String password) {
    log.info("(listSpamMail) username: {}", username);

    Session session = sessionService.getReceiveMailSession(username);
    String mailStoreType = username.contains("gmail") ? "imaps" : "imap";

    List<Email> allSpam = new ArrayList<>();
    try {
      // 1. Thư từ thư mục SPAM của Mail Server (nếu có)
      List<Email> serverSpam = mailService.getSpamMail(username, password, mailStoreType, session);
      if (serverSpam != null) {
        allSpam.addAll(serverSpam);
      }

      // 2. Thư từ INBOX nhưng vi phạm quy tắc lọc spam trong MySQL
      List<Email> rawInbox = mailService.getInBoxMail(username, password, mailStoreType, session);
      if (rawInbox != null) {
        List<Email> filteredSpam = rawInbox.stream()
                .filter(spamFilterService::isSpam)
                .collect(Collectors.toList());
        for (Email email : filteredSpam) {
          if (allSpam.stream().noneMatch(e -> e.getSubject().equals(email.getSubject()) && e.getFromAddress().equals(email.getFromAddress()))) {
            allSpam.add(email);
          }
        }
      }
    } catch (Exception e) {
      log.warn("IMAP listSpam failed: {}", e.getMessage());
    }

    // Nếu chưa có thư spam từ IMAP, nạp các thư spam mẫu từ MySQL Database
    if (allSpam.isEmpty()) {
      List<EmailLog> dbLogs = emailLogRepository.findByMailTypeOrderByCreatedAtDesc(MailType.SPAM);
      for (EmailLog dbLog : dbLogs) {
        if (!dbLog.isDeleted()) {
          allSpam.add(convertLogToEmail(dbLog));
        }
      }
    }

    return allSpam;
  }

  public Email convertLogToEmail(EmailLog log) {
    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    String dateStr = log.getCreatedAt() != null ? log.getCreatedAt().format(formatter) : "";
    return Email.builder()
            .id(log.getId() != null ? String.valueOf(log.getId()) : (log.getMessageUuid() != null ? log.getMessageUuid() : java.util.UUID.randomUUID().toString()))
            .fromAddress(log.getSender())
            .toAddress(log.getRecipient())
            .subject(log.getSubject() != null ? log.getSubject() : "(Không có tiêu đề)")
            .content(log.getContent() != null ? log.getContent() : "")
            .date(dateStr)
            .isReplying(false)
            .dbId(log.getId())
            .read(log.isRead())
            .deleted(log.isDeleted())
            .build();
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
