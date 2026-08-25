package com.ptit.ltm.mail_application.service.impl;

import com.ptit.ltm.mail_application.model.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
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
        log.info("(copyIntoSent) start");

        Folder folder;
        try {
            Store emailStore = (session.getStore("imap"));
            emailStore.connect(username, password);

            folder = emailStore.getFolder("SENT");
            folder.open(Folder.READ_WRITE);

            folder.appendMessages(new Message[]{msg});
        } catch (MessagingException e) {
            throw new RuntimeException(e);
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
        Email email;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyy hh:mm");

        try {
            Store emailStore = (emailSession.getStore(mailStoreType));
            emailStore.connect(username, password);

            Folder folder = emailStore.getFolder(folderName);
            folder.open(Folder.READ_ONLY);

            System.out.println(folder.getMessageCount());

            Message[] messages = folder.getMessages();

            for (Message message : messages) {
                email = Email.of(
                      UUID.randomUUID().toString(),
                        extractEmail(message.getFrom()[0].toString()),
                        extractEmail(message.getAllRecipients()[0].toString()),
                        message.getSubject(),
                        message.getContent().toString(),
                        simpleDateFormat.format(message.getReceivedDate()),
                        false
                );

                emails.add(email);
            }

            folder.close(false);
            emailStore.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return emails;
    }
}
