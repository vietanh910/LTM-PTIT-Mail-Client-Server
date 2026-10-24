package com.ptit.ltm.mail_application.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class HMailPasswordService {
    @Value("${hmail.admin-password:123456}")
    private String adminPassword;

    public void changePassword(String email, String password) {
        // Truyền dữ liệu qua environment, không ghép mật khẩu vào mã PowerShell.
        String script = "$ErrorActionPreference = 'Stop'; "
                + "$app = New-Object -ComObject hMailServer.Application; "
                + "$null = $app.Authenticate('Administrator', $env:MAIL_PROFILE_ADMIN); "
                + "$domain = $app.Domains.ItemByName($env:MAIL_PROFILE_EMAIL.Split('@')[1]); "
                + "$account = $domain.Accounts.ItemByAddress($env:MAIL_PROFILE_EMAIL); "
                + "if ($null -eq $account -or $account.ID -le 0) { throw 'Account not found' }; "
                + "$account.Password = $env:MAIL_PROFILE_PASSWORD; $account.Save();";
        ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script);
        builder.environment().put("MAIL_PROFILE_ADMIN", adminPassword);
        builder.environment().put("MAIL_PROFILE_EMAIL", email);
        builder.environment().put("MAIL_PROFILE_PASSWORD", password);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process process = null;
        try {
            process = builder.start();
            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
                throw new IllegalStateException("Không thể đổi mật khẩu trên máy chủ thư. Vui lòng kiểm tra hMailServer và thử lại.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không thể kết nối hMailServer để đổi mật khẩu.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Quá trình đổi mật khẩu bị gián đoạn. Vui lòng thử lại.", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
