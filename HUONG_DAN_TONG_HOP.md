# 📘 TÀI LIỆU HƯỚNG DẪN TỔNG HỢP: CÀI ĐẶT, CẤU HÌNH & CHẠY HỆ THỐNG LTM MAIL CLIENT - SERVER

> **Đề tài**: Ứng dụng Mail Client - Server (Lập trình mạng - PTIT)  
> **Các tính năng chính**: Gửi thư, nhận thư (IMAP), trả lời thư (Reply), đính kèm tệp tin, gửi thư hàng loạt qua Excel, bộ lọc thư rác (Spam) tự động và quản lý tài khoản qua MySQL Database.

---

## 📑 MỤC LỤC
1. [Kiến trúc & Mô hình triển khai](#1-kiến-trúc--mô-hình-triển-khai)
2. [PHẦN A: Hướng dẫn cấu hình trên MÁY CHỦ (Server)](#phần-a-dành-cho-máy-chủ-server)
3. [PHẦN B: Hướng dẫn chi tiết KHỞI ĐỘNG CHƯƠNG TRÌNH](#phần-b-hướng-dẫn-chi-tiết-khởi-động-chương-trình)
4. [PHẦN C: Hướng dẫn dành cho MÁY KHÁCH (Client)](#phần-c-dành-cho-máy-khách-client)
5. [PHẦN D: Hướng dẫn kiểm tra trên CÙNG 1 MÁY TÍNH](#phần-d-hướng-dẫn-test-trên-cùng-1-máy-tính)
6. [PHẦN E: Hướng dẫn CẤU HÌNH BỘ LỌC SPAM (THƯ RÁC)](#phần-e-hướng-dẫn-cấu-hình-bộ-lọc-spam-thư-rác)
7. [PHẦN F: Bảng tổng hợp các vị trí IP & Tài khoản cần thay đổi](#phần-f-bảng-tổng-hợp-các-file-cấu-hình-ip--tài-khoản)
8. [PHẦN G: Xử lý các lỗi thường gặp khi khởi động](#phần-g-xử-lý-các-lỗi-thường-gặp-khi-khởi-động)

---

## 1. KIẾN TRÚC & MÔ HÌNH TRIỂN KHAI

* **MÁY CHỦ (SERVER)**: Là 1 máy tính duy nhất trong nhóm chạy **hMailServer** (quản lý giao thức SMTP cổng 25, IMAP cổng 143) + **MySQL Database** (cổng 3306) + **Spring Boot** (cổng 8080).
* **MÁY KHÁCH (CLIENT)**: **KHÔNG CẦN CÀI hMailServer**, **KHÔNG CẦN CÀI MySQL**. Chỉ cần mở trình duyệt web hoặc chạy client trỏ về IP của Máy Chủ.

```
       ┌────────────────────────────────────────────────────────┐
       │               MÁY CHỦ (SERVER - MÁY 1)                 │
       │  • IP LAN: Ví dụ 192.168.1.15                          │
       │  • Chạy: hMailServer + MySQL Database + Spring Boot    │
       └───────────────────────────┬────────────────────────────┘
                                   │
                    Kết nối cùng mạng WiFi / LAN
                                   │
          ┌────────────────────────┴────────────────────────┐
          │                                                 │
          ▼                                                 ▼
┌───────────────────────────┐                     ┌───────────────────────────┐
│     MÁY CLIENT 2          │                     │     MÁY CLIENT 3          │
│ • KHÔNG CẦN CÀI HMAIL     │                     │ • KHÔNG CẦN CÀI HMAIL     │
│ • Mở Web:                 │                     │ • Mở Web:                 │
│   http://192.168.1.15:8080│                     │   http://192.168.1.15:8080│
│ • Đăng nhập: user2        │                     │ • Đăng nhập: user3        │
└───────────────────────────┘                     └───────────────────────────┘
```

---

## PHẦN A: DÀNH CHO MÁY CHỦ (SERVER)

### Bước 1: Khởi tạo Database và Nạp dữ liệu trong MySQL Workbench
1. Mở **MySQL Workbench**, kết nối vào tài khoản `root` (Mật khẩu: `123456`).
2. **Chạy file Schema** (Tạo cấu trúc các bảng):
   * Vào `File` -> `Open SQL Script...` -> Chọn file **`schema.sql`** trong thư mục project.
   * Nhấn biểu tượng **Tia sét ⚡ (Execute)** để chạy.
3. **Chạy file Data** (Nạp dữ liệu mẫu):
   * Vào `File` -> `Open SQL Script...` -> Chọn file **`data.sql`**.
   * Nhấn biểu tượng **Tia sét ⚡ (Execute)** để nạp 8 tài khoản mẫu, 10 quy tắc lọc spam, 8 danh bạ và nhật ký gửi mail.
4. Bấm nút **Refresh 🔄** ở cột Schemas bên trái -> Sẽ thấy xuất hiện database `mail_db` với đầy đủ 5 bảng dữ liệu.

---

### Bước 2: Cài đặt và Thiết lập hMailServer (Máy chủ Email)
1. Chạy file cài đặt **`hMailServer-5.6.8-B2383.exe`**:
   * Ở bước chọn Components: Chọn **`Full installation (includes built-in database engine)`** *(chọn loại có sẵn SQL Compact để không bị lỗi SSL)*.
   * Đặt mật khẩu quản trị cho hMailServer (ví dụ: `123456`).
2. Mở ứng dụng **hMailServer Administrator**:
   * Chọn `localhost` -> Nhấn **Connect** -> Nhập mật khẩu quản trị `123456` -> Nhấn **OK**.
3. **Tạo Domain:**
   * Bấm vào mục **`Domains`** ở danh mục bên trái -> Chọn **`Add domain...`**
   * Nhập ô Domain: `domain1.com` -> Nhấn **Save**.
4. **Tạo các tài khoản email:**
   * Mở rộng `Domains` -> `domain1.com` -> Chọn **`Accounts`** -> Nhấn **`Add...`**:
     * **Tài khoản 1**: Address `user1`, Password `user1` -> Nhấn **Save** *(Email: `user1@domain1.com`)*.
     * **Tài khoản 2**: Address `user2`, Password `user2` -> Nhấn **Save** *(Email: `user2@domain1.com`)*.
     * **Tài khoản 3**: Address `user3`, Password `user3` -> Nhấn **Save** *(Email: `user3@domain1.com`)*.

---

### Bước 3: Lấy địa chỉ IP LAN của Máy Chủ
1. Mở cửa sổ **Command Prompt (CMD)** hoặc **PowerShell** trên Máy Chủ.
2. Gõ lệnh: `ipconfig`
3. Tìm dòng **`IPv4 Address`** của card mạng WiFi hoặc Ethernet đang kết nối (Ví dụ: `192.168.1.15`).

---

### Bước 4: Mở cổng Tường lửa (Windows Firewall) trên Máy Chủ
1. Mở Start Menu -> Tìm **Windows Defender Firewall with Advanced Security**.
2. Chọn **Inbound Rules** -> Chọn **New Rule...**:
   * Chọn **Port** -> Nhập các cổng: `8080, 25, 143, 587` -> Chọn **Allow the connection** -> Đặt tên `Mail_LTM_Ports` -> Nhấn **Finish**.
*(Hoặc trong lúc demo đồ án nhanh, bạn có thể tạm thời tắt Windows Firewall).*

---

## PHẦN B: HƯỚNG DẪN CHI TIẾT KHỞI ĐỘNG CHƯƠNG TRÌNH

### Cách 1: Khởi động bằng IntelliJ IDEA (Khuyên dùng - Đơn giản nhất)
1. Mở thư mục dự án **`LTM-PTIT-Mail-Client-Server`** trong **IntelliJ IDEA**.
2. **Kiểm tra Java 17**: Vào `File` -> `Project Structure` -> Mục `Project` -> Đảm bảo **SDK** là **Java 17**.
3. **Đồng bộ Maven**: Mở tab **Maven** ở góc phải màn hình -> Nhấn biểu tượng **Reload All Maven Projects** 🔄.
4. **Khởi chạy ứng dụng**:
   * Mở file [**`MailApplication.java`**](src/main/java/com/ptit/ltm/mail_application/MailApplication.java).
   * Nhấn chuột phải -> Chọn **`Run 'MailApplication'`** *(hoặc bấm tam giác xanh ▶️)*.
5. Khi console xuất hiện dòng `Started MailApplication in X.XXX seconds (process running on port 8080)` là thành công!

---

### Cách 2: Truy cập Web Mail sau khi khởi động
1. Mở trình duyệt Web (Chrome / Edge).
2. Nhập địa chỉ: **`http://localhost:8080`** *(tự động chuyển hướng tới `/login`)*.
3. Đăng nhập bằng tài khoản:
   * **Tài khoản 1**: `user1@domain1.com` / Mật khẩu: `user1`
   * **Tài khoản 2**: `user2@domain1.com` / Mật khẩu: `user2`
   * **Tài khoản Admin**: `admin@domain1.com` / Mật khẩu: `admin123`

---

## PHẦN C: DÀNH CHO MÁY KHÁCH (CLIENT)

> ⚠️ **LƯU Ý CỰC KỲ QUAN TRỌNG:**
> Máy Khách (Client) **TUYỆT ĐỐI KHÔNG CẦN CÀI hMailServer** và **KHÔNG CẦN CÀI MySQL**.

### Cách 1: Dùng qua trình duyệt Web (Khuyên dùng)
1. Kết nối máy Client vào **cùng mạng WiFi/LAN** với Máy Chủ.
2. Mở trình duyệt web gõ: `http://[IP_MÁY_CHỦ]:8080` *(Ví dụ: `http://192.168.1.15:8080`)*.
3. Đăng nhập: `user2@domain1.com` / `user2`. Sử dụng đầy đủ tính năng như Gmail!

---

## PHẦN D: HƯỚNG DẪN TEST TRÊN CÙNG 1 MÁY TÍNH

1. **User 1**: Mở Chrome tab thường vào `http://localhost:8080/login` -> Đăng nhập `user1@domain1.com` / `user1`.
2. **User 2**: Nhấn `Ctrl + Shift + N` mở **Cửa sổ Ẩn danh (Incognito)** vào `http://localhost:8080/login` -> Đăng nhập `user2@domain1.com` / `user2`.
3. **Thực hiện test**:
   * **Soạn thư & Đính kèm file**: User 1 gửi tới `user2@domain1.com`, đính kèm file PDF/ảnh.
   * **Nhận thư & Trả lời**: User 2 vào Hộp thư đến -> Xem chi tiết -> Nhấn **Trả lời thư (Reply)**.
   * **Lọc Spam**: Soạn thư có chữ `trúng thưởng` -> Bên nhận sẽ thấy thư tự động rơi vào mục **Spam**.

---

## PHẦN E: HƯỚNG DẪN CẤU HÌNH BỘ LỌC SPAM (THƯ RÁC)

Hệ thống hỗ trợ 2 cơ chế cấu hình quy tắc lọc spam:

### 1. Cấu hình tự động qua MySQL Database (Bảng `spam_rules`)
`SpamFilterService` trong mã nguồn Java sẽ tự động đối soát nội dung/người gửi của email với bảng `spam_rules` để chuyển thư vào thư mục SPAM.
* **`KEYWORD_BLOCK`**: Chặn theo từ khóa trong tiêu đề/nội dung.
* **`DOMAIN_BLOCK`**: Chặn toàn bộ thư gửi từ domain rác.
* **`SENDER_BLOCK`**: Chặn theo địa chỉ email cụ thể.

👉 **Thêm quy tắc mới bằng câu lệnh SQL trong MySQL Workbench:**
```sql
USE mail_db;

-- Chặn thư có từ khóa lừa đảo
INSERT INTO spam_rules (rule_type, pattern, action, is_active, description)
VALUES ('KEYWORD_BLOCK', 'vay tiền nhanh', 'MOVE_TO_SPAM', 1, 'Chặn thư tín dụng đen');

-- Chặn toàn bộ email từ domain rác
INSERT INTO spam_rules (rule_type, pattern, action, is_active, description)
VALUES ('DOMAIN_BLOCK', 'quangcao-247.net', 'MOVE_TO_SPAM', 1, 'Chặn domain dịch vụ quảng cáo');
```

---

### 2. Cấu hình Quy tắc Spam trên hMailServer Administrator
1. Mở **hMailServer Administrator** -> Chọn mục **`Rules`** -> Bấm **`Add...`**
2. **Tab General**: Đặt tên (ví dụ: `Auto Filter Spam Keyword`).
3. **Tab Criteria** *(Điều kiện)*: Bấm **`Add...`** -> Field: Chọn `Subject` hoặc `Body` -> Comparison type: Chọn `Contains` -> Value: Nhập từ khóa (ví dụ: `trúng thưởng`) -> Bấm **OK**.
4. **Tab Actions** *(Hành động)*: Bấm **`Add...`** -> Action: Chọn `Move to IMAP folder` -> Gõ: `SPAM` -> Bấm **OK**.
5. Nhấn **`Save`** để kích hoạt quy tắc.

---

## PHẦN F: BẢNG TỔNG HỢP CÁC FILE CẤU HÌNH IP & TÀI KHOẢN

| Tên File | Vị trí cần chỉnh sửa | Ý nghĩa cấu hình | Giá trị mặc định (Local) | Khi chạy mạng LAN đa máy |
| :--- | :--- | :--- | :--- | :--- |
| [`application.yml`](src/main/resources/application.yml) | `spring.datasource.url` | Địa chỉ kết nối MySQL Database | `localhost:3306/mail_db` | `localhost:3306/mail_db` (Server) |
| [`application.yml`](src/main/resources/application.yml) | `spring.datasource.password` | Mật khẩu tài khoản MySQL `root` | `123456` | Mật khẩu MySQL máy bạn |
| [`MailApplicationConfiguration.java`](src/main/java/com/ptit/ltm/mail_application/configuration/MailApplicationConfiguration.java) | Dòng 39: `mail.smtp.host` | Địa chỉ máy chủ SMTP gửi mail | `"127.0.0.1"` | `"IP_MÁY_SERVER"` (nếu chạy code trên Client) |
| [`MailApplicationConfiguration.java`](src/main/java/com/ptit/ltm/mail_application/configuration/MailApplicationConfiguration.java) | Dòng 60: `mail.imap.host` | Địa chỉ máy chủ IMAP nhận mail | `"127.0.0.1"` | `"IP_MÁY_SERVER"` (nếu chạy code trên Client) |
| [`schema.sql`](schema.sql) | Toàn bộ file | Cấu trúc bảng Database MySQL | Cố định | Chạy 1 lần trên MySQL Server |
| [`data.sql`](data.sql) | Danh sách `INSERT IGNORE` | Dữ liệu mẫu phong phú | `user1`, `user2`, `admin`... | Chạy 1 lần sau khi chạy `schema.sql` |

---

## PHẦN G: XỬ LÝ CÁC LỖI THƯỜNG GẶP KHI KHỞI ĐỘNG

### 1. Lỗi cổng `8080` bị chiếm (`Port 8080 was already in use`)
* Mở PowerShell chạy: `netstat -ano | findstr :8080`
* Tắt tiến trình bằng PID (ví dụ PID 1234): `taskkill /F /PID 1234`

### 2. Lỗi kết nối MySQL (`Access denied for user 'root'`)
* Sửa dòng `password: 123456` trong [`application.yml`](src/main/resources/application.yml) thành mật khẩu MySQL trên máy của bạn.

### 3. Lỗi không gửi được mail (`Connection refused` hoặc `Connect failed`)
* Mở Start Menu -> Tìm **Services** -> Chuột phải vào dịch vụ **hMailServer** -> Chọn **Start** (hoặc **Restart**).
