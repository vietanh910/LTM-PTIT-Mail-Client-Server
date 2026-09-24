package com.ptit.ltm.mail_application.controller;

import com.google.gson.Gson;
import com.ptit.ltm.mail_application.dto.SendMailRequest;
import com.ptit.ltm.mail_application.entity.EmailLog;
import com.ptit.ltm.mail_application.facade.impl.MailFacadeServiceImpl;
import com.ptit.ltm.mail_application.model.Email;
import com.ptit.ltm.mail_application.model.MailContent;
import com.ptit.ltm.mail_application.repository.EmailLogRepository;
import com.ptit.ltm.mail_application.utils.Utils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MailController {
    private final MailFacadeServiceImpl mailFacadeService;
    private final EmailLogRepository emailLogRepository;

    private String getSessionUsername(HttpSession session) {
        return Objects.nonNull(session.getAttribute("username")) ?
                session.getAttribute("username").toString() : "user2@domain1.com";
    }

    private String getSessionPassword(HttpSession session) {
        return Objects.nonNull(session.getAttribute("password")) ?
                session.getAttribute("password").toString() : "user2";
    }

    private String readTemplateJson(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource("data/" + fileName);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.error("Failed to read template {}: {}", fileName, e.getMessage());
        }
        return "";
    }

    @GetMapping
    public String home(Model model, HttpSession httpSession) {
        log.info("(home) bat dau vao trang chu - lay ra thu nhan duoc");
        String username = getSessionUsername(httpSession);
        String password = getSessionPassword(httpSession);

        List<Email> emails = mailFacadeService.listInboxMail(username, password);
        model.addAttribute("emails", emails);
        model.addAttribute("currentUser", username);
        return "home";
    }

    @GetMapping("/sent")
    public String sent(Model model, HttpSession httpSession) {
        log.info("(sent) lay ra thu da gui");
        String username = getSessionUsername(httpSession);
        String password = getSessionPassword(httpSession);

        List<Email> emails = mailFacadeService.listSentMail(username, password);
        model.addAttribute("emails", emails);
        model.addAttribute("currentUser", username);
        return "sentMail";
    }

    @GetMapping("/template")
    public String template(Model model, HttpSession httpSession) {
        model.addAttribute("currentUser", getSessionUsername(httpSession));
        return "chooseTemplate";
    }

    @GetMapping("/send")
    public String sendMail(@RequestParam(value = "template", required = false) String template,
                           @RequestParam(value = "replyTo", required = false) String replyTo,
                           @RequestParam(value = "subject", required = false) String replySubject,
                           Model model, HttpSession httpSession) {
        log.info("Giao dien gui mail, template={}, replyTo={}", template, replyTo);
        Email email = new Email();
        if (replyTo != null && !replyTo.trim().isEmpty()) {
            email.setToAddress(replyTo);
        }
        if (replySubject != null && !replySubject.trim().isEmpty()) {
            email.setSubject(replySubject);
        }
        if (template != null) {
            if (template.equals("interview")) {
                email.setSubject("Thư mời phỏng vấn");
                email.setContent(readTemplateJson("interview-mail.json"));
            } else if (template.equals("announcement")) {
                email.setSubject("Thông báo");
                email.setContent(readTemplateJson("event-mail.json"));
            } else if (template.equals("invite")) {
                email.setSubject("Thư mời sự kiện");
                email.setContent(readTemplateJson("invite-mail.json"));
            } else if (template.equals("thanks")) {
                email.setSubject("Lời cảm ơn");
                email.setContent(readTemplateJson("thanks-mail.json"));
            }
        }
        model.addAttribute("email", email);
        model.addAttribute("currentUser", getSessionUsername(httpSession));
        return "sendMail";
    }

    @PostMapping("/send")
    public String sendMail(@Valid SendMailRequest sendMailRequest,
                           @RequestParam(value = "file", required = false) MultipartFile file,
                           HttpSession httpSession) {
        String username = getSessionUsername(httpSession);
        String password = getSessionPassword(httpSession);

        mailFacadeService.sendMail(sendMailRequest, file, username, password);
        return "redirect:/sent";
    }

    @GetMapping("/multi_send")
    public String sendMultiMail(Model model, HttpSession httpSession) {
        model.addAttribute("currentUser", getSessionUsername(httpSession));
        return "sendMultiMail";
    }

    @PostMapping("/multi_send")
    public String sendMultiMail(@RequestParam("file") MultipartFile excelFile, HttpSession httpSession) throws Exception {
        String username = getSessionUsername(httpSession);
        String password = getSessionPassword(httpSession);

        try (XSSFWorkbook workbook = new XSSFWorkbook(excelFile.getInputStream())) {
            XSSFSheet worksheet = workbook.getSheetAt(0);

            for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
                XSSFRow row = worksheet.getRow(i);
                if (row == null || row.getCell(0) == null || row.getCell(1) == null) {
                    continue;
                }
                SendMailRequest request = new SendMailRequest();
                String templateURL = row.getCell(0).getStringCellValue();
                String template = Utils.getQueryParams(new URL(templateURL), "template");
                String content = null;

                if ("interview".equals(template)) {
                    content = readTemplateJson("interview-mail.json");
                    request.setSubject("Thư mời phỏng vấn");
                } else if ("announcement".equals(template)) {
                    content = readTemplateJson("event-mail.json");
                    request.setSubject("Thông báo");
                } else if ("invite".equals(template)) {
                    content = readTemplateJson("invite-mail.json");
                    request.setSubject("Thư mời sự kiện");
                } else if ("thanks".equals(template)) {
                    content = readTemplateJson("thanks-mail.json");
                    request.setSubject("Lời cảm ơn");
                }

                if (content != null && !content.isEmpty()) {
                    JSONObject jsonObject = new JSONObject(content);
                    Gson gson = new Gson();
                    MailContent mailContent = gson.fromJson(jsonObject.toString(), MailContent.class);
                    if (mailContent != null && mailContent.getOps() != null) {
                        for (MailContent.Row mailContentRow : mailContent.getOps()) {
                            if (mailContentRow.getInsert() == null) continue;
                            for (int j = 2; j < row.getLastCellNum(); j++) {
                                XSSFRow labelRow = worksheet.getRow(0);
                                if (labelRow != null && labelRow.getCell(j) != null && row.getCell(j) != null) {
                                    String label = labelRow.getCell(j).getStringCellValue();
                                    String replacement = row.getCell(j).getStringCellValue();
                                    mailContentRow.setInsert(mailContentRow.getInsert().replaceAll("\\[" + label + "\\]", replacement));
                                }
                            }
                        }
                    }
                    request.setContent(gson.toJson(mailContent));
                    request.setToAddress(row.getCell(1).getStringCellValue());
                    mailFacadeService.sendMail(request, null, username, password);
                }
            }
        }
        return "redirect:/sent";
    }

    @GetMapping("/spam")
    public String listSpamMail(Model model, HttpSession httpSession) {
        log.info("(listSpamMail) lay ra thu spam");
        String username = getSessionUsername(httpSession);
        String password = getSessionPassword(httpSession);

        List<Email> emails = mailFacadeService.listSpamMail(username, password);
        model.addAttribute("emails", emails);
        model.addAttribute("currentUser", username);
        return "spamMail";
    }

    @GetMapping("/detail/{id}")
    public String detailMail(@PathVariable("id") String id, Model model, HttpSession httpSession) {
        log.info("(detailMail) lay ra chi tiet thu va danh dau da doc: {}", id);
        try {
            Long numericId = Long.parseLong(id);
            emailLogRepository.markAsReadByIds(List.of(numericId));
        } catch (NumberFormatException e) {
            emailLogRepository.markAsReadByUuids(List.of(id));
        } catch (Exception e) {
            log.warn("Khong the danh dau da doc cho thu {}: {}", id, e.getMessage());
        }
        model.addAttribute("currentUser", getSessionUsername(httpSession));
        return "detailMail";
    }

    // =================== THÙNG RÁC ===================

    @GetMapping("/trash")
    public String trash(Model model, HttpSession httpSession) {
        log.info("(trash) lay ra thu da xoa");
        String username = getSessionUsername(httpSession);
        List<EmailLog> deletedLogs = emailLogRepository.findDeletedByOwner(username);
        List<Email> emails = deletedLogs.stream()
                .map(mailFacadeService::convertLogToEmail)
                .toList();
        model.addAttribute("emails", emails);
        model.addAttribute("currentUser", username);
        return "trash";
    }

    // =================== XÓA MỀM HÀNG LOẠT ===================

    @PostMapping("/mail/delete")
    @ResponseBody
    public ResponseEntity<String> deleteMails(@RequestBody List<String> ids, HttpSession httpSession) {
        log.info("(deleteMails) xoa mem {} thu", ids != null ? ids.size() : 0);
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("Không có thư nào được chọn");
        }
        java.util.List<Long> numericIds = new java.util.ArrayList<>();
        java.util.List<String> uuidList = new java.util.ArrayList<>();
        for (String idStr : ids) {
            try {
                numericIds.add(Long.parseLong(idStr));
            } catch (NumberFormatException e) {
                uuidList.add(idStr);
            }
        }
        if (!numericIds.isEmpty()) {
            emailLogRepository.softDeleteByIds(numericIds, LocalDateTime.now());
        }
        if (!uuidList.isEmpty()) {
            emailLogRepository.softDeleteByUuids(uuidList, LocalDateTime.now());
        }
        return ResponseEntity.ok("Đã chuyển " + ids.size() + " thư vào thùng rác");
    }

    // =================== ĐÁNH DẤU ĐÃ ĐỌC HÀNG LOẠT ===================

    @PostMapping("/mail/mark-read")
    @ResponseBody
    public ResponseEntity<String> markAsRead(@RequestBody List<String> ids, HttpSession httpSession) {
        log.info("(markAsRead) danh dau da doc {} thu", ids != null ? ids.size() : 0);
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("Không có thư nào được chọn");
        }
        java.util.List<Long> numericIds = new java.util.ArrayList<>();
        java.util.List<String> uuidList = new java.util.ArrayList<>();
        for (String idStr : ids) {
            try {
                numericIds.add(Long.parseLong(idStr));
            } catch (NumberFormatException e) {
                uuidList.add(idStr);
            }
        }
        if (!numericIds.isEmpty()) {
            emailLogRepository.markAsReadByIds(numericIds);
        }
        if (!uuidList.isEmpty()) {
            emailLogRepository.markAsReadByUuids(uuidList);
        }
        return ResponseEntity.ok("Đã đánh dấu " + ids.size() + " thư là đã đọc");
    }

    // =================== KHÔI PHỤC THƯ TỪ THÙNG RÁC ===================

    @PostMapping("/mail/restore")
    @ResponseBody
    public ResponseEntity<String> restoreMails(@RequestBody List<String> ids, HttpSession httpSession) {
        log.info("(restoreMails) khoi phuc {} thu", ids != null ? ids.size() : 0);
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("Không có thư nào được chọn");
        }
        java.util.List<Long> numericIds = new java.util.ArrayList<>();
        for (String idStr : ids) {
            try {
                numericIds.add(Long.parseLong(idStr));
            } catch (Exception ignored) {}
        }
        if (!numericIds.isEmpty()) {
            emailLogRepository.restoreByIds(numericIds);
        }
        return ResponseEntity.ok("Đã khôi phục " + ids.size() + " thư thành công");
    }
}
