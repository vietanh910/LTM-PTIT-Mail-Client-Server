package com.ptit.ltm.mail_application.repository;

import com.ptit.ltm.mail_application.entity.EmailLog;
import com.ptit.ltm.mail_application.entity.MailType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    // --- Queries cũ (giữ lại) ---
    List<EmailLog> findBySenderOrderByCreatedAtDesc(String sender);
    List<EmailLog> findByRecipientOrderByCreatedAtDesc(String recipient);
    List<EmailLog> findByMailTypeOrderByCreatedAtDesc(MailType mailType);
    java.util.Optional<EmailLog> findByMessageUuid(String messageUuid);

    // --- Queries lọc chưa bị xóa ---
    List<EmailLog> findByRecipientAndDeletedFalseOrderByCreatedAtDesc(String recipient);
    List<EmailLog> findBySenderAndDeletedFalseOrderByCreatedAtDesc(String sender);

    // --- Đếm số thư chưa đọc trong Hộp thư đến ---
    @Query("SELECT COUNT(e) FROM EmailLog e WHERE " +
           "(e.recipient = :recipient OR (:recipient LIKE '%@%' AND e.recipient = SUBSTRING(:recipient, 1, LOCATE('@', :recipient) - 1))) " +
           "AND e.mailType = com.ptit.ltm.mail_application.entity.MailType.INBOX " +
           "AND e.read = false AND e.deleted = false AND (e.spam = false OR e.spam IS NULL)")
    long countUnreadInbox(@Param("recipient") String recipient);

    // --- Đếm tổng số thư trong Hộp thư đến ---
    @Query("SELECT COUNT(e) FROM EmailLog e WHERE " +
           "(e.recipient = :recipient OR (:recipient LIKE '%@%' AND e.recipient = SUBSTRING(:recipient, 1, LOCATE('@', :recipient) - 1))) " +
           "AND e.mailType = com.ptit.ltm.mail_application.entity.MailType.INBOX " +
           "AND e.deleted = false")
    long countTotalInbox(@Param("recipient") String recipient);

    // --- Thùng rác: thư bị xóa của người dùng hiện tại ---
    @Query("SELECT e FROM EmailLog e WHERE e.deleted = true AND (" +
           "(e.recipient = :email AND e.mailType = com.ptit.ltm.mail_application.entity.MailType.INBOX) OR " +
           "(e.sender = :email AND e.mailType = com.ptit.ltm.mail_application.entity.MailType.SENT) OR " +
           "(e.recipient = :email AND e.mailType = com.ptit.ltm.mail_application.entity.MailType.SPAM) OR " +
           "(e.mailType IS NULL AND (e.recipient = :email OR e.sender = :email))) " +
           "ORDER BY e.deletedAt DESC")
    List<EmailLog> findDeletedByOwner(@Param("email") String email);

    // --- Xóa mềm hàng loạt theo danh sách ID ---
    @Modifying
    @Transactional
    @Query("UPDATE EmailLog e SET e.deleted = true, e.deletedAt = :now WHERE e.id IN :ids")
    void softDeleteByIds(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE EmailLog e SET e.deleted = true, e.deletedAt = :now WHERE e.messageUuid IN :uuids")
    void softDeleteByUuids(@Param("uuids") List<String> uuids, @Param("now") LocalDateTime now);

    // --- Đánh dấu đã đọc hàng loạt ---
    @Modifying
    @Transactional
    @Query("UPDATE EmailLog e SET e.read = true WHERE e.id IN :ids")
    void markAsReadByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("UPDATE EmailLog e SET e.read = true WHERE e.messageUuid IN :uuids")
    void markAsReadByUuids(@Param("uuids") List<String> uuids);

    // --- Khôi phục thư từ thùng rác ---
    @Modifying
    @Transactional
    @Query("UPDATE EmailLog e SET e.deleted = false, e.deletedAt = null WHERE e.id IN :ids")
    void restoreByIds(@Param("ids") List<Long> ids);

    // --- Xóa vĩnh viễn hàng loạt theo danh sách ID ---
    @Modifying
    @Transactional
    @Query("DELETE FROM EmailLog e WHERE e.id IN :ids")
    void deleteByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailLog e WHERE e.messageUuid IN :uuids")
    void deleteByUuids(@Param("uuids") List<String> uuids);

    // --- Dọn sạch toàn bộ thùng rác của người dùng ---
    @Modifying
    @Transactional
    @Query("DELETE FROM EmailLog e WHERE e.deleted = true AND (" +
           "(e.recipient = :email AND e.mailType = com.ptit.ltm.mail_application.entity.MailType.INBOX) OR " +
           "(e.sender = :email AND e.mailType = com.ptit.ltm.mail_application.entity.MailType.SENT) OR " +
           "(e.recipient = :email AND e.mailType = com.ptit.ltm.mail_application.entity.MailType.SPAM) OR " +
           "(e.mailType IS NULL AND (e.recipient = :email OR e.sender = :email)))")
    void emptyTrashByOwner(@Param("email") String email);
}

