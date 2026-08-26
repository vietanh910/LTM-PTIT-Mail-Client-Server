package com.ptit.ltm.mail_application.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailUtils {
    public static String extractEmail(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        String regex = "<([^>]+)>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1).trim();
        } else {
            return input.trim();
        }
    }
}
