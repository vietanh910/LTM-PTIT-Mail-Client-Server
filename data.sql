-- ==============================================================================
-- 2. FILE NẠP DỮ LIỆU MẪU (DATA SEEDING)
-- DỰ ÁN: LTM MAIL CLIENT - SERVER (PTIT)
-- Hướng dẫn: Chạy file này SAU KHI đã chạy file schema.sql
-- ==============================================================================

USE `mail_db`;

-- 1. Nạp danh sách người dùng (Users)
INSERT IGNORE INTO `users` (`id`, `username`, `password`, `full_name`, `email`, `phone_number`, `address`, `role`, `status`) VALUES
(1, 'admin', '123456', 'Quản Trị Viên Hệ Thống', 'admin@domain1.com', '0901234567', 'Hà Nội', 'ROLE_ADMIN', 'ACTIVE'),
(2, 'user1', '123456', 'Nguyễn Văn An', 'user1@domain1.com', '0912345678', 'Hà Nội', 'ROLE_USER', 'ACTIVE'),
(3, 'user2', '123456', 'Trần Thị Bình', 'user2@domain1.com', '0987654321', 'Hồ Chí Minh', 'ROLE_USER', 'ACTIVE'),
(4, 'user3', '123456', 'Lê Hoàng Cường', 'user3@domain1.com', '0933445566', 'Đà Nẵng', 'ROLE_USER', 'ACTIVE'),
(5, 'nguyenvana', '123456', 'Nguyễn Văn A', 'nguyenvana@gmail.com', '0977889900', 'Hải Phòng', 'ROLE_USER', 'ACTIVE'),
(6, 'tranthib', '123456', 'Trần Thị B', 'tranthib@gmail.com', '0966112233', 'Cần Thơ', 'ROLE_USER', 'ACTIVE'),
(7, 'lethic', '123456', 'Lê Thị Cẩm', 'lethic@domain1.com', '0944556677', 'Huế', 'ROLE_USER', 'ACTIVE'),
(8, 'phamvand', '123456', 'Phạm Văn Dũng', 'phamvand@domain1.com', '0911223344', 'Nghệ An', 'ROLE_USER', 'ACTIVE');

-- 2. Nạp danh sách quy tắc lọc thư rác (Spam Rules)
INSERT IGNORE INTO `spam_rules` (`id`, `rule_type`, `pattern`, `action`, `is_active`, `description`) VALUES
(1, 'KEYWORD_BLOCK', 'trúng thưởng', 'MOVE_TO_SPAM', 1, 'Chặn thư lừa đảo thông báo trúng thưởng'),
(2, 'KEYWORD_BLOCK', 'khuyến mãi sốc', 'MOVE_TO_SPAM', 1, 'Chặn thư quảng cáo khuyến mãi làm phiền'),
(3, 'KEYWORD_BLOCK', 'vay tiền nhanh', 'MOVE_TO_SPAM', 1, 'Chặn thư rác tài chính tín dụng đen'),
(4, 'KEYWORD_BLOCK', 'kiếm tiền online', 'MOVE_TO_SPAM', 1, 'Chặn thư lừa đảo việc làm tại nhà'),
(5, 'KEYWORD_BLOCK', 'ưu đãi khủng', 'MOVE_TO_SPAM', 1, 'Chặn thư quảng cáo tiếp thị hàng loạt'),
(6, 'DOMAIN_BLOCK', 'spam-domain.com', 'MOVE_TO_SPAM', 1, 'Chặn toàn bộ email gửi từ domain rác spam-domain.com'),
(7, 'DOMAIN_BLOCK', 'fake-news.xyz', 'MOVE_TO_SPAM', 1, 'Chặn domain tin giả và mã độc'),
(8, 'DOMAIN_BLOCK', 'quangcao-247.net', 'MOVE_TO_SPAM', 1, 'Chặn domain dịch vụ quảng cáo'),
(9, 'SENDER_BLOCK', 'bot-spammer@random.org', 'MOVE_TO_SPAM', 1, 'Chặn email của bot tự động'),
(10, 'SENDER_BLOCK', 'offer-noreply@promo-center.info', 'MOVE_TO_SPAM', 1, 'Chặn email marketing không phản hồi');

-- 3. Nạp danh bạ người liên hệ (Contacts)
INSERT IGNORE INTO `contacts` (`id`, `name`, `email`, `phone_number`, `note`, `user_id`) VALUES
(1, 'Trần Thị Bình', 'user2@domain1.com', '0987654321', 'Đồng nghiệp phòng Kỹ thuật', 2),
(2, 'Lê Hoàng Cường', 'user3@domain1.com', '0933445566', 'Trưởng nhóm phát triển LTM', 2),
(3, 'Nguyễn Văn An', 'user1@domain1.com', '0912345678', 'Chuyên viên quản trị mạng', 3),
(4, 'Phạm Văn Dũng', 'phamvand@domain1.com', '0911223344', 'Đối tác dự án Mail Server', 3),
(5, 'Giảng Viên LTM PTIT', 'gv_ltm@ptit.edu.vn', '0243756218', 'Giáo viên hướng dẫn môn học LTM', 1),
(6, 'Lê Thị Cẩm', 'lethic@domain1.com', '0944556677', 'Nhân viên kiểm thử chất lượng (QA)', 2),
(7, 'Hỗ Trợ Kỹ Thuật', 'support@domain1.com', '19001000', 'Bộ phận IT Helpdesk', 1),
(8, 'Nguyễn Văn A', 'nguyenvana@gmail.com', '0977889900', 'Khách hàng đối tác ngoài', 2);

