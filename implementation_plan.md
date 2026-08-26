# Kế hoạch khắc phục và hoàn thiện hệ thống LTM Mail Client - Server

Tài liệu này mô tả chi tiết các bước sửa lỗi, chuẩn hóa dependencies cho Spring Boot 3, khắc phục các lỗi giao diện/JavaScript, hoàn thiện tính năng gửi đính kèm file, xử lý tài nguyên template an toàn và tạo tài liệu hướng dẫn hoàn chỉnh.

## Các thay đổi đề xuất

### 1. Chuẩn hóa Dependencies và Package ([`pom.xml`](file:///c:/Users/Admin/Desktop/ltm-14(4)/LTM-PTIT-Mail-Client-Server/pom.xml))
- Gỡ bỏ các dependency `javax.*` cũ không tương thích với Spring Boot 3: `javax.mail:mail:1.4.7`, `javax.validation:validation-api:2.0.1.Final`.
- Gỡ bỏ `springdoc-openapi-ui:1.7.0` (trùng lặp với bản `2.1.0` dành cho Spring Boot 3).
- Chuyển đổi toàn bộ mã nguồn sử dụng `javax.mail.*`, `javax.activation.*`, `javax.validation.*` sang `jakarta.mail.*`, `jakarta.activation.*`, `jakarta.validation.*`.

### 2. Sửa lỗi Backend Logic & Controller
- **[`MailController.java`](file:///c:/Users/Admin/Desktop/ltm-14(4)/LTM-PTIT-Mail-Client-Server/src/main/java/com/ptit/ltm/mail_application/controller/MailController.java)**:
  - Sửa `@GetMapping("/spam")` trả về template `"spamMail"` thay vì `"sentMail"`.
  - Cập nhật `@PostMapping("/send")` để nhận `@RequestParam(value = "file", required = false) MultipartFile file` và chuyển tới `mailFacadeService`.
  - Chuyển đổi cơ chế đọc file template JSON từ `Paths.get("src/main/java/...")` sang `ClassPathResource` để tránh lỗi khi đóng gói JAR.
- **[`AuthController.java`](file:///c:/Users/Admin/Desktop/ltm-14(4)/LTM-PTIT-Mail-Client-Server/src/main/java/com/ptit/ltm/mail_application/controller/AuthController.java)**:
  - Bổ sung `@PostMapping("/register")` để điều hướng an toàn sau khi tạo tài khoản.

### 3. Sửa lỗi Frontend & JavaScript
- **[`home.html`](file:///c:/Users/Admin/Desktop/ltm-14(4)/LTM-PTIT-Mail-Client-Server/src/main/resources/templates/home.html) & [`sentMail.html`](file:///c:/Users/Admin/Desktop/ltm-14(4)/LTM-PTIT-Mail-Client-Server/src/main/resources/templates/sentMail.html) & [`detailMail.html`](file:///c:/Users/Admin/Desktop/ltm-14(4)/LTM-PTIT-Mail-Client-Server/src/main/resources/templates/detailMail.html)**:
  - Sửa lỗi off-by-one trong vòng lặp JavaScript `i <= emails.length` thành `i < emails.length`.
  - Xử lý an toàn khi parse nội dung mail: hỗ trợ cả mail văn bản thuần (plain text/HTML) và mail dạng JSON Quill Delta mà không bị throw `SyntaxError`.
- **[`spamMail.html`](file:///c:/Users/Admin/Desktop/ltm-14(4)/LTM-PTIT-Mail-Client-Server/src/main/resources/templates/spamMail.html)**:
  - Sửa đường dẫn `<script src="/js/tailwind.js">` thành `/js/lib/tailwind/tailwind.js`.
- **[`sendMail.html`](file:///c:/Users/Admin/Desktop/ltm-14(4)/LTM-PTIT-Mail-Client-Server/src/main/resources/templates/sendMail.html)**:
  - Thêm thuộc tính `enctype="multipart/form-data"` và input chọn file đính kèm để người dùng gửi file trực tiếp trên giao diện.

### 4. Tạo tài liệu tổng kết và hướng dẫn chạy hoàn thiện
- Tạo file [`HUONG_DAN_VA_DANH_SACH_SUA_DOI.md`](file:///c:/Users/Admin/Desktop/ltm-14(4)/LTM-PTIT-Mail-Client-Server/HUONG_DAN_VA_DANH_SACH_SUA_DOI.md) trong thư mục gốc của dự án, bao gồm:
  - Danh sách tất cả các điểm đã được sửa và cải tiến.
  - Hướng dẫn cài đặt hMailServer, tạo domain, tạo tài khoản.
  - Hướng dẫn cấu hình cổng mạng, gửi nhận email và gửi hàng loạt bằng Excel.

## Kế hoạch kiểm tra (Verification Plan)
- Kiểm tra tính nhất quán của mã nguồn Java sau khi chuyển sang `jakarta.*`.
- Kiểm tra render HTML và tính an toàn của các script Javascript.
- Đảm bảo tất cả các file JSON template được sao chép/đọc đúng từ `resources`.
