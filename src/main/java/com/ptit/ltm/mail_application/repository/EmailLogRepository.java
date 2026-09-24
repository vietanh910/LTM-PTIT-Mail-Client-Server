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

    // --- Queries lọc chưa bị xóa ---
    List<EmailLog> findByRecipientAndDeletedFalseOrderByCreatedAtDesc(String recipient);
    List<EmailLog> findBySenderAndDeletedFalseOrderByCreatedAtDesc(String sender);

    // --- Thùng rác: thư bị xóa của người nhận hoặc người gửi ---
    @Query("SELECT e FROM EmailLog e WHERE e.deleted = true AND (e.recipient = :email OR e.sender = :email) ORDER BY e.deletedAt DESC")
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
}

