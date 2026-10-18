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

        if (email != null && !email.trim().isEmpty() && userRepository.existsByEmail(email.trim())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
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

        User savedUser = userRepository.save(user);

        // Đồng bộ tạo tài khoản trên hMailServer nếu là email nội bộ (@domain1.com)
        syncHMailServerAccount(email, password);

        return savedUser;
    }

    private void syncHMailServerAccount(String email, String password) {
        if (email == null || !email.toLowerCase().endsWith("@domain1.com")) {
            return;
        }
        try {
            String domain = email.substring(email.indexOf("@") + 1);
            String psCmd = String.format(
                "& { $app = New-Object -ComObject hMailServer.Application; " +
                "$app.Authenticate('Administrator', '123456'); " +
                "$d = $app.Domains.ItemByName('%s'); " +
                "if ($d -ne $null) { " +
                "  $acc = $null; " +
                "  try { $acc = $d.Accounts.ItemByAddress('%s'); } catch {} " +
                "  if ($acc -eq $null) { " +
                "    $acc = $d.Accounts.Add(); " +
                "    $acc.Address = '%s'; " +
                "    $acc.Password = '%s'; " +
                "    $acc.Active = $true; " +
                "    $acc.Save(); " +
                "  } " +
                "} }", domain, email, email, password
            );

            ProcessBuilder pb = new ProcessBuilder("powershell", "-ExecutionPolicy", "Bypass", "-Command", psCmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
            }
            log.info("(syncHMailServerAccount) Dong bo tai khoan hMailServer thanh cong: {}", email);
        } catch (Exception e) {
            log.warn("(syncHMailServerAccount) Khong the tao tai khoan hMailServer: {}", e.getMessage());
        }
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
