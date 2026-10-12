package com.ptit.ltm.mail_application.service.impl;

import com.ptit.ltm.mail_application.model.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ptit.ltm.mail_application.utils.MailUtils.extractEmail;


@Slf4j
@Service
public class MailServiceImpl {

    public Message mailSend(String fromMail, Session session, Email email, String fileName) {
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setHeader("Content-Type", "text/plain; charset=UTF-8");
            msg.setFrom(new InternetAddress(fromMail));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(email.getToAddress()));
            msg.setSubject(email.getSubject(), "UTF-8");
            msg.setSentDate(new Date());

            if (Objects.nonNull(fileName) && fileName.length() > 0) {
                BodyPart messageBodyPart = new MimeBodyPart();

                messageBodyPart.setText(email.getContent());

                Multipart multipart = new MimeMultipart();

                multipart.addBodyPart(messageBodyPart);

                messageBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(fileName);
                messageBodyPart.setDataHandler(new DataHandler(source));
                String[] name = fileName.split("\\\\");
                messageBodyPart.setFileName(name[name.length - 1]);
                multipart.addBodyPart(messageBodyPart);

                msg.setContent(multipart, "UTF-8");
            } else {
                msg.setText(email.getContent(), "UTF-8");
            }
            return msg;
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Message replyMail(Session session, Email email, Message message) {
        try {
            MimeMessage replyMessage = new MimeMessage(session);
            replyMessage = (MimeMessage) message.reply(false);
            replyMessage.setFrom(new InternetAddress(email.getFromAddress()));
            replyMessage.setText(email.getContent(), "UTF-8");
            replyMessage.setReplyTo(message.getReplyTo());
            return replyMessage;
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void copyIntoSent(final Message msg, Session session, String username, String password) {
        log.info("(copyIntoSent) start for user: {}", username);

        // Danh sách tên thư mục Sent thường gặp trong hMailServer
        String[] sentFolderNames = {"Sent", "SENT", "Sent Items", "Sent Messages", "sent"};

        Store emailStore = null;
        try {
            emailStore = session.getStore("imap");
            emailStore.connect("127.0.0.1", 143, username, password);

            Folder folder = null;
            for (String folderName : sentFolderNames) {
                try {
                    Folder f = emailStore.getFolder(folderName);
                    if (f != null && f.exists()) {
                        folder = f;
                        break;
                    }
                } catch (Exception ignored) {}
            }

            if (folder == null) {
                // Tạo thư mục Sent nếu chưa tồn tại
                folder = emailStore.getFolder("Sent");
                if (!folder.exists()) {
                    folder.create(Folder.HOLDS_MESSAGES);
                }
            }

            folder.open(Folder.READ_WRITE);
            folder.appendMessages(new Message[]{msg});
            folder.close(false);
            log.info("(copyIntoSent) copied to Sent folder successfully");
        } catch (Exception e) {
            // Không throw exception - việc copy vào Sent thất bại không ảnh hưởng đến việc gửi thư
            log.warn("(copyIntoSent) failed to copy to Sent folder (non-critical): {}", e.getMessage());
        } finally {
            if (emailStore != null) {
                try { emailStore.close(); } catch (Exception ignored) {}
            }
        }
    }

    public Message getSendMailMessage(Email email, MultipartFile file, Session session) {
        log.info("(sendEmail) email: {}", email);

        Message message = new MimeMessage(session);
        MimeBodyPart bodyPart = new MimeBodyPart();
        MimeMultipart multipart = new MimeMultipart();
        try {
            message.setFrom(new InternetAddress(email.getFromAddress()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email.getToAddress()));
            message.setSubject(email.getSubject());

            if (file != null && !file.isEmpty()) {
                bodyPart.setText(email.getContent());
                multipart.addBodyPart(bodyPart);
                MimeBodyPart attachmentPart = new MimeBodyPart();
                DataSource source = new ByteArrayDataSource(file.getBytes(), file.getContentType());
                attachmentPart.setDataHandler(new DataHandler(source));
                attachmentPart.setFileName(file.getOriginalFilename());
                multipart.addBodyPart(attachmentPart);
                message.setContent(multipart);
            } else {
                message.setText(email.getContent());
            }

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return message;
    }

    public List<Email> getInBoxMail(String username, String password, String mailStoreType, Session emailSession) {
        return this.getEmails(username, password, mailStoreType, emailSession, "INBOX");
    }

    public List<Email> getSentMail(String username, String password, String mailStoreType, Session emailSession) {
        return this.getEmails(username, password, mailStoreType, emailSession, "SENT");
    }

    public List<Email> getSpamMail(String username, String password, String mailStoreType, Session emailSession) {
        return this.getEmails(username, password, mailStoreType, emailSession, "SPAM");
    }

    public List<Email> getEmails(String username, String password, String mailStoreType, Session emailSession, String folderName) {
        log.info("(getEmails) username: {}, folderName: {}", username, folderName);

        List<Email> emails = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        try {
            Store emailStore = (emailSession.getStore(mailStoreType));
            emailStore.connect(username, password);

            Folder folder = emailStore.getFolder(folderName);
            if (!folder.exists()) {
                log.warn("Folder {} does not exist", folderName);
                emailStore.close();
                return emails;
            }
            folder.open(Folder.READ_ONLY);

            log.info("Folder {} message count: {}", folderName, folder.getMessageCount());

            Message[] messages = folder.getMessages();

            // Duyệt từ thư mới nhất đến cũ nhất
            for (int i = messages.length - 1; i >= 0; i--) {
                Message message = messages[i];
                String from = "";
                if (message.getFrom() != null && message.getFrom().length > 0) {
                    from = extractEmail(message.getFrom()[0].toString());
                }

                String to = "";
                if (message.getAllRecipients() != null && message.getAllRecipients().length > 0) {
                    to = extractEmail(message.getAllRecipients()[0].toString());
                }

                String subject = message.getSubject() != null ? message.getSubject() : "(Không có tiêu đề)";
                
                Date mailDate = message.getReceivedDate() != null ? message.getReceivedDate() : message.getSentDate();
                String dateStr = mailDate != null ? simpleDateFormat.format(mailDate) : "";

                String content = extractTextFromMessage(message);
                List<String> attachments = extractAttachmentsFromMessage(message);
                boolean hasAttachment = !attachments.isEmpty();
                String attachmentName = hasAttachment ? String.join(";", attachments) : null;

                Email email = Email.builder()
                        .id(UUID.randomUUID().toString())
                        .fromAddress(from)
                        .toAddress(to)
                        .subject(subject)
                        .content(content)
                        .date(dateStr)
                        .rawDate(mailDate)
                        .isReplying(false)
                        .hasAttachment(hasAttachment)
                        .attachmentName(attachmentName)
                        .build();

                emails.add(email);
            }

            // Sắp xếp đảm bảo thư mới nhất luôn ở đầu danh sách
            emails.sort((e1, e2) -> {
                if (e1.getRawDate() != null && e2.getRawDate() != null) {
                    return e2.getRawDate().compareTo(e1.getRawDate());
                }
                return 0;
            });

            folder.close(false);
            emailStore.close();
        } catch (Exception e) {
            log.error("Error while fetching emails from folder {}: {}", folderName, e.getMessage(), e);
        }

        return emails;
    }

    private String extractTextFromMessage(Message message) {
        try {
            if (message.isMimeType("text/plain")) {
                return message.getContent().toString();
            } else if (message.isMimeType("text/html")) {
                return message.getContent().toString();
            } else if (message.isMimeType("multipart/*")) {
                Multipart multipart = (Multipart) message.getContent();
                return extractTextFromMultipart(multipart);
            }
            return message.getContent() != null ? message.getContent().toString() : "";
        } catch (Exception e) {
            log.warn("Cannot extract text from message: {}", e.getMessage());
            return "";
        }
    }

    private String extractTextFromMultipart(Multipart multipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            // Bỏ qua các phần là tệp đính kèm khi trích xuất nội dung văn bản
            String disposition = bodyPart.getDisposition();
            String rawFileName = bodyPart.getFileName();
            if (Part.ATTACHMENT.equalsIgnoreCase(disposition) || (rawFileName != null && !rawFileName.trim().isEmpty())) {
                continue;
            }

            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                if (result.length() == 0) {
                    result.append(bodyPart.getContent());
                }
            } else if (bodyPart.getContent() instanceof Multipart) {
                result.append(extractTextFromMultipart((Multipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }

    private List<String> extractAttachmentsFromMessage(Message message) {
        List<String> attachments = new ArrayList<>();
        try {
            if (message.isMimeType("multipart/*")) {
                Multipart multipart = (Multipart) message.getContent();
                extractAttachmentsFromMultipart(multipart, attachments);
            }
        } catch (Exception e) {
            log.warn("(extractAttachmentsFromMessage) error: {}", e.getMessage());
        }
        return attachments;
    }

    private void extractAttachmentsFromMultipart(Multipart multipart, List<String> attachments) throws MessagingException, IOException {
        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            try {
                if (bodyPart.getContent() instanceof Multipart) {
                    extractAttachmentsFromMultipart((Multipart) bodyPart.getContent(), attachments);
                    continue;
                }
            } catch (Exception ignored) {}

            String disposition = bodyPart.getDisposition();
            String rawFileName = bodyPart.getFileName();

            boolean isAttachment = false;
            if (rawFileName != null && !rawFileName.trim().isEmpty()) {
                isAttachment = true;
            } else if (Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
                isAttachment = true;
            }

            if (isAttachment) {
                String fileName = rawFileName != null ? MimeUtility.decodeText(rawFileName) : "attachment_" + System.currentTimeMillis();
                String safeOriginal = fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_");

                // Kiểm tra xem file này đã tồn tại trong thư mục uploads chưa (do người gửi trên cùng máy đã lưu)
                String savedFileName = null;
                File[] existingFiles = dir.listFiles();
                if (existingFiles != null) {
                    for (File f : existingFiles) {
                        if (f.getName().endsWith("_" + safeOriginal) || f.getName().equalsIgnoreCase(safeOriginal)) {
                            if (f.length() > 0) {
                                savedFileName = f.getName();
                                break;
                            }
                        }
                    }
                }

                // Nếu chưa có, lưu từ luồng dữ liệu của BodyPart
                if (savedFileName == null) {
                    savedFileName = System.currentTimeMillis() + "_" + safeOriginal;
                    Path targetPath = Paths.get(uploadDir, savedFileName);
                    try (InputStream is = bodyPart.getInputStream()) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    log.info("(extractAttachments) Saved attachment to disk: {}", savedFileName);
                }

                if (!attachments.contains(savedFileName)) {
                    attachments.add(savedFileName);
                }
            }
        }
    }

    public void deleteMessageFromFolder(Session session, String username, String password, String folderName, String subject, String otherParty) {
        log.info("(deleteMessageFromFolder) username: {}, folder: {}, subject: {}", username, folderName, subject);
        Store emailStore = null;
        try {
            String mailStoreType = username != null && username.contains("gmail") ? "imaps" : "imap";
            emailStore = session.getStore(mailStoreType);
            emailStore.connect("127.0.0.1", 143, username, password);

            List<String> candidateFolders = new ArrayList<>();
            candidateFolders.add(folderName);
            if ("SENT".equalsIgnoreCase(folderName)) {
                candidateFolders.addAll(Arrays.asList("Sent", "SENT", "Sent Items", "Sent Messages", "sent"));
            } else if ("SPAM".equalsIgnoreCase(folderName)) {
                candidateFolders.addAll(Arrays.asList("Spam", "SPAM", "Junk", "Junk E-mail", "spam"));
            } else if ("INBOX".equalsIgnoreCase(folderName)) {
                candidateFolders.addAll(Arrays.asList("INBOX", "Inbox", "inbox"));
            }

            for (String fName : candidateFolders) {
                Folder folder = null;
                try {
                    folder = emailStore.getFolder(fName);
                } catch (Exception ignored) {}

                if (folder != null && folder.exists()) {
                    try {
                        folder.open(Folder.READ_WRITE);
                        Message[] messages = folder.getMessages();
                        boolean found = false;
                        for (int i = messages.length - 1; i >= 0; i--) {
                            Message msg = messages[i];
                            String msgSub = msg.getSubject() != null ? msg.getSubject() : "";
                            String msgFrom = (msg.getFrom() != null && msg.getFrom().length > 0) ? extractEmail(msg.getFrom()[0].toString()) : "";
                            String msgTo = (msg.getAllRecipients() != null && msg.getAllRecipients().length > 0) ? extractEmail(msg.getAllRecipients()[0].toString()) : "";

                            boolean subMatch = (subject == null && msgSub.isEmpty()) || (subject != null && subject.equals(msgSub));
                            boolean partyMatch = (otherParty == null) || msgFrom.equalsIgnoreCase(otherParty) || msgTo.equalsIgnoreCase(otherParty);

                            if (subMatch && partyMatch) {
                                msg.setFlag(Flags.Flag.DELETED, true);
                                found = true;
                                break;
                            }
                        }
                        folder.close(true); // true = expunge
                        if (found) {
                            log.info("(deleteMessageFromFolder) Da xoa thu khoi IMAP folder {}", fName);
                            break;
                        }
                    } catch (Exception e) {
                        try { if (folder.isOpen()) folder.close(false); } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            log.warn("(deleteMessageFromFolder) failed: {}", e.getMessage());
        } finally {
            if (emailStore != null) {
                try { emailStore.close(); } catch (Exception ignored) {}
            }
        }
    }
}
