package com.ptit.ltm.mail_application.repository;

import com.ptit.ltm.mail_application.entity.EmailLog;
import com.ptit.ltm.mail_application.entity.MailType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findBySenderOrderByCreatedAtDesc(String sender);
    List<EmailLog> findByRecipientOrderByCreatedAtDesc(String recipient);
    List<EmailLog> findByMailTypeOrderByCreatedAtDesc(MailType mailType);
}
