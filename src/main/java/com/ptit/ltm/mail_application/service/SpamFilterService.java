package com.ptit.ltm.mail_application.service;

import com.ptit.ltm.mail_application.model.Email;

public interface SpamFilterService {
    boolean isSpam(Email email);
}