-- 4. Nạp danh sách mẫu email (Email Templates)
INSERT IGNORE INTO `email_templates` (`id`, `template_code`, `title`, `subject`, `content_json`) VALUES
(1, 'interview', 'Thư mời phỏng vấn', 'Thư mời phỏng vấn - Vị trí Lập trình viên Java', '{"ops":[{"insert":"Kính gửi ứng viên,\n\nChúng tôi trân trọng mời bạn tham dự buổi phỏng vấn vị trí Kỹ sư phần mềm.\n- Thời gian: 09:00 AM\n- Địa điểm: Tầng 5, Tòa nhà PTIT, Hà Nội.\n\nTrân trọng!\nBan Tuyển Dụng"}]}'),
(2, 'announcement', 'Thông báo nội bộ', 'Thông báo về kế hoạch bảo trì hệ thống máy chủ', '{"ops":[{"insert":"Kính gửi toàn thể cán bộ nhân viên,\n\nPhòng Công nghệ thông tin xin thông báo kế hoạch nâng cấp và bảo trì hệ thống Mail Server vào cuối tuần này.\n\nTrân trọng thông báo!"}]}'),
(3, 'invite', 'Thư mời sự kiện', 'Thư mời tham dự Hội thảo Công nghệ Mạng LTM', '{"ops":[{"insert":"Trân trọng kính mời quý đối tác và sinh viên tham dự buổi báo cáo đồ án Lập trình mạng.\n- Địa điểm: Hội trường A2 - PTIT\n\nRất hân hạnh được đón tiếp!"}]}'),
(4, 'thanks', 'Lời cảm ơn', 'Thư cảm ơn Quý đối tác đã đồng hành cùng dự án', '{"ops":[{"insert":"Kính gửi Quý đối tác,\n\nChúng tôi xin gửi lời cảm ơn chân thành nhất vì sự hợp tác quý báu của Quý công ty trong suốt thời gian qua.\n\nKính chúc Quý đối tác luôn thành công và phát triển!"}]}');

-- 5. Nạp lịch sử nhật ký email (Email Logs)
INSERT IGNORE INTO `email_logs` (`id`, `message_uuid`, `sender`, `recipient`, `subject`, `content`, `mail_type`, `has_attachment`, `attachment_name`, `is_spam`, `user_id`) VALUES
(1, UUID(), 'user1@domain1.com', 'user2@domain1.com', 'Chào bạn, gửi tài liệu báo cáo LTM', 'Chào Bình, mình gửi kèm file tài liệu phân tích hệ thống Mail Client nhé.', 'SENT', 1, 'Bao_Cao_LTM_Nhom_14.pdf', 0, 2),
(2, UUID(), 'user2@domain1.com', 'user1@domain1.com', 'Re: Chào bạn, gửi tài liệu báo cáo LTM', 'Mình đã nhận được tài liệu rồi nhé An, cảm ơn bạn rất nhiều!', 'INBOX', 0, NULL, 0, 2),
(3, UUID(), 'user1@domain1.com', 'user3@domain1.com', 'Thư mời tham gia buổi họp đồ án', 'Kính mời bạn Cường tham gia buổi họp trực tuyến lúc 15h hôm nay.', 'SENT', 0, NULL, 0, 2),
(4, UUID(), 'spammer@spam-domain.com', 'user2@domain1.com', 'Chúc mừng bạn đã trúng thưởng iPhone 15 Pro!', 'Bạn đã may mắn nhận được phần thưởng trị giá 30 triệu đồng, bấm vào link để nhận quà.', 'SPAM', 0, NULL, 1, 3),
(5, UUID(), 'promo@quangcao-247.net', 'user1@domain1.com', 'Khuyến mãi sốc dịch vụ lưu trữ đám mây', 'Giảm giá 50% toàn bộ gói máy chủ mail trong tuần lễ vàng.', 'SPAM', 0, NULL, 1, 2),
(6, UUID(), 'admin@domain1.com', 'user1@domain1.com', 'Cấp quyền truy cập hệ thống Mail Server', 'Tài khoản của bạn đã được kích hoạt thành công trên máy chủ hMailServer.', 'INBOX', 0, NULL, 0, 2),
(7, UUID(), 'admin@domain1.com', 'user2@domain1.com', 'Cấp quyền truy cập hệ thống Mail Server', 'Tài khoản của bạn đã được kích hoạt thành công trên máy chủ hMailServer.', 'INBOX', 0, NULL, 0, 3),
(8, UUID(), 'user1@domain1.com', 'nguyenvana@gmail.com', 'Thư mời phỏng vấn - Vị trí Lập trình viên Java', 'Kính gửi ứng viên Nguyễn Văn A, chúng tôi trân trọng mời bạn tham dự phỏng vấn...', 'SENT', 1, 'Thong_Tin_Phong_Van.pdf', 0, 2);

-- Hiển thị kết quả nạp dữ liệu
SELECT 'Users' AS Table_Name, COUNT(*) AS Records FROM `users`
UNION ALL
SELECT 'Spam Rules', COUNT(*) FROM `spam_rules`
UNION ALL
SELECT 'Contacts', COUNT(*) FROM `contacts`
UNION ALL
SELECT 'Email Templates', COUNT(*) FROM `email_templates`
UNION ALL
SELECT 'Email Logs', COUNT(*) FROM `email_logs`;
