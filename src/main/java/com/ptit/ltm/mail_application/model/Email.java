package com.ptit.ltm.mail_application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class Email {
    private String id = UUID.randomUUID().toString();
    private String fromAddress;
    private String toAddress;
    private String subject;
    private String content;
    private String date;
    private boolean isReplying;
    
}
