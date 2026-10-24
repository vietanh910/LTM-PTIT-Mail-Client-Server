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

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    String cleanTo = request.getToAddress() != null ? request.getToAddress().trim() : "";
    log.info("(sendMail) start: from={}, to={}", username, cleanTo);

    Session session = sessionService.getSendMailSession(username, password);

    Email email = Email.builder()
          .fromAddress(username)
          .toAddress(cleanTo)
          .subject(request.getSubject())
          .content(request.getContent())
          .build();

    Message message = mailService.getSendMailMessage(email, file, session);

    // Lưu file đính kèm lên đĩa
    String savedFileName = null;
    if (file != null && !file.isEmpty()) {
      long maxAllowed = 15L * 1024 * 1024; // 15 MB
      if (file.getSize() > maxAllowed) {
        throw new IllegalArgumentException("Dung lượng tệp đính kèm vượt quá giới hạn tối đa 15 MB! Vui lòng chọn tệp nhỏ hơn.");
      }
      try {
        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        // Đặt tên file: timestamp_originalName để tránh trùng
        String safeOriginal = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._\\-]", "_")
                : "attachment";
        savedFileName = System.currentTimeMillis() + "_" + safeOriginal;
        java.nio.file.Path targetPath = java.nio.file.Paths.get(uploadDir, savedFileName);
        java.nio.file.Files.write(targetPath, file.getBytes());
        log.info("(sendMail) attachment saved: {}", targetPath);
      } catch (Exception e) {
        log.warn("(sendMail) failed to save attachment: {}", e.getMessage());
      }
    }

    try {
      Transport transport = session.getTransport("smtp");
      transport.connect();
      transport.sendMessage(message, message.getAllRecipients());
      transport.close();
      log.info("(sendMail) success: to={}", cleanTo);

      // Ghi log vào MySQL Database (SENT)
      EmailLog logEntry = EmailLog.builder()
              .sender(username)
              .recipient(cleanTo)
              .subject(request.getSubject())
              .content(request.getContent())
              .mailType(MailType.SENT)
              .hasAttachment(savedFileName != null)
              .attachmentName(savedFileName)
              .read(true)
              .deleted(false)
              .build();
      emailLogRepository.save(logEntry);

      // Ghi log INBOX hoặc SPAM cho người nhận theo kết quả lọc spam
      try {
        Email recipientCheckMail = Email.builder()
                .fromAddress(username)
                .toAddress(cleanTo)
                .subject(request.getSubject())
                .content(request.getContent())
                .build();
        boolean isSpamMail = spamFilterService.isSpam(recipientCheckMail);

        EmailLog inboxEntry = EmailLog.builder()
                .sender(username)
                .recipient(cleanTo)
                .subject(request.getSubject())
                .content(request.getContent())
                .mailType(isSpamMail ? MailType.SPAM : MailType.INBOX)
                .spam(isSpamMail)
                .hasAttachment(savedFileName != null)
                .attachmentName(savedFileName)
                .read(false)
                .deleted(false)
                .build();
        emailLogRepository.save(inboxEntry);
      } catch (Exception e) {
        log.warn("(sendMail) failed to save recipient log: {}", e.getMessage());
      }

      // Copy vào thư mục Sent sau khi gửi thành công
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

    // Nạp thêm các thư gửi đến tài khoản này từ MySQL Database mà IMAP chưa có hoặc chưa kịp đồng bộ (bao gồm video và tệp đính kèm)
    List<EmailLog> dbLogs = emailLogRepository.findByRecipientAndDeletedFalseOrderByCreatedAtDesc(username);
    for (EmailLog dbLog : dbLogs) {
      if (dbLog.isDeleted()) continue;

      Email converted = convertLogToEmail(dbLog);
      boolean isSpam = dbLog.isSpam() || dbLog.getMailType() == MailType.SPAM || spamFilterService.isSpam(converted);
      if (isSpam) {
        if (!dbLog.isSpam() || dbLog.getMailType() != MailType.SPAM) {
          dbLog.setSpam(true);
          dbLog.setMailType(MailType.SPAM);
          emailLogRepository.save(dbLog);
        }
        continue;
      }

      boolean alreadyInList = emails.stream().anyMatch(e -> {
        if (e.getDbId() != null && e.getDbId().equals(dbLog.getId())) return true;
        if (String.valueOf(dbLog.getId()).equals(e.getId())) return true;
        boolean subMatch = (dbLog.getSubject() == null && e.getSubject() == null)
                || (dbLog.getSubject() != null && dbLog.getSubject().equals(e.getSubject()));
        boolean senderMatch = (dbLog.getSender() == null && e.getFromAddress() == null)
                || (dbLog.getSender() != null && dbLog.getSender().equalsIgnoreCase(e.getFromAddress()));
        return subMatch && senderMatch;
      });

      if (!alreadyInList) {
        emails.add(converted);
      }
    }

    // Đồng bộ ID, attachment và trạng thái đã xóa/đã đọc với MySQL
    try {
      syncInboxWithDb(emails, username);
    } catch (Exception e) {
      log.warn("Failed to sync inbox with db: {}", e.getMessage());
    }

    // Lọc bỏ những thư đã bị chuyển vào thùng rác
    emails = emails.stream()
        .filter(email -> !email.isDeleted())
        .collect(Collectors.toList());

    sortEmailsByDateDesc(emails);
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

    // Nếu IMAP chưa có thư đã gửi, chỉ nạp đúng các thư do chính tài khoản này gửi từ MySQL Database
    if (emails.isEmpty()) {
      List<EmailLog> dbLogs = emailLogRepository.findBySenderAndDeletedFalseOrderByCreatedAtDesc(username);
      for (EmailLog dbLog : dbLogs) {
        if (!dbLog.isDeleted()) {
          emails.add(convertLogToEmail(dbLog));
        }
      }
    }

    // Đồng bộ ID, attachment và trạng thái đã xóa với MySQL
    try {
      syncSentWithDb(emails, username);
    } catch (Exception e) {
      log.warn("Failed to sync sent with db: {}", e.getMessage());
    }

    // Lọc bỏ những thư đã bị chuyển vào thùng rác
    emails = emails.stream()
        .filter(email -> !email.isDeleted())
        .collect(Collectors.toList());

    sortEmailsByDateDesc(emails);
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

    // Nạp thêm các thư spam của tài khoản này từ MySQL Database (bao gồm cả thư gửi trực tiếp qua web app)
    List<EmailLog> dbLogs = emailLogRepository.findByRecipientAndDeletedFalseOrderByCreatedAtDesc(username);
    for (EmailLog dbLog : dbLogs) {
      if (dbLog.isDeleted()) continue;
      Email converted = convertLogToEmail(dbLog);
      boolean isSpam = dbLog.isSpam() || dbLog.getMailType() == MailType.SPAM || spamFilterService.isSpam(converted);
      if (isSpam) {
        if (!dbLog.isSpam() || dbLog.getMailType() != MailType.SPAM) {
          dbLog.setSpam(true);
          dbLog.setMailType(MailType.SPAM);
          emailLogRepository.save(dbLog);
        }
        boolean alreadyInList = allSpam.stream().anyMatch(e -> {
          if (e.getDbId() != null && e.getDbId().equals(dbLog.getId())) return true;
          if (String.valueOf(dbLog.getId()).equals(e.getId())) return true;
          boolean subMatch = (dbLog.getSubject() == null && e.getSubject() == null)
                  || (dbLog.getSubject() != null && dbLog.getSubject().equals(e.getSubject()));
          boolean senderMatch = (dbLog.getSender() == null && e.getFromAddress() == null)
                  || (dbLog.getSender() != null && dbLog.getSender().equalsIgnoreCase(e.getFromAddress()));
          return subMatch && senderMatch;
        });
        if (!alreadyInList) {
          allSpam.add(converted);
        }
      }
    }

    // Đồng bộ ID và trạng thái đã xóa với MySQL
    try {
      syncSpamWithDb(allSpam, username);
    } catch (Exception e) {
      log.warn("Failed to sync spam with db: {}", e.getMessage());
    }

    // Lọc bỏ những thư đã bị chuyển vào thùng rác
    allSpam = allSpam.stream()
        .filter(email -> !email.isDeleted())
        .collect(Collectors.toList());

    sortEmailsByDateDesc(allSpam);
    return allSpam;
  }

  private void syncInboxWithDb(List<Email> emails, String username) {
    if (emails == null || emails.isEmpty()) return;

    List<EmailLog> userInboxLogs = emailLogRepository.findByRecipientOrderByCreatedAtDesc(username);
    Set<Long> usedLogIds = new HashSet<>();

    for (Email email : emails) {
      EmailLog matchedLog = null;
      for (EmailLog log : userInboxLogs) {
        if (!usedLogIds.contains(log.getId())
            && log.getMailType() == MailType.INBOX
            && log.getSubject() != null && log.getSubject().equals(email.getSubject())
            && log.getSender() != null && log.getSender().equalsIgnoreCase(email.getFromAddress())) {
          matchedLog = log;
          break;
        }
      }

      if (matchedLog != null) {
        usedLogIds.add(matchedLog.getId());
        email.setDbId(matchedLog.getId());
        email.setId(String.valueOf(matchedLog.getId()));
        email.setRead(matchedLog.isRead());
        email.setDeleted(matchedLog.isDeleted());

        // Nếu matchedLog chưa có attachment hoặc tên attachment trống, thử khôi phục từ email IMAP hoặc SENT logs của sender
        if (!matchedLog.isHasAttachment() || matchedLog.getAttachmentName() == null || matchedLog.getAttachmentName().trim().isEmpty()) {
          if (email.isHasAttachment() && email.getAttachmentName() != null && !email.getAttachmentName().trim().isEmpty()) {
            matchedLog.setHasAttachment(true);
            matchedLog.setAttachmentName(email.getAttachmentName());
            emailLogRepository.save(matchedLog);
          } else if (email.getFromAddress() != null) {
            List<EmailLog> senderSentLogs = emailLogRepository.findBySenderOrderByCreatedAtDesc(email.getFromAddress());
            for (EmailLog sentLog : senderSentLogs) {
              if (sentLog.getSubject() != null && sentLog.getSubject().equals(email.getSubject())
                  && sentLog.isHasAttachment() && sentLog.getAttachmentName() != null) {
                matchedLog.setHasAttachment(true);
                matchedLog.setAttachmentName(sentLog.getAttachmentName());
                emailLogRepository.save(matchedLog);
                break;
              }
            }
          }
        }

        if (matchedLog.isHasAttachment()) {
          email.setHasAttachment(true);
          email.setAttachmentName(matchedLog.getAttachmentName());
        }
      } else {
        boolean hasAttach = email.isHasAttachment();
        String attachName = email.getAttachmentName();

        // Kiểm tra xem người gửi (sender) có bản ghi SENT với attachment không
        if (!hasAttach && email.getFromAddress() != null) {
          List<EmailLog> senderSentLogs = emailLogRepository.findBySenderOrderByCreatedAtDesc(email.getFromAddress());
          for (EmailLog sentLog : senderSentLogs) {
            if (sentLog.getSubject() != null && sentLog.getSubject().equals(email.getSubject())
                && sentLog.isHasAttachment() && sentLog.getAttachmentName() != null) {
              hasAttach = true;
              attachName = sentLog.getAttachmentName();
              break;
            }
          }
        }

        EmailLog newLog = EmailLog.builder()
            .sender(email.getFromAddress())
            .recipient(username)
            .subject(email.getSubject())
            .content(email.getContent())
            .mailType(MailType.INBOX)
            .hasAttachment(hasAttach)
            .attachmentName(attachName)
            .read(false)
            .deleted(false)
            .messageUuid(email.getId())
            .build();
        newLog = emailLogRepository.save(newLog);
        usedLogIds.add(newLog.getId());

        email.setDbId(newLog.getId());
        email.setId(String.valueOf(newLog.getId()));
        email.setRead(false);
        email.setDeleted(false);
        email.setHasAttachment(hasAttach);
        email.setAttachmentName(attachName);
      }
    }
  }

  private void syncSentWithDb(List<Email> emails, String username) {
    if (emails == null || emails.isEmpty()) return;

    List<EmailLog> userSentLogs = emailLogRepository.findBySenderOrderByCreatedAtDesc(username);
    Set<Long> usedLogIds = new HashSet<>();

    for (Email email : emails) {
      EmailLog matchedLog = null;
      for (EmailLog log : userSentLogs) {
        if (!usedLogIds.contains(log.getId())
            && log.getMailType() == MailType.SENT
            && log.getSubject() != null && log.getSubject().equals(email.getSubject())
            && log.getRecipient() != null && log.getRecipient().equalsIgnoreCase(email.getToAddress())) {
          matchedLog = log;
          break;
        }
      }

      if (matchedLog != null) {
        usedLogIds.add(matchedLog.getId());
        email.setDbId(matchedLog.getId());
        email.setId(String.valueOf(matchedLog.getId()));
        email.setDeleted(matchedLog.isDeleted());
        email.setRead(matchedLog.isRead());
        if (matchedLog.isHasAttachment()) {
          email.setHasAttachment(true);
          email.setAttachmentName(matchedLog.getAttachmentName());
        }
      } else {
        EmailLog newLog = EmailLog.builder()
            .sender(username)
            .recipient(email.getToAddress())
            .subject(email.getSubject())
            .content(email.getContent())
            .mailType(MailType.SENT)
            .hasAttachment(email.isHasAttachment())
            .attachmentName(email.getAttachmentName())
            .read(true)
            .deleted(false)
            .messageUuid(email.getId())
            .build();
        newLog = emailLogRepository.save(newLog);
        usedLogIds.add(newLog.getId());

        email.setDbId(newLog.getId());
        email.setId(String.valueOf(newLog.getId()));
        email.setDeleted(false);
      }
    }
  }

  private void syncSpamWithDb(List<Email> emails, String username) {
    if (emails == null || emails.isEmpty()) return;

    List<EmailLog> userSpamLogs = emailLogRepository.findByRecipientOrderByCreatedAtDesc(username);
    Set<Long> usedLogIds = new HashSet<>();

    for (Email email : emails) {
      EmailLog matchedLog = null;
      for (EmailLog log : userSpamLogs) {
        if (!usedLogIds.contains(log.getId())
            && (log.isSpam() || log.getMailType() == MailType.SPAM)
            && log.getSubject() != null && log.getSubject().equals(email.getSubject())
            && log.getSender() != null && log.getSender().equalsIgnoreCase(email.getFromAddress())) {
          matchedLog = log;
          break;
        }
      }

      if (matchedLog != null) {
        usedLogIds.add(matchedLog.getId());
        email.setDbId(matchedLog.getId());
        email.setId(String.valueOf(matchedLog.getId()));
        email.setDeleted(matchedLog.isDeleted());
      } else {
        EmailLog newLog = EmailLog.builder()
            .sender(email.getFromAddress())
            .recipient(username)
            .subject(email.getSubject())
            .content(email.getContent())
            .mailType(MailType.SPAM)
            .spam(true)
            .hasAttachment(email.isHasAttachment())
            .attachmentName(email.getAttachmentName())
            .read(false)
            .deleted(false)
            .messageUuid(email.getId())
            .build();
        newLog = emailLogRepository.save(newLog);
        usedLogIds.add(newLog.getId());

        email.setDbId(newLog.getId());
        email.setId(String.valueOf(newLog.getId()));
        email.setDeleted(false);
      }
    }
  }

  public void sortEmailsByDateDesc(List<Email> emails) {
    if (emails == null) return;
    emails.sort((e1, e2) -> {
      if (e1.getRawDate() != null && e2.getRawDate() != null) {
        return e2.getRawDate().compareTo(e1.getRawDate());
      }
      if (e1.getRawDate() != null) return -1;
      if (e2.getRawDate() != null) return 1;
      return 0;
    });
  }

  public Email convertLogToEmail(EmailLog log) {
    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    String dateStr = log.getCreatedAt() != null ? log.getCreatedAt().format(formatter) : "";
    java.util.Date rawDate = null;
    if (log.getCreatedAt() != null) {
      rawDate = java.util.Date.from(log.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
    }
    return Email.builder()
            .id(log.getId() != null ? String.valueOf(log.getId()) : (log.getMessageUuid() != null ? log.getMessageUuid() : java.util.UUID.randomUUID().toString()))
            .fromAddress(log.getSender())
            .toAddress(log.getRecipient())
            .subject(log.getSubject() != null ? log.getSubject() : "(Không có tiêu đề)")
            .content(log.getContent() != null ? log.getContent() : "")
            .date(dateStr)
            .rawDate(rawDate)
            .isReplying(false)
            .dbId(log.getId())
            .read(log.isRead())
            .deleted(log.isDeleted())
            .hasAttachment(log.isHasAttachment())
            .attachmentName(log.getAttachmentName())
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

  public void deleteEmailPermanently(EmailLog logEntity, String username, String password) {
    if (logEntity == null) return;
    try {
      Session session = sessionService.getReceiveMailSession(username);
      String folderName = "INBOX";
      String otherParty = logEntity.getSender();
      if (logEntity.getMailType() == MailType.SENT) {
        folderName = "SENT";
        otherParty = logEntity.getRecipient();
      } else if (logEntity.getMailType() == MailType.SPAM) {
        folderName = "SPAM";
        otherParty = logEntity.getSender();
      }
      mailService.deleteMessageFromFolder(session, username, password, folderName, logEntity.getSubject(), otherParty);
    } catch (Exception e) {
      log.warn("Failed to permanently delete email from IMAP: {}", e.getMessage());
    }
  }

  public long countTotalInbox(String username) {
    if (username == null || username.trim().isEmpty()) {
      return 0;
    }
    try {
      return emailLogRepository.countTotalInbox(username.trim());
    } catch (Exception e) {
      return 0;
    }
  }

  public long countUnreadInbox(String username) {
    return countUnreadInbox(username, null);
  }

  public long countUnreadInbox(String username, String password) {
    if (username == null || username.trim().isEmpty()) {
      return 0;
    }
    // 1. Kiểm tra nhanh nếu IMAP có thư mới chưa đồng bộ vào MySQL
    try {
      if (password != null && !password.trim().isEmpty()) {
        Session session = sessionService.getReceiveMailSession(username);
        String mailStoreType = username.contains("gmail") ? "imaps" : "imap";
        Store store = session.getStore(mailStoreType);
        store.connect(username, password);
        Folder folder = store.getFolder("INBOX");
        if (folder != null && folder.exists()) {
          folder.open(Folder.READ_ONLY);
          int imapTotal = folder.getMessageCount();
          folder.close(false);
          store.close();

          long dbTotal = emailLogRepository.countTotalInbox(username.trim());
          if (imapTotal > dbTotal) {
            log.info("(countUnreadInbox) Phat hien thu moi tu IMAP (IMAP: {}, DB: {}). Tien hanh dong bo...", imapTotal, dbTotal);
            List<Email> rawEmails = mailService.getInBoxMail(username, password, mailStoreType, session);
            if (rawEmails != null && !rawEmails.isEmpty()) {
              List<Email> nonSpam = rawEmails.stream()
                      .filter(email -> !spamFilterService.isSpam(email))
                      .collect(Collectors.toList());
              syncInboxWithDb(nonSpam, username);

              List<Email> spamList = rawEmails.stream()
                      .filter(email -> spamFilterService.isSpam(email))
                      .collect(Collectors.toList());
              if (!spamList.isEmpty()) {
                syncSpamWithDb(spamList, username);
              }
            }
          }
        } else if (store.isConnected()) {
          store.close();
        }
      }
    } catch (Exception e) {
      log.debug("(countUnreadInbox) IMAP check exception: {}", e.getMessage());
    }

    // 2. Đếm số lượng thư chưa đọc trong Database
    try {
      return emailLogRepository.countUnreadInbox(username.trim());
    } catch (Exception e) {
      log.warn("(countUnreadInbox) error: {}", e.getMessage());
      return 0;
    }
  }
}
