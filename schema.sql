-- ==============================================================================
-- 1. FILE KHỞI TẠO CẤU TRÚC BẢNG (SCHEMA DEFINITION)
-- DỰ ÁN: LTM MAIL CLIENT - SERVER (PTIT)
-- Hướng dẫn: Chạy file này ĐẦU TIÊN để tạo Database và các Bảng
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS `mail_db` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `mail_db`;

-- Xóa các bảng cũ nếu tồn tại theo thứ tự ràng buộc khóa ngoại
DROP TABLE IF EXISTS `contacts`;
DROP TABLE IF EXISTS `email_logs`;
DROP TABLE IF EXISTS `spam_rules`;
DROP TABLE IF EXISTS `email_templates`;
DROP TABLE IF EXISTS `users`;

-- 1. BẢNG USERS (Tài khoản người dùng)
CREATE TABLE `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(100) NOT NULL UNIQUE COMMENT 'Tên đăng nhập',
    `password` VARCHAR(255) NOT NULL COMMENT 'Mật khẩu',
    `full_name` VARCHAR(150) COMMENT 'Họ và tên',
    `email` VARCHAR(150) COMMENT 'Địa chỉ email',
    `phone_number` VARCHAR(30) COMMENT 'Số điện thoại',
    `address` VARCHAR(255) COMMENT 'Địa chỉ',
    `role` VARCHAR(50) DEFAULT 'ROLE_USER' COMMENT 'Quyền hạn: ROLE_USER, ROLE_ADMIN',
    `status` VARCHAR(30) DEFAULT 'ACTIVE' COMMENT 'Trạng thái: ACTIVE, INACTIVE, BLOCKED',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. BẢNG SPAM_RULES (Bộ quy tắc lọc thư rác)
CREATE TABLE `spam_rules` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `rule_type` VARCHAR(50) NOT NULL COMMENT 'Loại quy tắc: DOMAIN_BLOCK, SENDER_BLOCK, KEYWORD_BLOCK',
    `pattern` VARCHAR(255) NOT NULL COMMENT 'Từ khóa hoặc Domain/Email cần chặn',
    `action` VARCHAR(50) DEFAULT 'MOVE_TO_SPAM' COMMENT 'Hành động: MOVE_TO_SPAM, DELETE_IMMEDIATELY',
    `is_active` TINYINT(1) DEFAULT 1 COMMENT 'Trạng thái kích hoạt: 1=Bật, 0=Tắt',
    `description` VARCHAR(255) COMMENT 'Mô tả quy tắc',
    `user_id` BIGINT COMMENT 'ID người dùng sở hữu (NULL = Quy tắc toàn hệ thống)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_spam_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. BẢNG EMAIL_LOGS (Nhật ký email gửi / nhận / spam)
CREATE TABLE `email_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `message_uuid` VARCHAR(100) COMMENT 'UUID định danh của email',
    `sender` VARCHAR(150) NOT NULL COMMENT 'Email người gửi',
    `recipient` VARCHAR(150) NOT NULL COMMENT 'Email người nhận',
    `subject` VARCHAR(255) COMMENT 'Tiêu đề email',
    `content` LONGTEXT COMMENT 'Nội dung email (Plain text hoặc JSON Delta)',
    `mail_type` VARCHAR(30) DEFAULT 'SENT' COMMENT 'Loại thư: INBOX, SENT, SPAM, DRAFT',
    `has_attachment` TINYINT(1) DEFAULT 0 COMMENT 'Có file đính kèm: 1=Có, 0=Không',
    `attachment_name` VARCHAR(255) COMMENT 'Tên file đính kèm',
    `is_spam` TINYINT(1) DEFAULT 0 COMMENT 'Là thư rác: 1=Đúng, 0=Sai',
    `is_read` TINYINT(1) DEFAULT 0 COMMENT 'Đã đọc: 1=Đã đọc, 0=Chưa đọc',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT 'Đã xóa vào thùng rác: 1=Đã xóa, 0=Chưa xóa',
    `deleted_at` DATETIME DEFAULT NULL COMMENT 'Thời gian chuyển vào thùng rác',
    `user_id` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_email_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. BẢNG CONTACTS (Danh bạ liên hệ cá nhân)
CREATE TABLE `contacts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(150) NOT NULL COMMENT 'Tên liên hệ',
    `email` VARCHAR(150) NOT NULL COMMENT 'Địa chỉ email',
    `phone_number` VARCHAR(30) COMMENT 'Số điện thoại',
    `note` VARCHAR(255) COMMENT 'Ghi chú (chức vụ, phòng ban, quan hệ)',
    `user_id` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_contact_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. BẢNG EMAIL_TEMPLATES (Mẫu email soạn sẵn)
CREATE TABLE `email_templates` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `template_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã template: interview, announcement, invite, thanks',
    `title` VARCHAR(150) NOT NULL COMMENT 'Tên mẫu',
    `subject` VARCHAR(255) COMMENT 'Tiêu đề mặc định',
    `content_json` LONGTEXT COMMENT 'Nội dung mẫu',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SHOW TABLES;
