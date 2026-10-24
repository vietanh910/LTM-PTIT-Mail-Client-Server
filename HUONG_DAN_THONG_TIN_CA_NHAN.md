# Thông tin cá nhân

Mở menu ảnh đại diện ở góc trên bên phải, chọn **Thông tin cá nhân** (ngay trên **Đăng xuất**), hoặc truy cập `/profile` sau khi đăng nhập.

- Chỉnh sửa họ tên, số điện thoại, chọn ảnh JPG/PNG hoặc chọn dùng ảnh mặc định, rồi bấm **Lưu thay đổi**.
- Ảnh tối đa 2 MB, tối đa 4 triệu điểm ảnh. Máy chủ kiểm tra nội dung ảnh và lưu dưới dạng PNG trong MySQL.
- Muốn đổi mật khẩu, nhập mật khẩu hiện tại, mật khẩu mới ít nhất 6 ký tự và xác nhận. Để trống cả ba ô để giữ mật khẩu cũ.
- Mật khẩu mới được đồng bộ sang tài khoản tương ứng trên hMailServer. Nếu đồng bộ thất bại, giao dịch MySQL bị hủy và mật khẩu trong phiên đăng nhập được giữ nguyên.

## Chạy ứng dụng

Khởi động lại Spring Boot sau khi cập nhật mã. Cấu hình hiện tại `spring.jpa.hibernate.ddl-auto: update` tự thêm cột `users.avatar`; không cần chạy lại `schema.sql` vì file đó xóa các bảng cũ. Nếu không dùng tự cập nhật cấu trúc, chạy riêng:

```sql
ALTER TABLE users ADD COLUMN avatar MEDIUMBLOB NULL;
```

Chức năng đổi mật khẩu cần chạy Spring Boot trên máy Windows có hMailServer COM API. Mật khẩu quản trị dùng cùng giá trị mặc định của phần đăng ký hiện tại; có thể cấu hình `hmail.admin-password` trong cấu hình ngoài hoặc biến môi trường `HMAIL_ADMIN_PASSWORD` nếu máy chủ dùng mật khẩu khác.

## Kiểm thử

```sh
mvn -Dtest=ProfileServiceTest,ProfileControllerTest test
```

Các bài kiểm tra bao gồm hiển thị Thymeleaf, quyền truy cập theo phiên đăng nhập, CSRF, kiểm tra dữ liệu, ảnh hợp lệ/giả/quá lớn, mật khẩu cũ/sai xác nhận, đồng bộ mật khẩu và giữ phiên cũ khi đồng bộ lỗi. Không kết nối hMailServer thật trong bộ kiểm tra này.

Kết quả kiểm tra tính năng: **16 bài kiểm tra đạt**. Bản mã hiện tại có lỗi biên dịch sẵn ở `MailController`: tham chiếu `RecipientValidationService` nhưng thiếu lớp này. Đã chạy các bài kiểm tra trên bằng POM tạm ở `target/profile-check-pom.xml`, loại trừ `MailController` khỏi bước biên dịch. Cần khôi phục lớp bị thiếu trước khi biên dịch/chạy toàn bộ ứng dụng theo lệnh Maven thông thường. Chưa kiểm thử trực tiếp với MySQL và hMailServer đang chạy.
