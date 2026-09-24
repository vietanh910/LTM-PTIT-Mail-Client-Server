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
import jakarta.mail.util.ByteArrayDataSource;
import java.io.IOException;
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

            for (Message message : messages) {
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

                Email email = Email.of(
                        UUID.randomUUID().toString(),
                        from,
                        to,
                        subject,
                        content,
                        dateStr,
                        false
                );

                emails.add(email);
            }

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
}
