# Hướng dẫn cài đặt — AI-Powered CP Judge System

# 1. Yêu cầu hệ thống


| Thành phần          | Phiên bản tối thiểu          | Ghi chú                                                                                                                       |
| ------------------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| **JDK**             | Java 21 (LTS)                | Cài đặt từ [Oracle](https://www.oracle.com/java/technologies/downloads/#java21) hoặc [Eclipse Temurin](https://adoptium.net/) |
| **Maven**           | 3.8+                         | Dùng để build project                                                                                                         |
| **MySQL**           | 8.0+                         | Database lưu đề thi, testcase, submission                                                                                     |
| **Docker Desktop**  | Bất kỳ                       | Khuyến nghị chạy MySQL trong container                                                                                        |
| **Trình biên dịch** | gcc/g++ (Windows: MinGW-w64) | Cần thiết để chấm C++                                                                                                         |
| **Python**          | 3.10+                        | Cần thiết để chấm Python                                                                                                      |


> **Lưu ý Windows:** Cài [MinGW-w64](https://www.mingw-w64.org/downloads/) và thêm `C:\mingw64\bin` vào `PATH` để `g++` hoạt động.

---

## 2. Cấu trúc thư mục

```
BT_Java-master/
├── pom.xml                          # Maven build config
├── run.bat                          # Script chạy nhanh trên Windows
├── src/main/java/com/java/          # Source code
│   ├── Main.java                    # Entry point
│   ├── SystemTest.java              # Kiểm thử hệ thống
│   ├── model/                       # Entity classes
│   ├── dao/                         # Database Access Objects
│   ├── service/                     # Business logic + Judge Engine
│   ├── ui/                          # Swing UI panels
│   └── util/                        # FileManager, DatabaseConnection
├── src/main/resources/
│   └── config.properties            # Cấu hình DB (tùy chọn)
└── target/                          # JAR output sau build
```

---

## 3. Cài đặt từng bước

### Bước 1: Clone hoặc giải nén source code

```bash
git clone <repo-url>
cd BT_Java-master
```

### Bước 2: Khởi động MySQL (bằng Docker — khuyến nghị)

```bash
docker run -d \
  --name judge-mysql \
  -e MYSQL_ROOT_PASSWORD=123 \
  -e MYSQL_DATABASE=JudgeSystem \
  -p 3306:3306 \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

> Nếu không dùng Docker, cài MySQL trực tiếp và tạo database `JudgeSystem` với charset `utf8mb4`.

### Bước 3: Kiểm tra biên dịch (compile)

```bash
mvn clean compile -q
```

Nếu thành công → không có lỗi, `BUILD SUCCESS`.

### Bước 4: Chạy kiểm thử hệ thống

```bash
mvn exec:java -Dexec.mainClass="com.java.SystemTest"
```

**Kết quả mong đợi:** `20 PASSED | 0 FAILED`

### Bước 5: Đóng gói JAR

```bash
mvn clean package -q
```

File JAR sẽ được tạo tại: `target/JudgeSystem-1.0-SNAPSHOT.jar`

### Bước 6: Chạy ứng dụng

**Cách 1 — Dùng script:**

```bash
run.bat
```

**Cách 2 — Dùng Maven:**

```bash
mvn exec:java -Dexec.mainClass="com.java.Main"
```

**Cách 3 — Chạy JAR trực tiếp:**

```bash
java -jar target/JudgeSystem-1.0-SNAPSHOT.jar
```

---

## 4. Cấu hình Database (tùy chọn)

Nếu muốn thay đổi thông tin kết nối, tạo file `src/main/resources/config.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/JudgeSystem?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC
db.username=root
db.password=123
db.driver=com.mysql.cj.jdbc.Driver
```

Nếu không có file này, ứng dụng sẽ dùng giá trị mặc định (`localhost:3306/JudgeSystem`, `root`/`123`).

---

## 5. Troubleshooting


| Lỗi                                                | Nguyên nhân                           | Cách khắc phục                                         |
| -------------------------------------------------- | ------------------------------------- | ------------------------------------------------------ |
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | MySQL Connector/J chưa được tải       | `mvn clean install` để tải dependency                  |
| `Communications link failure`                      | MySQL chưa chạy hoặc sai port         | Kiểm tra `docker ps`, đảm bảo port 3306 mở             |
| `g++ is not recognized`                            | MinGW chưa vào PATH                   | Thêm `C:\mingw64\bin` vào System Environment Variables |
| `python3: command not found` (Windows)             | Python chưa cài hoặc không trong PATH | Cài Python và đảm bảo `python` có thể gọi từ cmd       |
| Font/UI hiển thị lỗi                               | Java version thấp                     | Đảm bảo JDK 21+ và `JAVA_HOME` đúng                    |


---

## 6. Kiểm tra sau cài đặt

Sau khi chạy app, bạn sẽ thấy:

- **Dashboard** hiển thị số lượng đề thi, testcase, submission.
- Có thể tạo đề mới, chấm code C++/Java/Python, phân tích bằng AI.

Nếu gặp lỗi, kiểm tra log trong terminal hoặc IDE console.