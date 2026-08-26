package com.ptit.ltm.mail_application.controller;

import com.ptit.ltm.mail_application.dto.LoginRequest;
import com.ptit.ltm.mail_application.facade.impl.MailFacadeServiceImpl;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;
import com.ptit.ltm.mail_application.entity.User;
import com.ptit.ltm.mail_application.service.UserService;
import org.springframework.ui.Model;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {
  private final MailFacadeServiceImpl mailFacadeService;
  private final UserService userService;

  @GetMapping("/login")
  public String login(@RequestParam(value = "error", required = false) String error,
                      @RequestParam(value = "registered", required = false) String registered,
                      Model model) {
    if (error != null) {
      model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không chính xác!");
    }
    if (registered != null) {
      model.addAttribute("message", "Đăng ký thành công! Vui lòng đăng nhập.");
    }
    return "login";
  }

  @PostMapping("/login")
  public String login(@Valid LoginRequest loginRequest, HttpSession httpSession) {
    log.info("(login) username: {}", loginRequest.getUsername());

    boolean dbAuthenticated = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
    boolean mailServerAuthenticated = mailFacadeService.isAuthenticatedWithHMailServer(loginRequest.getUsername(), loginRequest.getPassword());

    if (dbAuthenticated || mailServerAuthenticated) {
      String fullEmail = loginRequest.getUsername();
      if (!fullEmail.contains("@")) {
        Optional<User> userOpt = userService.findByUsername(loginRequest.getUsername());
        if (userOpt.isPresent() && userOpt.get().getEmail() != null) {
          fullEmail = userOpt.get().getEmail();
        } else {
          fullEmail = loginRequest.getUsername() + "@domain1.com";
        }
      }
      httpSession.setAttribute("username", fullEmail);
      httpSession.setAttribute("password", loginRequest.getPassword());
      return "redirect:/";
    }
    return "redirect:/login?error=true";
  }

  @GetMapping("/logout")
  public String logout(HttpSession httpSession) {
    httpSession.invalidate();
    return "redirect:/login";
  }

  @GetMapping("/register")
  public String register() {
    return "register";
  }

  @PostMapping("/register")
  public String handleRegister(@RequestParam Map<String, String> params, Model model) {
    log.info("(register) params: {}", params);
    String username = params.get("taikhoan");
    String password = params.get("matkhau");
    String fullName = params.get("ten");
    String email = params.get("email");
    String phone = params.get("dt");
    String address = params.get("diachi");

    try {
      userService.registerUser(username, password, fullName, email, phone, address);
      return "redirect:/login?registered=true";
    } catch (Exception e) {
      log.error("Register failed: {}", e.getMessage());
      return "redirect:/register?error=" + e.getMessage();
    }
  }
}
