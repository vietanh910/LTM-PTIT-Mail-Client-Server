# TỔNG KẾT KHẮC PHỤC & HƯỚNG DẪN VẬN HÀNH HỆ THỐNG LTM MAIL CLIENT - SERVER

---

## I. CƠ SỞ DỮ LIỆU MYSQL & HỆ THỐNG ENTITY (MỚI TÍCH HỢP)

Hệ thống đã được tích hợp cơ sở dữ liệu **MySQL Server** (`mail_db`) thông qua **Spring Data JPA & Hibernate**:
* **Database Name**: `mail_db` (UTF-8 MB4)
* **Cổng kết nối**: `3306` (User: `root`, Mật khẩu: `123456`)

### Danh sách các Entity được thiết kế đầy đủ:
1. **`User` (`users`)**:
   - Quản lý tài khoản người dùng: `username`, `password`, `fullName`, `email`, `phoneNumber`, `address`, `role`, `status`, `createdAt`, `updatedAt`.
   - Kết nối với form Đăng ký (`/register`) và Đăng nhập (`/login`).
2. **`SpamRule` (`spam_rules`)**:
   - Quản lý quy tắc lọc thư rác theo đúng yêu cầu đề tài:
     - `DOMAIN_BLOCK`: Chặn thư theo domain gửi (ví dụ: `spam-domain.com`).
     - `SENDER_BLOCK`: Chặn thư theo địa chỉ email nguồn.
     - `KEYWORD_BLOCK`: Chặn thư dựa trên từ khóa trong tiêu đề/nội dung (ví dụ: `trúng thưởng`, `khuyến mãi sốc`).
3. **`EmailLog` (`email_logs`)**:
   - Lưu vết lịch sử gửi/nhận email trong database: `sender`, `recipient`, `subject`, `content`, `mailType` (INBOX/SENT/SPAM), `hasAttachment`, `attachmentName`, `createdAt`.
4. **`Contact` (`contacts`)**:
   - Quản lý danh bạ liên hệ cá nhân của người dùng.
5. **`EmailTemplate` (`email_templates`)**:
   - Lưu trữ các mẫu thư động (`interview`, `announcement`, `invite`, `thanks`).

### Khởi tạo dữ liệu tự động (`DataInitService`):
Khi ứng dụng khởi chạy lần đầu, `DataInitService` sẽ tự động tạo sẵn:
* 2 tài khoản mẫu: `user1` (pass: `user1`, email: `user1@domain1.com`) và `user2` (pass: `user2`, email: `user2@domain1.com`).
* 3 quy tắc lọc spam mẫu trong database.

---

## II. TỔNG HỢP CÁC CẢI TIẾN & KHẮC PHỤC ĐÃ THỰC HIỆN

### 1. Chuẩn hóa Dependencies & Nền tảng Spring Boot 3 (`pom.xml`)
* Loại bỏ các thư viện cũ gây xung đột: `javax.mail:mail:1.4.7`, `javax.validation:validation-api:2.0.1.Final`, `springdoc-openapi-ui:1.7.0`.
* Thêm `spring-boot-starter-data-jpa` và `mysql-connector-j`.
* Bổ sung `org.eclipse.angus:jakarta.mail` hỗ trợ đầy đủ bộ giao thức **SMTP**, **IMAP**, **IMAPS** trên nền Spring Boot 3.
* Chuyển đổi toàn bộ imports sang `jakarta.*`.

### 2. Xử lý & Khắc phục Lỗi Backend / Logic
* **Bộ lọc Spam thông minh (`SpamFilterService`)**: Tự động đối soát các email nhận từ IMAP với bộ quy tắc trong bảng `spam_rules` để chuyển thư vào mục Spam.
* **Ghi nhật ký gửi thư**: Tự động lưu bản ghi vào bảng `email_logs` mỗi khi gửi thư thành công.
* **Trích xuất Email & Đọc nội dung an toàn**: Hỗ trợ Plain text, HTML và Multipart message (thư có file đính kèm), phòng tránh lỗi Null Pointer.
* **Đọc tài nguyên Template bằng `ClassPathResource`**: Ổn định khi đóng gói thành file JAR.
* **Sửa các Endpoint Controller**: Sửa `/spam` trả về đúng view `spamMail`, thêm `/logout` và xử lý form `POST /register` lưu trực tiếp vào MySQL.

### 3. Khắc phục Lỗi Giao diện & JavaScript (Frontend Thymeleaf)
* **Gửi file đính kèm trên Web (`sendMail.html`)**: Thêm `enctype="multipart/form-data"` và ô chọn file đính kèm.
* **Tính năng Trả lời thư (`detailMail.html`)**: Thêm nút **Trả lời thư (Reply)** tự động điền email nhận và tiền tố `Re:`.
* **Khắc phục lỗi Crash JavaScript**: Sửa lỗi off-by-one trong vòng lặp JavaScript, bọc `JSON.parse` an toàn cho Quill Editor trong tất cả các trang (`home.html`, `sentMail.html`, `spamMail.html`, `detailMail.html`).
* **Header**: Hiển thị chính xác email tài khoản đang đăng nhập.

---

## III. HƯỚNG DẪN CÀI ĐẶT & CHẠY THỬ NGHIỆM HỆ THỐNG

### Bước 1: Khởi động MySQL Database
* Database `mail_db` đã được tạo sẵn trên MySQL Server của bạn (Port `3306`, User: `root`, Pass: `123456`).
* Hibernate sẽ tự động tạo/cập nhật bảng khi ứng dụng chạy (`ddl-auto: update`).

### Bước 2: Cài đặt và cấu hình hMailServer (hoặc dùng Gmail)
1. Tải và cài đặt **hMailServer** từ [https://www.hmailserver.com/download](https://www.hmailserver.com/download).
2. Tạo Domain `domain1.com` và 2 tài khoản: `user1@domain1.com` / `user1`, `user2@domain1.com` / `user2`.
3. Đảm bảo cổng SMTP (25 hoặc 587) và IMAP (143) đang mở.

### Bước 3: Chạy ứng dụng Spring Boot
1. Mở dự án trong **IntelliJ IDEA** (hoặc Eclipse / VS Code).
2. Chạy file khởi động: `MailApplication.java`.
3. Mở trình duyệt và truy cập: `http://localhost:8080`.

### Bước 4: Kiểm tra các luồng nghiệp vụ
1. **Đăng ký tài khoản mới**: Vào `/register`, điền thông tin -> dữ liệu sẽ được lưu trực tiếp vào MySQL bảng `users`.
2. **Đăng nhập**: Sử dụng tài khoản vừa đăng ký hoặc tài khoản mặc định `user1@domain1.com` / `user1`.
3. **Soạn thư & Gửi đính kèm**: Soạn thư gửi tới `user2@domain1.com`, đính kèm file và gửi đi.
4. **Kiểm tra Hộp thư đến & Bộ lọc Spam**: Đăng nhập `user2@domain1.com` để đọc thư, thử gửi thư có chứa từ khóa `trúng thưởng` để kiểm tra bộ lọc Spam tự động.
