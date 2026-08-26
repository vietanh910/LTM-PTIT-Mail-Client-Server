package com.ptit.ltm.mail_application.service;

import com.ptit.ltm.mail_application.entity.User;
import java.util.Optional;

public interface UserService {
    User registerUser(String username, String password, String fullName, String email, String phone, String address);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean authenticate(String username, String password);
}
