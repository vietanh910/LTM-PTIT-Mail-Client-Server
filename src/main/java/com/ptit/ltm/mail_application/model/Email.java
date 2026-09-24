package com.ptit.ltm.mail_application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Email {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String fromAddress;
    private String toAddress;
    private String subject;
    private String content;
    private String date;
    private boolean isReplying;
    private Long dbId;
    @Builder.Default
    private boolean read = false;
    @Builder.Default
    private boolean deleted = false;

    public static Email of(String id, String fromAddress, String toAddress, String subject, String content, String date, boolean isReplying) {
        return Email.builder()
                .id(id)
                .fromAddress(fromAddress)
                .toAddress(toAddress)
                .subject(subject)
                .content(content)
                .date(date)
                .isReplying(isReplying)
                .read(false)
                .deleted(false)
                .build();
    }
}

