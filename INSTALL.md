# Hướng dẫn cài đặt — AI-Powered CP Judge System

## 1. Yêu cầu hệ thống

| Thành phần          | Phiên bản tối thiểu          | Ghi chú                                                                                                                       |
| ------------------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| **JDK**             | Java 17 (LTS) hoặc Java 21   | Cài đặt từ [Oracle](https://www.oracle.com/java/technologies/downloads/) hoặc [Eclipse Temurin](https://adoptium.net/)        |
| **Maven**           | 3.8+                         | Dùng để build project và chạy tests                                                                                           |
| **MySQL**           | 8.0+                         | Database lưu đề thi, testcase, submission                                                                                     |
| **Docker Desktop**  | Bất kỳ                       | Khuyến nghị chạy MySQL trong container                                                                                        |
| **Trình biên dịch** | gcc/g++ (Windows: MinGW-w64) | Cần thiết để chấm C++                                                                                                         |
| **Python**          | 3.10+                        | Cần thiết để chấm Python và chạy checker script                                                                               |

> **Lưu ý Windows:** Cài [MinGW-w64](https://www.mingw-w64.org/downloads/) và thêm `C:\mingw64\bin` vào `PATH` để `g++` hoạt động.

---

## 2. Cấu trúc thư mục

```
JavaLast-main/
├── pom.xml                          # Maven build config (JUnit 5, FlatLaf, Gson, OkHttp)
├── run.bat                          # Script build & chạy nhanh trên Windows
├── build.bat                        # Script build nhanh
├── src/main/java/com/java/          # Source code chính
│   ├── Main.java                    # Entry point + shutdown hook dọn dẹp
│   ├── SystemTest.java              # Kiểm thử hệ thống (20 tests)
│   ├── model/                       # Entity classes (Problem, Testcase, SampleCode, Submission)
│   ├── dao/                         # Database Access Objects (CRUD operations)
│   ├── service/                     # Business logic (ProblemService, JudgeEngine, GeminiAIService)
│   ├── ui/                          # Swing UI panels (Dashboard, ProblemEntry, AI, CodeSubmit, Result)
│   └── util/                        # FileManager, DatabaseConnection, CodeFormatter
├── src/test/java/com/java/          # Unit & Integration tests (JUnit 5)
│   ├── service/                     # JudgeEngineTest, ProblemServiceIntegrationTest
│   ├── util/                        # CodeFormatterTest, FileManagerTest
│   └── dao/                         # DaoIntegrationTest
├── src/main/resources/
│   └── config.properties            # Cấu hình DB + AI API key
└── target/                          # JAR output sau build
```

---

## 3. Cài đặt từng bước

### Bước 1: Clone hoặc giải nén source code

```bash
git clone <repo-url>
cd JavaLast-main
```

### Bước 2: Cấu hình API Key (cho AI)

Mở file `src/main/resources/config.properties` (hoặc copy từ `config.properties.example`), thay thế `YOUR_API_KEY_HERE` bằng API key thực:

```properties
ai.api.key=YOUR_API_KEY_HERE
```

> **Google Gemini** (khuyến nghị): Lấy API key miễn phí tại https://aistudio.google.com/apikey
> **OpenAI-compatible**: Hỗ trợ NVIDIA, OpenRouter, v.v. — xem hướng dẫn trong `config.properties.example`

### Bước 3: Khởi động MySQL (bằng Docker — khuyến nghị)

```bash
docker-compose up -d
```

Hoặc chạy trực tiếp:

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

### Bước 4: Chạy tests (kiểm tra cài đặt thành công)

```bash
mvn clean test
```

**Kết quả mong đợi:** `37 tests passed | 0 failed`

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

**Cách 2 — Chạy JAR trực tiếp:**

```bash
java -jar target/JudgeSystem-1.0-SNAPSHOT.jar
```

---

## 4. Cấu hình Database (tùy chọn)

Nếu muốn thay đổi thông tin kết nối, sửa file `src/main/resources/config.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/JudgeSystem?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC
db.username=root
db.password=123
db.driver=com.mysql.cj.jdbc.Driver
```

Nếu không có file này, ứng dụng sẽ dùng giá trị mặc định (`localhost:3306/JudgeSystem`, `root`/`123`).

---

## 5. Chạy dữ liệu mẫu

```bash
# Nếu dùng Docker
docker exec -i judge-mysql mysql -uroot -p123 JudgeSystem < sample_data.sql

# Nếu cài MySQL trực tiếp
mysql -u root -p JudgeSystem < sample_data.sql
```

Dữ liệu mẫu bao gồm 3 đề: A+B Problem, Prime Check, Sum 1 to N — mỗi đề có testcase + code AC/WA/TLE.

---

## 6. Troubleshooting

| Lỗi                                                | Nguyên nhân                           | Cách khắc phục                                         |
| -------------------------------------------------- | ------------------------------------- | ------------------------------------------------------ |
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | MySQL Connector/J chưa được tải       | `mvn clean install` để tải dependency                  |
| `Communications link failure`                      | MySQL chưa chạy hoặc sai port         | Kiểm tra `docker ps`, đảm bảo port 3306 mở             |
| `g++ is not recognized`                            | MinGW chưa vào PATH                   | Thêm `C:\mingw64\bin` vào System Environment Variables |
| `python: command not found` (Windows)              | Python chưa cài hoặc không trong PATH | Cài Python và đảm bảo `python` có thể gọi từ cmd       |
| Font/UI hiển thị lỗi                               | Java version thấp                     | Đảm bảo JDK 17+ và `JAVA_HOME` đúng                    |
| API key lỗi / AI không phản hồi                    | Chưa cấu hình API key                 | Sửa `config.properties`, thay `YOUR_API_KEY_HERE`      |
| Tests thất bại                                     | MySQL chưa chạy                       | Chạy `docker-compose up -d` trước khi `mvn test`       |

---

## 7. Kiểm tra sau cài đặt

Sau khi chạy app, bạn sẽ thấy:

- **Dashboard** hiển thị số lượng đề thi, testcase, submission.
- Có thể tạo đề mới, chấm code C++/Java/Python, phân tích bằng AI.
- **37 unit/integration tests** đều pass (`mvn clean test`).

Nếu gặp lỗi, kiểm tra log trong terminal hoặc IDE console.
