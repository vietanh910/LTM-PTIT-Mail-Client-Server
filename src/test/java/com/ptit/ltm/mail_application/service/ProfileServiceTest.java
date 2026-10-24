package com.ptit.ltm.mail_application.service;

import com.ptit.ltm.mail_application.dto.ProfileForm;
import com.ptit.ltm.mail_application.entity.User;
import com.ptit.ltm.mail_application.repository.UserRepository;
import com.ptit.ltm.mail_application.service.impl.HMailPasswordService;
import com.ptit.ltm.mail_application.service.impl.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock UserRepository repository;
    @Mock HMailPasswordService mailPasswords;
    ProfileService service;
    User user;
    ProfileForm form;

    @BeforeEach
    void setup() {
        service = new ProfileService(repository, mailPasswords);
        user = User.builder().id(7L).username("vuong").email("vuong@domain1.com")
                .password("old123").fullName("Tên cũ").phoneNumber("0901234567").build();
        form = new ProfileForm();
        form.setFullName("  Nguyễn Văn Vương  ");
        form.setPhoneNumber("0912345678");
        lenient().when(repository.findById(7L)).thenReturn(Optional.of(user));
    }

    private void update(MockMultipartFile image, boolean remove) {
        service.update(7L, user.getEmail(), "old123", form, image, remove);
    }

    @Test
    void savesProfileWithoutChangingPassword() {
        user.setAvatar(new byte[]{1, 2, 3});
        update(null, false);
        assertThat(user.getFullName()).isEqualTo("Nguyễn Văn Vương");
        assertThat(user.getPhoneNumber()).isEqualTo("0912345678");
        assertThat(user.getPassword()).isEqualTo("old123");
        assertThat(user.getAvatar()).containsExactly((byte) 1, (byte) 2, (byte) 3);
        verify(repository).saveAndFlush(user);
        verifyNoInteractions(mailPasswords);
    }

    @Test
    void rejectsWrongCurrentPasswordBeforeSaving() {
        form.setCurrentPassword("wrong");
        form.setNewPassword("new123");
        form.setConfirmPassword("new123");
        assertThatThrownBy(() -> update(null, false)).hasMessageContaining("hiện tại không chính xác");
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(mailPasswords);
    }

    @Test
    void rejectsMismatchedOrShortNewPassword() {
        form.setCurrentPassword("old123");
        form.setNewPassword("new123");
        form.setConfirmPassword("other123");
        assertThatThrownBy(() -> update(null, false)).hasMessageContaining("không khớp");
        form.setNewPassword("short");
        form.setConfirmPassword("short");
        assertThatThrownBy(() -> update(null, false)).hasMessageContaining("ít nhất 6");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void synchronizesNewPasswordWithMailServer() {
        form.setCurrentPassword("old123");
        form.setNewPassword("new'123$()");
        form.setConfirmPassword("new'123$()");
        update(null, false);
        assertThat(user.getPassword()).isEqualTo("new'123$()");
        verify(mailPasswords).changePassword("vuong@domain1.com", "new'123$()");
    }

    @Test
    void propagatesMailFailureForTransactionRollback() {
        form.setCurrentPassword("old123");
        form.setNewPassword("new123");
        form.setConfirmPassword("new123");
        doThrow(new IllegalStateException("hMailServer unavailable"))
                .when(mailPasswords).changePassword(anyString(), anyString());
        assertThatThrownBy(() -> update(null, false)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void storesValidatedImageAsPngAndAllowsRemoval() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "jpeg", output);
        update(new MockMultipartFile("avatar", "photo.jpg", "image/jpeg", output.toByteArray()), false);
        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(user.getAvatar()))).isNotNull();
        assertThat(user.getAvatar()).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
        update(null, true);
        assertThat(user.getAvatar()).isNull();
    }

    @Test
    void rejectsFakeImageAndOversizedUploadBeforeSaving() {
        assertThatThrownBy(() -> update(new MockMultipartFile("avatar", "fake.png", "image/png",
                "<script>bad</script>".getBytes()), false)).hasMessageContaining("hợp lệ");
        assertThatThrownBy(() -> update(new MockMultipartFile("avatar", "large.png", "image/png",
                new byte[2 * 1024 * 1024 + 1]), false)).hasMessageContaining("2 MB");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsImageWithTooManyPixelsEvenWhenFileIsSmall() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2001, 2000, BufferedImage.TYPE_INT_RGB), "png", output);
        assertThatThrownBy(() -> update(new MockMultipartFile("avatar", "large.png", "image/png",
                output.toByteArray()), false)).hasMessageContaining("4 triệu");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void supportsAccountThatPreviouslyExistedOnlyOnMailServer() {
        when(repository.findByEmail("mailonly@domain1.com")).thenReturn(Optional.empty());
        service.update(null, "mailonly@domain1.com", "old123", form, null, false);
        verify(repository).saveAndFlush(argThat(saved -> saved.getEmail().equals("mailonly@domain1.com")
                && saved.getUsername().equals("mailonly@domain1.com") && saved.getPassword().equals("old123")));
    }
}
