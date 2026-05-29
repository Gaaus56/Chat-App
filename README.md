# Chat App (JavaFX + Socket + SQLite)

Đây là ứng dụng Chat được thiết kế theo mô hình Client-Server sử dụng Socket, JavaFX cho giao diện đồ họa và SQLite để lưu trữ cơ sở dữ liệu. Dự án được quản lý bằng Maven.

## Yêu cầu môi trường
- **JDK**: Java 17 trở lên.
- **Maven**: Cài đặt sẵn Maven hoặc sử dụng Maven wrapper trong IDE.

## Cách chạy chương trình

Dự án sử dụng plugin `javafx-maven-plugin` để hỗ trợ chạy ứng dụng trực tiếp từ Terminal một cách dễ dàng, tự động xử lý các dependencies JavaFX.

### Chạy Server (Phase 1)
Mở terminal/cmd tại thư mục gốc của dự án (`ChatAppProject`) và gõ lệnh:
```bash
mvn clean javafx:run
```

**Hoặc chạy qua IDE (IntelliJ / Eclipse):**
Bạn có thể tìm file `MainServerApp.java` trong đường dẫn `src/main/java/com/chatapp/server/MainServerApp.java` và ấn nút **Run**. (Cần cấu hình Add VM options nếu IDE chưa nhận tự động JavaFX).

### Các tính năng hiện tại (Phase 1)
- Giao diện quản lý Server (Port, Nút Khởi động/Tắt).
- Theo dõi theo thời gian thực (Real-time) danh sách các thiết bị/client đang kết nối vào Server.
- Cấu trúc luồng chạy nền (Multithreading) không làm đơ giao diện.