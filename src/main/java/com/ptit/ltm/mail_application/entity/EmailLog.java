package com.ptit.ltm.mail_application.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_uuid", length = 100)
    private String messageUuid;

    @Column(nullable = false, length = 150)
    private String sender;

    @Column(nullable = false, length = 150)
    private String recipient;

    @Column(length = 255)
    private String subject;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "mail_type", length = 30)
    @Builder.Default
    private MailType mailType = MailType.SENT;

    @Column(name = "has_attachment")
    @Builder.Default
    private boolean hasAttachment = false;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "is_spam")
    @Builder.Default
    private boolean spam = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
