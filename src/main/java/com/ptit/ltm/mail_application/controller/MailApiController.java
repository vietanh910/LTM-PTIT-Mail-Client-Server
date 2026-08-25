package com.ptit.ltm.mail_application.controller;

import com.ptit.ltm.mail_application.dto.SendMailRequest;
import com.ptit.ltm.mail_application.facade.impl.MailFacadeServiceImpl;
import com.ptit.ltm.mail_application.model.Email;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/mails")
public class MailApiController {
  private final MailFacadeServiceImpl mailFacadeService;

  @GetMapping("/sent")
  public List<Email> listMail(HttpSession session) {
    String username = "user1@domain1.com";
    String password = "user1";

    return mailFacadeService.listSentMail(username, password);
  }

  @GetMapping("/spam")
  public List<Email> listSpamMail(HttpSession session) {
    String username = "user1@domain1.com";
    String password = "user1";

    return mailFacadeService.listSpamMail(username, password);
  }

  @GetMapping("/inbox")
  public List<Email> emails(HttpSession session) {
    String username = "user2@domain2.com";
    String password = "user2";

    return mailFacadeService.listInboxMail(username, password);
  }

  @PostMapping("/send")
  public void send(@RequestPart("file") MultipartFile file) {
    SendMailRequest request = SendMailRequest.of(
        "user2@domain2.com",
        "subject",
        "content",
        null);

    String username = "user1@domain1.com";
    String password = "user1";

    mailFacadeService.sendMail(request, file, username, password);
  }

}
