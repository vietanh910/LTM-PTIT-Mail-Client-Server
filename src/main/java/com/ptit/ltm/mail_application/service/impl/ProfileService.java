package com.ptit.ltm.mail_application.service.impl;

import com.ptit.ltm.mail_application.dto.ProfileForm;
import com.ptit.ltm.mail_application.entity.User;
import com.ptit.ltm.mail_application.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepository;
    private final HMailPasswordService hMailPasswordService;

    public Optional<User> findUser(Long userId, String email) {
        if (userId != null) {
            return userRepository.findById(userId);
        }
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email);
    }

    @Transactional
    public Long update(Long userId, String email, String sessionPassword, ProfileForm form,
                       MultipartFile avatar, boolean removeAvatar) {
        User user = findUser(userId, email).orElseGet(() -> User.builder()
                .username(email).email(email).password(sessionPassword).build());
        boolean changePassword = form.getNewPassword() != null && !form.getNewPassword().isEmpty();
        if (changePassword) {
            if (!java.util.Objects.equals(user.getPassword(), form.getCurrentPassword())) {
                throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
            }
            if (form.getNewPassword().trim().length() < 6) {
                throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
            }
            if (!form.getNewPassword().equals(form.getConfirmPassword())) {
                throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp.");
            }
        } else if ((form.getCurrentPassword() != null && !form.getCurrentPassword().isEmpty())
                || (form.getConfirmPassword() != null && !form.getConfirmPassword().isEmpty())) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu mới để đổi mật khẩu.");
        }

        byte[] image = avatar != null && !avatar.isEmpty() ? readAvatar(avatar) : null;
        user.setFullName(form.getFullName().trim());
        user.setPhoneNumber(form.getPhoneNumber() == null ? "" : form.getPhoneNumber().trim());
        if (image != null) {
            user.setAvatar(image);
        } else if (removeAvatar) {
            user.setAvatar(null);
        }
        if (changePassword) {
            user.setPassword(form.getNewPassword());
        }
        userRepository.saveAndFlush(user);
        if (changePassword) {
            hMailPasswordService.changePassword(email, form.getNewPassword());
        }
        return user.getId();
    }

    private byte[] readAvatar(MultipartFile file) {
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("Ảnh đại diện không được vượt quá 2 MB.");
        }
        try (InputStream source = file.getInputStream();
             ImageInputStream input = ImageIO.createImageInputStream(source)) {
            if (input == null) {
                throw new IllegalArgumentException("Vui lòng chọn ảnh JPG hoặc PNG hợp lệ.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Vui lòng chọn ảnh JPG hoặc PNG hợp lệ.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")) {
                    throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG hoặc PNG.");
                }
                reader.setInput(input);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 4_000_000) {
                    throw new IllegalArgumentException("Ảnh quá lớn. Vui lòng chọn ảnh tối đa 4 triệu điểm ảnh.");
                }
                BufferedImage image = reader.read(0);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                return output.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc ảnh. Vui lòng chọn ảnh JPG hoặc PNG hợp lệ.", e);
        }
    }
}
