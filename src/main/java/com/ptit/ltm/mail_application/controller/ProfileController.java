package com.ptit.ltm.mail_application.controller;

import com.ptit.ltm.mail_application.dto.ProfileForm;
import com.ptit.ltm.mail_application.service.impl.ProfileService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController {
    private final ProfileService profileService;

    private Long userId(HttpSession session) {
        return (Long) session.getAttribute("profileUserId");
    }

    private void populate(Model model, HttpSession session) {
        model.addAttribute("currentUser", session.getAttribute("username"));
        if (session.getAttribute("profileCsrfToken") == null) {
            session.setAttribute("profileCsrfToken", UUID.randomUUID().toString());
        }
    }

    @GetMapping
    public String profile(Model model, HttpSession session) {
        if (session.getAttribute("username") == null) {
            return "redirect:/login";
        }
        ProfileForm form = new ProfileForm();
        profileService.findUser(userId(session), (String) session.getAttribute("username")).ifPresent(user -> {
            form.setFullName(user.getFullName());
            form.setPhoneNumber(user.getPhoneNumber());
        });
        model.addAttribute("profileForm", form);
        populate(model, session);
        return "profile";
    }

    @PostMapping
    public String update(@Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult binding,
                         @RequestParam(value = "avatar", required = false) MultipartFile avatar,
                         @RequestParam(value = "removeAvatar", defaultValue = "false") boolean removeAvatar,
                         @RequestParam(value = "csrfToken", required = false) String token,
                         HttpSession session, Model model, RedirectAttributes redirect) {
        if (session.getAttribute("username") == null) {
            return "redirect:/login";
        }
        if (token == null || !token.equals(session.getAttribute("profileCsrfToken"))) {
            model.addAttribute("error", "Phiên biểu mẫu không hợp lệ. Vui lòng tải lại trang và thử lại.");
        } else if (!binding.hasErrors()) {
            try {
                Long savedId = profileService.update(userId(session), (String) session.getAttribute("username"),
                        (String) session.getAttribute("password"), form, avatar, removeAvatar);
                session.setAttribute("profileUserId", savedId);
                if (form.getNewPassword() != null && !form.getNewPassword().isEmpty()) {
                    session.setAttribute("password", form.getNewPassword());
                }
                redirect.addFlashAttribute("message", "Đã cập nhật thông tin cá nhân thành công.");
                return "redirect:/profile";
            } catch (IllegalArgumentException | IllegalStateException e) {
                model.addAttribute("error", e.getMessage());
            } catch (Exception e) {
                log.error("Không thể cập nhật hồ sơ", e);
                model.addAttribute("error", "Không thể lưu thông tin. Vui lòng thử lại.");
            }
        }
        form.setCurrentPassword(null);
        form.setNewPassword(null);
        form.setConfirmPassword(null);
        populate(model, session);
        return "profile";
    }

    @GetMapping("/avatar")
    public ResponseEntity<?> avatar(HttpSession session) {
        if (session.getAttribute("username") != null) {
            byte[] avatar = profileService.findUser(userId(session), (String) session.getAttribute("username"))
                    .map(user -> user.getAvatar()).orElse(null);
            if (avatar != null && avatar.length > 0) {
                return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                        .contentType(MediaType.IMAGE_PNG).body(avatar);
            }
        }
        return ResponseEntity.status(302).cacheControl(CacheControl.noStore())
                .location(URI.create("/image/user.jpeg")).build();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge(RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", "Ảnh đại diện không được vượt quá 2 MB.");
        return "redirect:/profile";
    }
}
