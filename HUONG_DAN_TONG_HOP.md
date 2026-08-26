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
6. [PHẦN E: Bảng tổng hợp các vị trí IP & Tài khoản cần thay đổi](#phần-e-bảng-tổng-hợp-các-file-cấu-hình-ip--tài-khoản)
7. [PHẦN F: Xử lý các lỗi thường gặp khi khởi động](#phần-f-xử-lý-các-lỗi-thường-gặp-khi-khởi-động)

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
2. Gõ lệnh:
   ```cmd
   ipconfig
   ```
3. Tìm dòng **`IPv4 Address`** của card mạng WiFi hoặc Ethernet đang kết nối (Ví dụ: `192.168.1.15`).

---

### Bước 4: Mở cổng Tường lửa (Windows Firewall) trên Máy Chủ
Để các máy khác trong mạng LAN truy cập được:
1. Mở Start Menu -> Tìm **Windows Defender Firewall with Advanced Security**.
2. Chọn **Inbound Rules** -> Chọn **New Rule...**:
   * Chọn **Port** -> Nhấn **Next**.
   * Nhập các cổng: `8080, 25, 143, 587` -> Nhấn **Next**.
   * Chọn **Allow the connection** -> Nhấn **Next** liên tục -> Đặt tên `Mail_LTM_Ports` -> Nhấn **Finish**.
*(Hoặc trong lúc kiểm tra đồ án nhanh, bạn có thể tạm thời tắt Windows Firewall).*

---

## PHẦN B: HƯỚNG DẪN CHI TIẾT KHỞI ĐỘNG CHƯƠNG TRÌNH

### Cách 1: Khởi động bằng IntelliJ IDEA (Khuyên dùng - Đơn giản nhất)
1. Mở thư mục dự án **`LTM-PTIT-Mail-Client-Server`** trong **IntelliJ IDEA**.
2. **Kiểm tra cấu hình Java (JDK 17)**:
   * Vào menu `File` -> `Project Structure` -> Mục `Project`.
   * Đảm bảo **SDK** đang chọn là **Java 17** (hoặc `17 Oracle OpenJDK` / `17 Microsoft OpenJDK`).
3. **Đồng bộ Maven**:
   * Mở tab **Maven** ở góc phải màn hình -> Nhấn biểu tượng **Reload All Maven Projects** 🔄 để IntelliJ tải đầy đủ các thư viện.
4. **Khởi chạy ứng dụng**:
   * Mở đường dẫn file: `src` -> `main` -> `java` -> `com.ptit.ltm.mail_application` -> Mở file [**`MailApplication.java`**](src/main/java/com/ptit/ltm/mail_application/MailApplication.java).
   * Nhấn chuột phải vào vùng soạn thảo -> Chọn **`Run 'MailApplication'`**  
     *(hoặc bấm vào biểu tượng tam giác màu xanh lá cây ▶️ nằm cạnh dòng `public class MailApplication`)*.
5. **Kiểm tra console khởi động thành công**:
   * Ở cửa sổ Run phía dưới, khi thấy xuất hiện dòng chữ:
     ```
     Tomcat started on port(s): 8080 (http) with context path ''
     Started MailApplication in X.XXX seconds
     ```
   * Nghĩa là ứng dụng đã khởi động hoàn tất!

---

### Cách 2: Khởi động bằng Terminal / Command Prompt (CMD)
Nếu không dùng IntelliJ, bạn có thể chạy bằng dòng lệnh:
1. Mở cửa sổ **CMD** hoặc **PowerShell** tại thư mục gốc của dự án.
2. Chạy lệnh:
   ```bash
   mvn spring-boot:run
   ```
3. Hoặc đóng gói thành file `.jar` và chạy:
   ```bash
   mvn clean package -DskipTests
   java -jar target/mail-application-0.0.1-SNAPSHOT.jar
   ```

---

### Cách 3: Truy cập vào giao diện Web Mail sau khi khởi động
1. Mở trình duyệt Web (Google Chrome / Microsoft Edge / Cốc Cốc).
2. Nhập địa chỉ:
   ```
   http://localhost:8080
   ```
   *(Hệ thống sẽ tự động chuyển hướng đến trang đăng nhập `http://localhost:8080/login`)*.
3. Đăng nhập bằng một trong các tài khoản có sẵn:
   * **Tài khoản 1**: `user1@domain1.com` / Mật khẩu: `user1`
   * **Tài khoản 2**: `user2@domain1.com` / Mật khẩu: `user2`
   * **Tài khoản 3**: `admin@domain1.com` / Mật khẩu: `admin123`
   * Hoặc bấm vào **Đăng ký** để tạo một tài khoản mới bất kỳ!

---

## PHẦN C: DÀNH CHO MÁY KHÁCH (CLIENT)

> ⚠️ **LƯU Ý CỰC KỲ QUAN TRỌNG:**
> Máy Khách (Client) **TUYỆT ĐỐI KHÔNG CẦN CÀI hMailServer** và **KHÔNG CẦN CÀI MySQL**.

### Cách 1: Sử dụng giao diện Web Mail trên trình duyệt (Đơn giản nhất - Khuyên dùng)
1. Kết nối máy Client vào **cùng mạng WiFi/LAN** với Máy Chủ.
2. Mở trình duyệt web (**Google Chrome / Microsoft Edge**).
3. Nhập đường dẫn:
   ```
   http://[IP_CỦA_MÁY_CHỦ]:8080
   ```
   *Ví dụ: `http://192.168.1.15:8080` (hoặc `http://192.168.1.15:8080/login`)*.
4. Đăng nhập với tài khoản:
   * **Tài khoản**: `user2@domain1.com` *(hoặc `user2`)*
   * **Mật khẩu**: `user2`
5. Bạn có thể gửi, nhận thư, đính kèm file trực tiếp trên trình duyệt như dùng Gmail!

---

### Cách 2: Nếu Máy Client cũng muốn mở source code Java trong IntelliJ
Nếu bạn gửi file source code cho bạn bè và bạn bè muốn tự chạy code Java trên máy của họ:
1. Mở source code trong IntelliJ trên máy Client.
2. Mở file **`MailApplicationConfiguration.java`** trên máy Client và sửa IP:
   ```java
   // Đổi từ 127.0.0.1 thành IP của Máy Chủ (ví dụ: 192.168.1.15)
   props.put("mail.smtp.host", "192.168.1.15");
   properties.put("mail.imap.host", "192.168.1.15");
   ```
3. Mở file **`application.yml`** trên máy Client và sửa:
   ```yaml
   spring:
     datasource:
       # Trỏ về database của máy chủ nếu muốn dùng chung database:
       url: jdbc:mysql://192.168.1.15:3306/mail_db?...
   ```
4. Chạy `MailApplication.java` trên máy Client và truy cập `http://localhost:8080`.

---

## PHẦN D: HƯỚNG DẪN TEST TRÊN CÙNG 1 MÁY TÍNH

Nếu bạn chỉ có 1 máy tính và muốn tự kiểm tra 2 người gửi/nhận cho nhau:

1. **Người dùng 1 (User 1):**
   * Mở trình duyệt Chrome (tab thông thường).
   * Vào `http://localhost:8080/login`.
   * Đăng nhập: `user1@domain1.com` / `user1`.

2. **Người dùng 2 (User 2):**
   * Nhấn `Ctrl + Shift + N` để mở **Cửa sổ Ẩn danh (Incognito)** trên Chrome.
   * Vào `http://localhost:8080/login`.
   * Đăng nhập: `user2@domain1.com` / `user2`.

3. **Thực hiện kiểm tra các tính năng:**
   * **Gửi thư & Đính kèm file**: Từ tab User 1, bấm **Soạn thư** -> Nhập người nhận `user2@domain1.com` -> Đính kèm 1 file PDF/ảnh bất kỳ -> Bấm **Gửi**.
   * **Nhận thư & Trả lời (Reply)**: Sang tab User 2 -> Vào **Hộp thư đến** -> Nhấp vào thư vừa nhận -> Bấm nút **Trả lời thư** -> Gửi ngược lại cho User 1.
   * **Kiểm tra lọc Spam tự động**: Soạn thư có chứa từ khóa `trúng thưởng` hoặc `khuyến mãi sốc` -> Khi gửi tới User 2, thư sẽ tự động rơi vào mục **Spam** thay vì Hộp thư đến.
   * **Gửi hàng loạt**: Vào mục **Gửi hàng loạt** -> Chọn file Excel danh sách -> Bấm **Gửi**.

---

## PHẦN E: BẢNG TỔNG HỢP CÁC FILE CẤU HÌNH IP & TÀI KHOẢN

Khi bạn chia sẻ dự án hoặc chuyển đổi mạng, đây là các vị trí cần lưu ý:

| Tên File | Vị trí cần chỉnh sửa | Ý nghĩa cấu hình | Giá trị mặc định (Local) | Khi chạy mạng LAN đa máy |
| :--- | :--- | :--- | :--- | :--- |
| [`application.yml`](src/main/resources/application.yml) | `spring.datasource.url` | Địa chỉ kết nối MySQL Database | `localhost:3306/mail_db` | `localhost:3306/mail_db` (trên Server) hoặc `IP_Server:3306/mail_db` (trên Client) |
| [`application.yml`](src/main/resources/application.yml) | `spring.datasource.password` | Mật khẩu tài khoản MySQL `root` | `123456` | Thay đổi theo mật khẩu MySQL của máy bạn |
| [`MailApplicationConfiguration.java`](src/main/java/com/ptit/ltm/mail_application/configuration/MailApplicationConfiguration.java) | Dòng 39: `mail.smtp.host` | Địa chỉ máy chủ SMTP gửi mail | `"127.0.0.1"` | `"127.0.0.1"` (trên Server) hoặc `"IP_MÁY_SERVER"` (trên Client) |
| [`MailApplicationConfiguration.java`](src/main/java/com/ptit/ltm/mail_application/configuration/MailApplicationConfiguration.java) | Dòng 60: `mail.imap.host` | Địa chỉ máy chủ IMAP nhận mail | `"127.0.0.1"` | `"127.0.0.1"` (trên Server) hoặc `"IP_MÁY_SERVER"` (trên Client) |
| [`schema.sql`](schema.sql) | Toàn bộ file | Cấu trúc bảng Database MySQL | Cố định | Chạy 1 lần trên MySQL Server |
| [`data.sql`](data.sql) | Danh sách `INSERT INTO users` | Tài khoản đăng nhập ban đầu | `user1/user1`, `user2/user2` | Chạy 1 lần sau khi chạy `schema.sql` |

---

## PHẦN F: XỬ LÝ CÁC LỖI THƯỜNG GẶP KHI KHỞI ĐỘNG

### 1. Lỗi cổng `8080` bị chiếm dụng (`Port 8080 was already in use`)
* **Nguyên nhân**: Có một tiến trình hoặc lần chạy trước của Spring Boot chưa tắt hẳn.
* **Cách khắc phục**:
  1. Mở PowerShell / CMD chạy lệnh:
     ```cmd
     netstat -ano | findstr :8080
     ```
  2. Xem số PID ở cột cuối cùng (ví dụ `1234`) và tắt tiến trình:
     ```cmd
     taskkill /F /PID 1234
     ```
  3. Khởi động lại ứng dụng trong IntelliJ.

### 2. Lỗi kết nối MySQL (`Access denied for user 'root'@'localhost'`)
* **Nguyên nhân**: Mật khẩu root của MySQL trên máy bạn khác với `123456`.
* **Cách khắc phục**: Mở file [`application.yml`](src/main/resources/application.yml), sửa dòng `password: 123456` thành mật khẩu MySQL chính xác của máy bạn.

### 3. Lỗi không gửi được mail qua hMailServer (`Connection refused` hoặc `Connect failed`)
* **Nguyên nhân**: Dịch vụ hMailServer chưa được khởi động trên Windows.
* **Cách khắc phục**: Mở Start Menu -> Tìm **Services** -> Tìm dịch vụ **hMailServer** -> Nhấn chuột phải chọn **Start** (hoặc **Restart**).
