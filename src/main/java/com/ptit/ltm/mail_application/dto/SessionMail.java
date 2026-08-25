package com.ptit.ltm.mail_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class SessionMail {
    private String username;
    private String password;

    public static SessionMail of(String username) {
        return of(username, null);
    }
}
