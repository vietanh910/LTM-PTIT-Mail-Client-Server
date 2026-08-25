package com.ptit.ltm.mail_application.model;

import lombok.Data;

import java.util.ArrayList;

@Data
public class MailContent {
    private ArrayList<Row> ops;

    @Data
    public class Row {
        private String insert;
    }

}

