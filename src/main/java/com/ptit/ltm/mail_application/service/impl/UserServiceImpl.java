package com.ptit.ltm.mail_application.service.impl;

import com.ptit.ltm.mail_application.entity.User;
import com.ptit.ltm.mail_application.repository.UserRepository;
import com.ptit.ltm.mail_application.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public User registerUser(String username, String password, String fullName, String email, String phone, String address) {
        log.info("Registering user: username={}, email={}", username, email);
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }

        User user = User.builder()
                .username(username)
                .password(password)
                .fullName(fullName)
                .email(email)
                .phoneNumber(phone)
                .address(address)
                .role("ROLE_USER")
                .status("ACTIVE")
                .build();

        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(username);
        }
        return userOpt.isPresent() && userOpt.get().getPassword().equals(password);
    }
}
