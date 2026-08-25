package com.ptit.ltm.mail_application.model;

import lombok.Data;

@Data
public class InterviewMail {
    private String to;
    private String intervieweeName;
    private String companyName;
    private String date;
    private String time;
    private String address;
    private String language;
    private String companyPhoneNumber;
    private String companyEmail;
    private String companySignature;
}