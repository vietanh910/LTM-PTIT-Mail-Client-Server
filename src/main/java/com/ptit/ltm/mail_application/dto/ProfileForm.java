package com.ptit.ltm.mail_application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileForm {
    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 150, message = "Họ và tên không được vượt quá 150 ký tự.")
    private String fullName;

    @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự.")
    @Pattern(regexp = "^$|^\\+?[0-9][0-9 ()-]{5,29}$", message = "Số điện thoại không hợp lệ.")
    private String phoneNumber;

    @Size(max = 255, message = "Mật khẩu không được vượt quá 255 ký tự.")
    private String currentPassword;

    @Size(max = 255, message = "Mật khẩu không được vượt quá 255 ký tự.")
    private String newPassword;

    @Size(max = 255, message = "Mật khẩu không được vượt quá 255 ký tự.")
    private String confirmPassword;
}
