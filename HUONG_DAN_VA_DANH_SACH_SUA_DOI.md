# 📑 NHẬT KÝ CHI TIẾT CÁC CẢI TIẾN & KHẮC PHỤC HỆ THỐNG LTM MAIL CLIENT - SERVER

---

## I. TỔNG QUAN CÁC CÔNG VIỆC ĐÃ HOÀN THÀNH

### 1. Nâng cấp Nền tảng Spring Boot 3 & Jakarta EE (`pom.xml`)
* Loại bỏ các thư viện cũ không tương thích: `javax.mail:mail:1.4.7`, `javax.validation:validation-api:2.0.1.Final`, `springdoc-openapi-ui:1.7.0`.
* Bổ sung thư viện email chuẩn Spring Boot 3: `org.eclipse.angus:jakarta.mail`.
* Bổ sung cơ sở dữ liệu: `spring-boot-starter-data-jpa`, `com.mysql:mysql-connector-j`.
* Chuyển đổi toàn bộ mã nguồn từ `javax.*` sang `jakarta.*` (`jakarta.mail.*`, `jakarta.activation.*`, `jakarta.validation.*`).

---

### 2. Thiết kế & Khởi tạo Toàn bộ Hệ thống MySQL Database
* Đã cấu hình kết nối Datasource và Hibernate `ddl-auto: update` trong `application.yml`.
* Tạo đầy đủ 5 Entity trong package `com.ptit.ltm.mail_application.entity`:
  1. **`User`** (Bảng `users`): Quản lý tài khoản, mật khẩu, họ tên, email, số điện thoại, địa chỉ, quyền hạn.
  2. **`SpamRule`** (Bảng `spam_rules`): Quản lý quy tắc chặn spam theo từ khóa (`KEYWORD_BLOCK`), tên miền (`DOMAIN_BLOCK`), email người gửi (`SENDER_BLOCK`).
  3. **`EmailLog`** (Bảng `email_logs`): Ghi nhật ký email gửi/nhận, file đính kèm và phân loại thư.
  4. **`Contact`** (Bảng `contacts`): Quản lý danh bạ liên hệ cá nhân.
  5. **`EmailTemplate`** (Bảng `email_templates`): Quản lý các mẫu thư soạn sẵn.
* Tạo đầy đủ các JPA Repositories và Services:
  - `UserRepository`, `SpamRuleRepository`, `EmailLogRepository`, `ContactRepository`, `EmailTemplateRepository`.
  - `UserService`, `SpamFilterService`, `DataInitService`.

---

### 3. Tách File Khởi tạo Database & Dữ liệu Mẫu (Dùng để nạp vào MySQL)
* [**`schema.sql`**](schema.sql): File chứa các câu lệnh `CREATE DATABASE` và `CREATE TABLE` (5 bảng).
* [**`data.sql`**](data.sql): File nạp dữ liệu mẫu phong phú bằng `INSERT IGNORE INTO` (8 users, 10 spam rules, 8 contacts, 4 templates, 8 email logs) đảm bảo không bị lỗi trùng lặp dữ liệu.
* Xử lý bảng mã chuẩn UTF-8 (`utf8mb4_unicode_ci`) không bị lỗi font tiếng Việt.

---

### 4. Khắc phục Lỗi Logic & Xử lý Email (Backend)
* **`MailServiceImpl.java`**:
  - Sửa lỗi trích xuất địa chỉ email khi chuỗi không có dấu ngoặc nhọn `<>`.
  - Bổ sung cơ chế đọc nội dung an toàn: Hỗ trợ Plain text, HTML và Multipart (thư có file đính kèm), loại bỏ nguy cơ `NullPointerException`.
* **`MailFacadeServiceImpl.java`**:
  - Tích hợp `SpamFilterService`: Tự động đối soát thư từ IMAP với bảng `spam_rules` trong MySQL để chuyển thư vào mục Spam.
  - Tích hợp nạp email logs từ database khi hòm thư IMAP mới tạo chưa có thư.
  - Tích hợp `EmailLogRepository` để tự động lưu vết lịch sử mỗi khi gửi email thành công.
* **`AuthController.java`**:
  - Tích hợp `UserService` xử lý lưu người dùng mới vào MySQL khi đăng ký (`POST /register`).
  - Hỗ trợ cơ chế đăng nhập linh hoạt (Database MySQL + Fallback hMailServer).

---

### 5. Khắc phục Giao diện & JavaScript (Frontend Thymeleaf)
* **Gửi file đính kèm (`sendMail.html`)**: Thêm thuộc tính `enctype="multipart/form-data"` và ô chọn file đính kèm.
* **Trả lời thư (`detailMail.html`)**: Thêm nút **Trả lời thư (Reply)** tự động điền địa chỉ người nhận và thêm tiền tố `Re:`.
* **Sửa lỗi Crash JavaScript (Quill Editor)**:
  - Sửa lỗi off-by-one trong vòng lặp JavaScript (`i < emails.length`).
  - Bọc `JSON.parse` an toàn cho Quill trong tất cả các view (`home.html`, `sentMail.html`, `spamMail.html`, `detailMail.html`, `sendMail.html`).
* **Hiển thị thông báo đăng ký / đăng nhập**: Bổ sung hiển thị thông báo thành công và lỗi trong `login.html` và `register.html`.

---

### 6. Khắc phục Lỗi Ký tự BOM (`\ufeff`) & Cấu hình UTF-8
* Quét và chuyển đổi toàn bộ các file mã nguồn `.java` sang chuẩn **UTF-8 No BOM**, khắc phục triệt để lỗi `illegal character: '\ufeff'`.
* Cấu hình bắt buộc UTF-8 cho Servlet và Thymeleaf trong `application.yml`.

---

### 7. Tạo Tài liệu Hướng dẫn Toàn diện
* [**`HUONG_DAN_TONG_HOP.md`**](HUONG_DAN_TONG_HOP.md): Tài liệu hướng dẫn đầy đủ từ A -> Z:
  - Hướng dẫn cấu hình Máy Chủ (MySQL, hMailServer, IP LAN, Firewall).
  - Hướng dẫn chi tiết Khởi động chương trình qua IntelliJ IDEA và CMD.
  - Hướng dẫn kết nối cho Máy Khách (Client không cần cài hMailServer/MySQL).
  - Hướng dẫn Test 2 tài khoản trên cùng 1 máy tính.
  - Hướng dẫn cấu hình bộ lọc Spam.
  - Bảng tổng hợp các vị trí cần sửa IP & Tài khoản khi chia sẻ project.
  - Xử lý các lỗi thường gặp (trùng port 8080, lỗi MySQL, lỗi hMailServer).
