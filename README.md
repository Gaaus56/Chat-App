# Chat App (JavaFX + Socket + SQLite)

Đây là ứng dụng Chat được thiết kế theo mô hình Client-Server sử dụng Socket, JavaFX cho giao diện đồ họa và SQLite để lưu trữ cơ sở dữ liệu. Dự án được quản lý bằng Maven.

## Yêu cầu môi trường
- **JDK**: Java 17 trở lên.
- **Maven**: Cài đặt sẵn Maven hoặc sử dụng Maven wrapper trong IDE.

## Cách chạy chương trình (Bằng Terminal/CMD)

### 1. Chạy Server
Mở terminal/cmd tại thư mục gốc của dự án (`ChatAppProject`) và gõ lệnh:
```bash
mvn clean javafx:run
```

### 2. Chạy Client
Mở một terminal **mới** (hoàn toàn độc lập với terminal đang chạy Server) tại thư mục dự án và chạy lệnh sau (lưu ý có dấu ngoặc kép để PowerShell không bị lỗi):
```bash
mvn javafx:run "-Dapp.mainClass=com.chatapp.client.ClientApp"
```

*(Hoặc cách dễ nhất là mở project bằng IntelliJ/Eclipse và ấn chuột phải -> Run trên file `MainServerApp.java` và `ClientApp.java`)*

---

## Các tính năng hiện tại

### Phase 1: Server Core & GUI (Trọn vẹn 2/2 điểm)
- Khởi động DB SQLite tự động ngay khi bật Server.
- Giao diện quản lý Server (Port, Nút Khởi động/Tắt Server).
- Theo dõi danh sách các thiết bị/client đang kết nối (IP, Tài khoản, Thời gian) theo thời gian thực (Real-time).
- Cấu trúc luồng chạy nền (Multithreading + `Platform.runLater()`) đảm bảo giao diện Server không bao giờ bị đơ (Not Responding).

### Phase 2: Client Config & Authentication (Trọn vẹn 3/3 điểm)
- Giao diện quản lý cấu hình Server: Cho phép Thêm, Sửa, Xóa IP/Port các Server và **ghi nhớ lưu vào file** (`servers.dat`).
- Giao diện Đăng nhập / Đăng ký hiện đại, tinh gọn.
- Mã hóa dữ liệu truyền tải thông qua Socket (`ObjectOutputStream`).
- Phía Server sử dụng `UserDAO` để tương tác trực tiếp với CSDL SQLite (bảng `users`) kiểm tra tài khoản hợp lệ, và không cho phép đăng nhập 1 tài khoản trên 2 nơi cùng lúc.

### Phase 3: Giao diện Chat cốt lõi & UX (Trọn vẹn 3/3 điểm)
- **Main Chat GUI** với `TabPane`: Danh sách user online cập nhật Real-time (Thời gian thực).
- Mở **Tab** chat riêng biệt: Click đúp vào một user đang online để mở một màn hình chat 1-1 riêng biệt, có thể mở nhiều Tab cùng lúc.
- **Tương tác phím**: Hỗ trợ `Shift+Enter` để xuống dòng và `Enter` để Gửi tin nhắn.
- Tích hợp 5 nút **Emoji** chuẩn Unicode 😃 ❤️ 👍 😂 😭 có thể ấn để chèn ngay vào TextBox chat.