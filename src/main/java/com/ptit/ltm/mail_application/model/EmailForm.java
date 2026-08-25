package com.ptit.ltm.mail_application.model;

import lombok.Data;

@Data
public class EmailForm {
    private String recipient;
    private String subject;
    private String content;
}
