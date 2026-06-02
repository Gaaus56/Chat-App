# Chat App (JavaFX + Socket + SQLite)

Đây là ứng dụng Chat được thiết kế theo mô hình Client-Server sử dụng Socket, JavaFX cho giao diện đồ họa và SQLite để lưu trữ cơ sở dữ liệu. Dự án được quản lý bằng Maven.

## Yêu cầu môi trường
- **Hệ điều hành**: Windows, macOS, hoặc Linux.
- **JDK (Java Development Kit)**: Phiên bản 17 trở lên.
- **Maven**: Cài đặt sẵn Maven hoặc sử dụng Maven wrapper trong IDE.

## Thư viện quan trọng sử dụng trong dự án
Dự án sử dụng một số thư viện mã nguồn mở để hỗ trợ các tính năng nâng cao (xem chi tiết trong `pom.xml`):
- **JavaFX (org.openjfx)**: (phiên bản `21.0.1`) Xây dựng giao diện người dùng đồ họa hiện đại (GUI).
- **SQLite JDBC (org.xerial:sqlite-jdbc)**: (phiên bản `3.45.1.0`) Quản lý và lưu trữ cơ sở dữ liệu (tài khoản, tin nhắn, nhóm chat) mà không cần cài đặt server CSDL riêng biệt.
- **RichTextFX (org.fxmisc.richtext:richtextfx)**: (phiên bản `0.11.2`) Hỗ trợ khung soạn thảo văn bản nâng cao (dùng cho việc hiển thị tin nhắn đa dạng).
- **Webcam Capture (com.github.sarxos:webcam-capture)**: (phiên bản `0.3.12`) Hỗ trợ truy cập và chụp ảnh trực tiếp từ thiết bị Webcam/Camera của máy tính để gửi qua chat.

## Cách chạy chương trình

Có thể chạy dự án thông qua công cụ Maven hoặc thông qua file JAR đã được đóng gói sẵn.

### Cách 1: Chạy trực tiếp bằng Maven (Dành cho nhà phát triển)
Mở PowerShell hoặc Command Prompt tại thư mục gốc của dự án (`ChatAppProject`).

1. **Chạy Server**:
```powershell
mvn clean javafx:run
```

2. **Chạy Client**:
Mở một cửa sổ PowerShell **mới** (độc lập với cửa sổ chạy Server) và chạy lệnh sau (lưu ý có dấu ngoặc kép để PowerShell không bị lỗi):
```powershell
mvn javafx:run "-Dapp.mainClass=com.chatapp.client.ClientApp"
```
*(Có thể mở bao nhiêu cửa sổ Client tùy thích để test chat nhiều người).*

### Cách 2: Đóng gói và chạy bằng file JAR (Dành cho người dùng cuối)
Hệ thống đã được cấu hình `maven-shade-plugin` cùng với các `Launcher` (Lớp bọc JavaFX) để tạo ra các file JAR có thể chạy độc lập.

1. **Đóng gói dự án**:
Chạy lệnh sau để Maven tự động tải thư viện và đóng gói thành JAR:
```powershell
mvn clean package
```
Sau khi hoàn tất, bạn sẽ thấy 2 file JAR trong thư mục `target/`:
- `ChatApp-1.0-SNAPSHOT-server.jar`
- `ChatApp-1.0-SNAPSHOT-client.jar`

2. **Chạy ứng dụng từ file JAR**:
Có thể copy 2 file này sang bất kỳ thư mục hay máy tính nào có cài Java 17+ và chạy bằng cách nhấp đúp chuột, hoặc mở PowerShell tại thư mục đó và gõ:

- Chạy Server:
```powershell
java -jar target/ChatApp-1.0-SNAPSHOT-server.jar
```
- Chạy Client:
```powershell
java -jar target/ChatApp-1.0-SNAPSHOT-client.jar
```

---

## Các tính năng hiện tại

### Phase 1: Server Core & GUI
- Khởi động DB SQLite tự động ngay khi bật Server (Tự động tạo `chatapp.db` nếu chưa có).
- Giao diện quản lý Server (Port, Nút Khởi động/Tắt Server).
- Theo dõi danh sách các thiết bị/client đang kết nối (IP, Tài khoản, Thời gian) theo thời gian thực (Real-time).
- Cấu trúc luồng chạy nền (Multithreading + `Platform.runLater()`) đảm bảo giao diện Server không bao giờ bị đơ (Not Responding).

### Phase 2: Client Config & Authentication
- Giao diện quản lý cấu hình Server: Cho phép Thêm, Sửa, Xóa IP/Port các Server và **ghi nhớ lưu vào file** (`servers.dat`).
- Giao diện Đăng nhập / Đăng ký hiện đại, tinh gọn.
- Mã hóa dữ liệu truyền tải thông qua Socket (`ObjectOutputStream`).
- Phía Server sử dụng `UserDAO` để tương tác trực tiếp với CSDL SQLite (bảng `users`) kiểm tra tài khoản hợp lệ, và không cho phép đăng nhập 1 tài khoản trên 2 nơi cùng lúc.

### Phase 3: Giao diện Chat cốt lõi & UX
- **Main Chat GUI** với `TabPane`: Danh sách user online cập nhật Real-time (Thời gian thực).
- Mở **Tab** chat riêng biệt: Click đúp vào một user đang online để mở một màn hình chat 1-1 riêng biệt, có thể mở nhiều Tab cùng lúc.
- **Tương tác phím**: Hỗ trợ `Shift+Enter` để xuống dòng và `Enter` để Gửi tin nhắn.
- Tích hợp **Emoji** chuẩn Unicode có thể ấn để chèn ngay vào TextBox chat.

### Phase 4: Các tính năng Nâng cao (Media, Nhóm, Lịch sử)
- **Chat Nhóm (Group)**: Cho phép tạo nhóm từ danh sách những người đang online (sử dụng CheckBox để chọn nhiều người). Nhóm sẽ hiển thị trên Sidebar và hoạt động gửi/nhận như chat bình thường.
- **Gửi Đa Phương Tiện (File / Voice / Webcam)**: 
  - Hỗ trợ chọn File để gửi (người nhận có thể Tải Xuống).
  - Hỗ trợ ghi âm (Voice) gửi trực tiếp và nghe lại qua `AudioPlayer`.
  - Hỗ trợ mở Webcam chụp ảnh và gửi ảnh trực tiếp vào khung chat.
  - Tự động tạo thư mục `server_media/` trên Server để lưu trữ vĩnh viễn các file này.
- **Đồng bộ Lịch sử Chat & Xóa tin nhắn Real-time**: 
    - Mọi tin nhắn (Văn bản, File, Voice, Ảnh) đều được lưu vào CSDL và load lại chính xác vị trí/trạng thái khi đăng nhập lại.
    - Click chuột phải để Xóa tin nhắn (chỉ được xóa tin nhắn của chính mình).
    - Tính năng Xóa được đồng bộ Real-time: Khi một người xóa tin nhắn, màn hình của người nhận bên kia cũng lập tức cập nhật và ẩn tin nhắn đó đi mà không bị lỗi giao diện.