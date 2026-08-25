package com.ptit.ltm.mail_application.controller;

import com.ptit.ltm.mail_application.dto.LoginRequest;
import com.ptit.ltm.mail_application.facade.impl.MailFacadeServiceImpl;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {
  private final MailFacadeServiceImpl mailFacadeService;

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @PostMapping("/login")
  public String login(@Valid LoginRequest loginRequest, Error error, HttpSession httpSession) {
    log.info("(login) loginRequest: {}", loginRequest);

    if (mailFacadeService.isAuthenticatedWithHMailServer(loginRequest.getUsername(), loginRequest.getPassword())) {
      httpSession.setAttribute("username", loginRequest.getUsername());
      httpSession.setAttribute("password", loginRequest.getPassword());
      return "redirect:/";
    }
    return "login";
  }

  @GetMapping("/register")
  public String register() {
    return "register";
  }
}
