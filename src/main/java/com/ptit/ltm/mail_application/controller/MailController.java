package com.ptit.ltm.mail_application.controller;

import com.google.gson.Gson;
import com.ptit.ltm.mail_application.dto.SendMailRequest;
import com.ptit.ltm.mail_application.facade.impl.MailFacadeServiceImpl;
import com.ptit.ltm.mail_application.model.Email;
import com.ptit.ltm.mail_application.model.MailContent;
import com.ptit.ltm.mail_application.utils.Utils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MailController {
    private final MailFacadeServiceImpl mailFacadeService;

    @GetMapping
    public String home(Model model, HttpSession httpSession) {
        log.info("(home) bat dau vao trang chu - lay ra thu nhan duoc ");

        String username = Objects.nonNull(httpSession.getAttribute("username")) ?
                httpSession.getAttribute("username").toString() : "user2@domain1.com";

        String password = Objects.nonNull(httpSession.getAttribute("password")) ?
                httpSession.getAttribute("password").toString() : "user2";

        List<Email> emails = mailFacadeService.listInboxMail(username, password);
        model.addAttribute("emails", emails);
        return "home";
    }

    @GetMapping("/sent")
    public String sent(Model model, HttpSession httpSession) {
        log.info("(sent) lay ra thu da gui ");

        String username = Objects.nonNull(httpSession.getAttribute("username")) ?
                httpSession.getAttribute("username").toString() : "user2@domain1.com";

        String password = Objects.nonNull(httpSession.getAttribute("password")) ?
                httpSession.getAttribute("password").toString() : "user2";

        List<Email> emails = mailFacadeService.listSentMail(username, password);
        model.addAttribute("emails", emails);
        return "sentMail";
    }

    @GetMapping("/template")
    public String template() {
        return "chooseTemplate";
    }

    @GetMapping("/send")
    public String sendMail(@RequestParam(value = "template", required = false) String template, Model model) throws Exception {
        log.info("Giao dien gui mail");
        Email email = new Email();
        if (template != null) {
            if (template.equals("interview")) {
                String content = new String(Files.readAllBytes(Paths.get("src/main/java/com/ptit/ltm/mail_application/data/interview-mail.json")), "UTF-8");
                email.setSubject("Thư mời phỏng vấn");
                email.setContent(content);
            }
            if (template.equals("announcement")) {
                String content = new String(Files.readAllBytes(Paths.get("src/main/java/com/ptit/ltm/mail_application/data/event-mail.json")), "UTF-8");
                email.setSubject("Thông báo");
                email.setContent(content);
            }
            if (template.equals("invite")) {
                String content = new String(Files.readAllBytes(Paths.get("src/main/java/com/ptit/ltm/mail_application/data/invite-mail.json")), "UTF-8");
                email.setSubject("Thư mời sự kiện");
                email.setContent(content);
            }
            if (template.equals("thanks")) {
                String content = new String(Files.readAllBytes(Paths.get("src/main/java/com/ptit/ltm/mail_application/data/thanks-mail.json")), "UTF-8");
                email.setSubject("Lời cảm ơn");
                email.setContent(content);
            }
        }
        model.addAttribute("email", email);
        return "sendMail";
    }

    @PostMapping("/send")
    public String sendMail(@Valid SendMailRequest sendMailRequest, Error error, HttpSession httpSession) {

        String username = Objects.nonNull(httpSession.getAttribute("username")) ?
                httpSession.getAttribute("username").toString() : "user2@domain1.com";

        String password = Objects.nonNull(httpSession.getAttribute("password")) ?
                httpSession.getAttribute("password").toString() : "user2";

        mailFacadeService.sendMail(sendMailRequest, null, username, password);
        return "redirect:/sent";
    }

    @GetMapping("/multi_send")
    public String sendMultiMail() {
        return "sendMultiMail";
    }

    @PostMapping("/multi_send")
    public String sendMultiMail(@RequestParam("file") MultipartFile excelFile, HttpSession httpSession) throws Exception {
        String username = Objects.nonNull(httpSession.getAttribute("username")) ?
                httpSession.getAttribute("username").toString() : "user2@domain1.com";

        String password = Objects.nonNull(httpSession.getAttribute("password")) ?
                httpSession.getAttribute("password").toString() : "user2";

        XSSFWorkbook workbook = new XSSFWorkbook(excelFile.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);

        for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
            SendMailRequest request = new SendMailRequest();
            XSSFRow row = worksheet.getRow(i);
            String templateURL = row.getCell(0).getStringCellValue();
            String template = Utils.getQueryParams(new URL(templateURL), "template");
            String content = null;
            if (template.equals("interview")) {
                content = new String(Files.readAllBytes(Paths.get("src/main/java/com/ptit/ltm/mail_application/data/interview-mail.json")), "UTF-8");
                request.setSubject("Thư mời phỏng vấn");
            }
            if (template.equals("announcement")) {
                content = new String(Files.readAllBytes(Paths.get("src/main/java/com/ptit/ltm/mail_application/data/event-mail.json")), "UTF-8");
                request.setSubject("Thông báo");
            }
            if (template.equals("invite")) {
                content = new String(Files.readAllBytes(Paths.get("src/main/java/com/ptit/ltm/mail_application/data/invite-mail.json")), "UTF-8");
                request.setSubject("Thư mời sự kiện");
            }
            if (template.equals("thanks")) {
                content = new String(Files.readAllBytes(Paths.get("src/main/java/com/ptit/ltm/mail_application/data/thanks-mail.json")), "UTF-8");
                request.setSubject("Lời cảm ơn");
            }
            if (content != null) {
                JSONObject jsonObject = new JSONObject(content);
                Gson gson = new Gson();
                MailContent mailContent = gson.fromJson(jsonObject.toString(), MailContent.class);
                for (MailContent.Row mailContentRow : mailContent.getOps()) {
                    for (int j = 2; j < row.getLastCellNum(); j++) {
                        XSSFRow labelRow = worksheet.getRow(0);
                        String label = labelRow.getCell(j).getStringCellValue();
                        String replacement = row.getCell(j).getStringCellValue();
                        mailContentRow.setInsert(mailContentRow.getInsert().replaceAll("\\[" + label + "\\]", replacement));
                    }
                }
                request.setContent(gson.toJson(mailContent));
                request.setToAddress(row.getCell(1).getStringCellValue());
                mailFacadeService.sendMail(request, null, username, password);
            }
        }
        return "redirect:/sent";
    }

    @GetMapping("/spam")
    public String listSpamMail(Model model, HttpSession httpSession) {
        log.info("(listSpamMail) lay ra thu spam ");

        String username = Objects.nonNull(httpSession.getAttribute("username")) ?
                httpSession.getAttribute("username").toString() : "user2@domain1.com";

        String password = Objects.nonNull(httpSession.getAttribute("password")) ?
                httpSession.getAttribute("password").toString() : "user2";

        List<Email> emails = mailFacadeService.listSpamMail(username, password);
                model.addAttribute("emails", emails);
                return "sentMail";
        }

        @GetMapping("/detail/{id}")
        public String detailMail(@PathVariable("id") String id, Model model, HttpSession httpSession) {
                log.info("(detailMail) lay ra chi tiet thu ");

                return "detailMail";
        }
}
