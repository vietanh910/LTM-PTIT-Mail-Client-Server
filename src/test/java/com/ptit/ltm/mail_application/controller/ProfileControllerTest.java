package com.ptit.ltm.mail_application.controller;

import com.ptit.ltm.mail_application.dto.ProfileForm;
import com.ptit.ltm.mail_application.entity.User;
import com.ptit.ltm.mail_application.service.impl.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ProfileService profiles;
    MockHttpSession session;

    @BeforeEach
    void setup() {
        session = new MockHttpSession();
        session.setAttribute("username", "vuong@domain1.com");
        session.setAttribute("password", "old123");
        session.setAttribute("profileUserId", 7L);
        session.setAttribute("profileCsrfToken", "test-token");
    }

    @Test
    void requiresLogin() throws Exception {
        mvc.perform(get("/profile")).andExpect(redirectedUrl("/login"));
        mvc.perform(multipart("/profile").param("fullName", "Vương")).andExpect(redirectedUrl("/login"));
        verifyNoInteractions(profiles);
    }

    @Test
    void rendersSavedProfileAndMenuLinkAboveLogout() throws Exception {
        when(profiles.findUser(7L, "vuong@domain1.com")).thenReturn(Optional.of(User.builder()
                .fullName("Nguyễn Vương").phoneNumber("0912345678").build()));
        String html = mvc.perform(get("/profile").session(session))
                .andExpect(status().isOk()).andExpect(view().name("profile"))
                .andExpect(content().string(containsString("Nguyễn Vương")))
                .andExpect(content().string(containsString("0912345678")))
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("href=\"/profile\"")).isLessThan(html.indexOf("href=\"/logout\""));
        assertThat(html).doesNotContain("old123");
    }

    @Test
    void rejectsMissingCsrfToken() throws Exception {
        mvc.perform(multipart("/profile").session(session).param("fullName", "Vương"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("error"));
        verifyNoInteractions(profiles);
    }

    @Test
    void rejectsInvalidNameAndPhone() throws Exception {
        mvc.perform(multipart("/profile").session(session).param("csrfToken", "test-token")
                        .param("fullName", " ").param("phoneNumber", "abc"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("profileForm", "fullName", "phoneNumber"));
        verifyNoInteractions(profiles);
    }

    @Test
    void updatesSessionPasswordOnlyAfterSuccessfulSave() throws Exception {
        when(profiles.update(eq(7L), anyString(), anyString(), any(ProfileForm.class), isNull(), eq(false)))
                .thenReturn(7L);
        mvc.perform(multipart("/profile").session(session).param("csrfToken", "test-token")
                        .param("fullName", "Vương").param("currentPassword", "old123")
                        .param("newPassword", "new123").param("confirmPassword", "new123"))
                .andExpect(redirectedUrl("/profile")).andExpect(flash().attributeExists("message"));
        assertThat(session.getAttribute("password")).isEqualTo("new123");
    }

    @Test
    void keepsSessionPasswordWhenMailServerRejectsUpdate() throws Exception {
        when(profiles.update(eq(7L), anyString(), anyString(), any(ProfileForm.class), isNull(), eq(false)))
                .thenThrow(new IllegalStateException("Máy chủ thư không khả dụng"));
        mvc.perform(multipart("/profile").session(session).param("csrfToken", "test-token")
                        .param("fullName", "Vương").param("currentPassword", "old123")
                        .param("newPassword", "new123").param("confirmPassword", "new123"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("error"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("new123"))));
        assertThat(session.getAttribute("password")).isEqualTo("old123");
    }

    @Test
    void servesOnlyLoggedInUsersAvatarWithoutCaching() throws Exception {
        when(profiles.findUser(7L, "vuong@domain1.com"))
                .thenReturn(Optional.of(User.builder().avatar(new byte[]{1, 2, 3}).build()));
        mvc.perform(get("/profile/avatar").session(session)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/png")).andExpect(content().bytes(new byte[]{1, 2, 3}))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/profile/avatar")).andExpect(redirectedUrl("/image/user.jpeg"));
    }
}
