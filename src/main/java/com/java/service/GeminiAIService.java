package com.java.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.java.model.AIResponse;
import com.java.model.JudgeResult;
import com.java.model.Problem;
import com.java.model.Testcase;
import com.java.util.CodeFormatter;
import okhttp3.*;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiAIService implements AIService {
    private static final String API_URL_TEMPLATE;
    private static final String API_KEY;
    private static final String API_FORMAT;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private static final Gson gson = new Gson();
    private static final String API_MODEL;
    private static final String ZEN_SESSION_ID = java.util.UUID.randomUUID().toString();
    private static final String ZEN_APP_NAME = "opencode";
    private final JudgeEngine judgeEngine;

    public GeminiAIService() {
        this.judgeEngine = new JudgeEngine();
    }

    public GeminiAIService(JudgeEngine judgeEngine) {
        this.judgeEngine = judgeEngine != null ? judgeEngine : new JudgeEngine();
    }

    static {
        Properties props = new Properties();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
        String key = "";
        String format = "gemini";
        String model = "gemini-2.5-flash";
        try (var input = GeminiAIService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
                url = props.getProperty("ai.api.url", url);
                key = props.getProperty("ai.api.key", "");
                format = props.getProperty("ai.format", "gemini");
                model = props.getProperty("ai.model", model);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        API_URL_TEMPLATE = url;
        API_KEY = key;
        API_FORMAT = format;
        API_MODEL = model;
        System.out.println("[AI] Format=" + format + ", Model=" + model + ", URL=" + url);
    }

    @Override
    public AIResponse analyzeProblem(Problem problem) {
        AIResponse response = new AIResponse();
        if (API_KEY == null || API_KEY.isBlank() || API_KEY.contains("YOUR")) {
            response.setSuccess(false);
            response.setErrorMessage("Chưa cấu hình API Key trong config.properties (ai.api.key)");
            return response;
        }

        int problemTimeLimit = problem.getTimeLimit() > 0 ? problem.getTimeLimit() : 2000;

        int maxRetries = 3;
        boolean jsonParseFailed = false;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String prompt = jsonParseFailed ? buildSimplifiedPrompt(problem) : buildAnalyzePrompt(problem);
                String apiResponse = callGeminiAPI(prompt, problem.getImagePath());
                String text = extractTextFromResponse(apiResponse);

                if (text == null || text.isBlank()) {
                    if (attempt < maxRetries) {
                        Thread.sleep(500 * attempt);
                        continue;
                    }
                    response.setSuccess(false);
                    response.setErrorMessage("AI không trả về nội dung. Thử lại sau.");
                    return response;
                }

                String jsonBlock = extractJsonBlock(text);
                JsonObject obj = parseJsonObject(jsonBlock);
                if (obj == null) {
                    System.err.println("[AI] JSON parse failed. Raw response (first 500 chars): " + 
                        (text.length() > 500 ? text.substring(0, 500) : text));
                    System.err.println("[AI] Extracted JSON block (first 500 chars): " + 
                        (jsonBlock.length() > 500 ? jsonBlock.substring(0, 500) : jsonBlock));
                    jsonParseFailed = true;
                    if (attempt < maxRetries) {
                        System.out.println("[AI] Retrying with simplified prompt (attempt " + (attempt + 1) + ")");
                        Thread.sleep(500 * attempt);
                        continue;
                    }
                    response.setSuccess(false);
                    response.setErrorMessage("AI trả về định dạng không đúng JSON. Thử lại.");
                    return response;
                }

                JsonElement expEl = obj.get("explanation");
                response.setExplanation(expEl != null && !expEl.isJsonNull() ? expEl.getAsString() : "");

                JsonElement solEl = obj.get("ac_solution");
                String acCode = (solEl != null && !solEl.isJsonNull()) ? CodeFormatter.formatGeneratedCode(solEl.getAsString(), "java") : "";
                response.setGeneratedSolution(acCode);

                List<Testcase> testcases = new ArrayList<>();
                List<JudgeInput> rawInputs = new ArrayList<>();
                if (obj.has("testcases") && obj.get("testcases").isJsonArray()) {
                    JsonArray tcArray = obj.getAsJsonArray("testcases");
                    for (JsonElement e : tcArray) {
                        JsonObject tcObj = e.getAsJsonObject();
                        String input = tcObj.has("input") ? tcObj.get("input").getAsString() : "";
                        String type = tcObj.has("type") ? tcObj.get("type").getAsString() : "normal";
                        if (!input.isBlank()) rawInputs.add(new JudgeInput(input.trim(), type));
                    }
                }
                if (rawInputs.isEmpty() && obj.has("inputs") && obj.get("inputs").isJsonArray()) {
                    JsonArray inputArray = obj.getAsJsonArray("inputs");
                    for (JsonElement e : inputArray) {
                        JsonObject tcObj = e.getAsJsonObject();
                        String input = tcObj.has("input") ? tcObj.get("input").getAsString() : "";
                        String type = tcObj.has("type") ? tcObj.get("type").getAsString() : "normal";
                        if (!input.isBlank()) rawInputs.add(new JudgeInput(input.trim(), type));
                    }
                }

                if (!rawInputs.isEmpty() && !acCode.isBlank()) {
                    int ceCount = 0;
                    int reCount = 0;
                    int tleCount = 0;
                    int blankOutputCount = 0;
                    int tleValidated = 0;
                    int tleRejected = 0;
                    int tleTotal = 0;
                    for (JudgeInput ji : rawInputs) {
                        int timeLimit = problemTimeLimit * 2;
                        JudgeResult jr = judgeEngine.judge(acCode, "java", ji.input, "", timeLimit, 256);
                        if ("CE".equals(jr.getStatus())) {
                            ceCount++;
                            System.err.println("[AI] CE on input type=" + ji.type + ": " + jr.getErrorMessage());
                            continue;
                        }
                        if ("RE".equals(jr.getStatus())) {
                            reCount++;
                            System.err.println("[AI] RE on input type=" + ji.type + ": " + jr.getErrorMessage());
                            continue;
                        }
                        if ("TLE".equals(jr.getStatus())) {
                            tleCount++;
                            System.err.println("[AI] TLE on input type=" + ji.type + " with timeLimit=" + timeLimit + "ms - AC code should not TLE, retry needed");
                            continue;
                        }
                        if (jr.getActualOutput() == null || jr.getActualOutput().isBlank()) {
                            blankOutputCount++;
                            System.err.println("[AI] Blank output on input type=" + ji.type);
                            continue;
                        }

                        if (ji.type.equals("anti-tle") || ji.type.equals("stress")) {
                            tleTotal++;
                            if (validateTleEffectiveness(ji.input, jr.getExecutionTime(), problemTimeLimit)) {
                                tleValidated++;
                                System.out.println("[AI] TLE validation PASSED for " + ji.type + " testcase (n large, AC time=" + jr.getExecutionTime() + "ms)");
                            } else {
                                tleRejected++;
                                System.err.println("[AI] TLE validation FAILED for " + ji.type + " testcase - input too small or AC too fast, REJECTING");
                                continue;
                            }
                        }

                        Testcase tc = new Testcase();
                        tc.setInputData(ji.input);
                        tc.setExpectedOutput(jr.getActualOutput().trim());
                        tc.setTestcaseType(ji.type);
                        tc.setAiGenerated(true);
                        testcases.add(tc);
                    }
                    if (ceCount > 0) {
                        System.err.println("[AI] " + ceCount + " testcase(s) caused Compile Error in AC code");
                    }
                    if (reCount > 0) {
                        System.err.println("[AI] " + reCount + " testcase(s) caused Runtime Error");
                    }
                    if (tleCount > 0) {
                        System.err.println("[AI] " + tleCount + " testcase(s) caused TLE for AC code - AC code may not be optimal");
                    }
                    if (blankOutputCount > 0) {
                        System.err.println("[AI] " + blankOutputCount + " testcase(s) produced blank output");
                    }
                    if (tleTotal > 0) {
                        System.out.println("[AI] TLE validation: " + tleValidated + "/" + tleTotal + " effective, " + tleRejected + " rejected");
                    }
                }

                if (testcases.isEmpty()) {
                    String detailedError;
                    if (rawInputs.isEmpty() && acCode.isBlank()) {
                        detailedError = "AI không sinh được inputs lẫn code AC. Kiểm tra lại đề bài hoặc thử lại.";
                    } else if (rawInputs.isEmpty()) {
                        detailedError = "AI sinh được code AC nhưng KHÔNG có inputs trong JSON response. Thử lại.";
                    } else if (acCode.isBlank()) {
                        detailedError = "AI sinh được inputs nhưng KHÔNG có code AC. Hệ thống vẫn có thể lưu inputs (không có expected output).";
                        for (JudgeInput ji : rawInputs) {
                            Testcase tc = new Testcase();
                            tc.setInputData(ji.input);
                            tc.setExpectedOutput("[PENDING - cần chạy code AC để tính output]");
                            tc.setTestcaseType(ji.type);
                            tc.setAiGenerated(true);
                            testcases.add(tc);
                        }
                        System.out.println("[AI] Saved " + testcases.size() + " inputs without expected output (fallback mode)");
                    } else {
                        detailedError = "Tất cả " + rawInputs.size() + " inputs đều bị lỗi khi chạy AC code (CE/RE/TLE/blank). Kiểm tra code AC hoặc đề bài.";
                    }
                    if (attempt < maxRetries) {
                        System.err.println("[AI] Attempt " + attempt + " failed: " + detailedError);
                        Thread.sleep(500 * attempt);
                        continue;
                    }
                    response.setSuccess(false);
                    response.setErrorMessage(detailedError);
                    return response;
                }

                // FALLBACK: Auto-generate large testcases if AI didn't provide any with n >= 500
                if (!acCode.isBlank()) {
                    boolean hasLargeTestcase = false;
                    for (Testcase tc : testcases) {
                        int n = extractN(tc.getInputData());
                        if (n >= 500) {
                            hasLargeTestcase = true;
                            break;
                        }
                    }
                    if (!hasLargeTestcase) {
                        System.out.println("[AI] No large testcase (n>=500) found. Auto-generating anti-tle/stress testcases...");
                        List<Testcase> largeTestcases = generateLargeTestcases(acCode, problemTimeLimit);
                        testcases.addAll(largeTestcases);
                        System.out.println("[AI] Added " + largeTestcases.size() + " auto-generated large testcases");
                    }
                }

                response.setTestcases(testcases);
                response.setSuccess(true);
                return response;
            } catch (Exception e) {
                System.err.println("[AI] analyzeProblem attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < maxRetries) {
                    try { Thread.sleep(500 * attempt); } catch (InterruptedException ignored) {}
                    continue;
                }
                response.setSuccess(false);
                response.setErrorMessage("Lỗi: " + e.getMessage());
            }
        }
        return response;
    }

    @Override
    public String generateSolution(Problem problem, String language, String expectedType) {
        int maxRetries = 2;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String behavior = "GIẢI ĐÚNG HOÀN TOÀN (Accepted), tối ưu về thời gian và bộ nhớ";
                if ("WA".equals(expectedType)) behavior = "CỐ TÌNH VIẾT SAI LOGIC (Wrong Answer) ở một số trường hợp đặc biệt nhưng cấu trúc vẫn hợp lệ (in ra kết quả sai). BẮT BUỘC phải sai.";
                else if ("TLE".equals(expectedType)) behavior = "CỐ TÌNH VIẾT CODE CHẠY RẤT CHẬM (Time Limit Exceeded) bằng cách dùng vòng lặp lồng nhau vô ích hoặc đệ quy không tối ưu (ví dụ O(n^2) hoặc O(2^n)). BẮT BUỘC phải rất chậm.";

                String prompt = "Mày đóng vai một lập trình viên thi đấu. Hãy viết code bằng ngôn ngữ " + language + " cho bài toán dưới đây.\n"
                              + "YÊU CẦU: Code phải " + behavior + ".\n"
                              + "QUY TẮC: Trả về JSON hợp lệ với duy nhất 1 key \"code\" chứa toàn bộ source code.\n\n"
                              + "ĐỀ BÀI:\n" + problem.getTitle() + "\n" + problem.getDescription();
                String apiResponse = callGeminiAPI(prompt, problem.getImagePath());
                String text = extractTextFromResponse(apiResponse);
                String code = extractCodeFromText(text, language);
                if (code != null && !code.isBlank()) {
                    return CodeFormatter.formatGeneratedCode(code, language);
                }
                if (i < maxRetries - 1) {
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                System.err.println("[AI] generateSolution attempt " + (i+1) + " failed: " + e.getMessage());
                if (i < maxRetries - 1) {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                }
            }
        }
        return "// Lỗi sinh code: API quá tải sau nhiều lần thử.";
    }

    @Override
    public String generateChecker(Problem problem) {
        try {
            String prompt = "Hãy viết script Python checker cho bài toán lập trình thi đấu dưới đây.\n"
                          + "Checker nhận 3 tham số dòng lệnh: input_file, expected_output_file, actual_output_file (chuẩn Codeforces).\n"
                          + "Script đọc cả 3 file, so sánh expected và actual, in ra 'OK' nếu đúng, 'WA' nếu sai.\n"
                          + "Trả về JSON hợp lệ với duy nhất 1 key \"code\" chứa toàn bộ script.\n\n"
                          + "ĐỀ BÀI:\n" + problem.getTitle() + "\n" + problem.getDescription();
            String apiResponse = callGeminiAPI(prompt, problem.getImagePath());
            String text = extractTextFromResponse(apiResponse);
            String code = extractCodeFromText(text, "python");
            if (code != null && !code.isBlank()) {
                return code;
            }
            return "# Không thể sinh checker. Vui lòng thử lại.";
        } catch (Exception e) {
            return "# Lỗi sinh checker: " + e.getMessage();
        }
    }

    private String buildAnalyzePrompt(Problem problem) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là chuyên gia ra đề thi lập trình thi đấu (ICPC/IOI/Codeforces). Nhiệm vụ: phân tích đề và sinh testcase ĐỦ MẠNH để phân biệt rõ 3 loại code: AC (đúng), WA (sai logic), TLE (chậm).\n\n");
        sb.append("YÊU CẦU BẮT BUỘC:\n");
        sb.append("1. Code AC (key: \"ac_solution\"): Java, đúng hoàn toàn, tối ưu thời gian & bộ nhớ.\n");
        sb.append("   - Code PHẢI biên dịch được với `javac` (Java 17+).\n");
        sb.append("   - Dùng `class Main` với `public static void main(String[] args)`.\n");
        sb.append("   - Dùng `Scanner` hoặc `BufferedReader` để đọc input từ stdin.\n");
        sb.append("   - In kết quả ra stdout bằng `System.out.println()`.\n");
        sb.append("   - KHÔNG dùng package declaration.\n");
        sb.append("   - Xử lý đúng các trường hợp biên (n=1, giá trị âm, overflow).\n\n");
        sb.append("2. Testcase (key: \"inputs\"): SINH ÍT NHẤT 10-15 testcase, bao phủ đủ 6 nhóm sau:\n");
        sb.append("   [a] SMALL (1-2 cái): Input nhỏ nhất. VD: n=1, mảng 1 phần tử.\n");
        sb.append("   [b] NORMAL (2-3 cái): Input trung bình, trường hợp điển hình.\n");
        sb.append("   [c] EDGE/BOUNDARY (2-3 cái): Giá trị biên — n=1, n=max, giá trị âm, 0, overflow int.\n");
        sb.append("   [d] ANTI-WA (2-3 cái): Input mà code sai phổ biến sẽ FAIL — thiếu modulo, sai công thức, off-by-one.\n");
        sb.append("   [e] ANTI-TLE (2-3 cái): BẮT BUỘC n >= 500. VD: n=500 cho bài O(n³), n=5000 cho bài O(n²), n=100000 cho bài O(n). Input phải ĐỦ LỚN để code chậm bị TLE trong time limit, nhưng code tối ưu vẫn AC.\n");
        sb.append("   [f] STRESS (2-3 cái): Input LỚN NHẤT — n >= 1000. Kiểm tra giới hạn tuyệt đối.\n\n");
        sb.append("   ⚠️ QUY TẮC VÀNG CHO ANTI-TLE/STRESS:\n");
        sb.append("   - ANTI-TLE: n PHẢI >= 500. Nếu n < 500, testcase sẽ bị HỆ THỐNG TỰ ĐỘNG LOẠI BỎ.\n");
        sb.append("   - STRESS: n PHẢI >= 1000. Đây là testcase lớn nhất, kiểm tra giới hạn tuyệt đối.\n");
        sb.append("   - KHÔNG dùng tất cả giá trị giống nhau (VD: toàn 500 hoặc toàn 1). JIT sẽ optimize loop thành constant → code O(n²) vẫn AC!\n");
        sb.append("   - DÙNG GIÁ TRỊ NGẪU NHIÊN trong khoảng [-500, +500] để phá branch prediction.\n");
        sb.append("   - Ví dụ đúng: n=500, mảng gồm 500 số ngẫu nhiên trong [-500, 500].\n");
        sb.append("   - Ví dụ đúng: n=1000, mảng gồm 1000 số ngẫu nhiên trong [-500, 500].\n");
        sb.append("   - Tránh pattern lặp (VD: 1 2 3 1 2 3) vì JIT có thể nhận ra và optimize.\n");
        sb.append("   - Gợi ý: Dùng seed cố định (VD: random.seed(42)) để testcase reproducible.\n\n");
        sb.append("   ⚠️ LƯU Ý VỀ CONSTRAINT:\n");
        sb.append("   - Nếu đề bài KHÔNG ghi rõ constraint, hãy TỰ ĐẶT constraint hợp lý:\n");
        sb.append("     + n tối đa = 500 cho bài O(n³)\n");
        sb.append("     + n tối đa = 5000 cho bài O(n²)\n");
        sb.append("     + n tối đa = 100000 cho bài O(n log n) hoặc O(n)\n");
        sb.append("     + Giá trị mảng trong khoảng [-10^9, 10^9]\n");
        sb.append("   - Testcase anti-tle/stress PHẢI dùng n gần max constraint.\n\n");
        sb.append("   QUY TẮC:\n");
        sb.append("   - Chỉ sinh \"input\" (AI KHÔNG tính output). Hệ thống sẽ chạy code AC để lấy output chính xác.\n");
        sb.append("   - Mỗi testcase ghi rõ \"type\": \"small\"|\"normal\"|\"edge\"|\"anti-wa\"|\"anti-tle\"|\"stress\".\n");
        sb.append("   - Input phải hợp lệ theo ràng buộc đề bài.\n");
        sb.append("   - Anti-WA: nghĩ đến lỗi thường gặp (thiếu long, sai hướng, quên sort, ...) và tạo input bắt được lỗi đó.\n");
        sb.append("   - Input format: dùng newline (\\n) để phân cách dòng, space để phân cách số trên cùng dòng.\n\n");
        sb.append("3. Giải thích ngắn (key: \"explanation\"): Thuật toán + độ phức tạp + tại sao testcase đủ mạnh.\n\n");
        sb.append("QUY TẮC: Chỉ trả về JSON hợp lệ, KHÔNG thêm text ngoài JSON.\n\n");
        sb.append("ĐỊNH DẠNG JSON:\n");
        sb.append("{\n");
        sb.append("  \"explanation\": \"Thuật toán O(...) + lý do testcase mạnh\",\n");
        sb.append("  \"ac_solution\": \"import java.util.*;\\npublic class Main { ... }\",\n");
        sb.append("  \"inputs\": [\n");
        sb.append("    {\"type\": \"small\",     \"input\": \"...\"},\n");
        sb.append("    {\"type\": \"normal\",    \"input\": \"...\"},\n");
        sb.append("    {\"type\": \"edge\",      \"input\": \"...\"},\n");
        sb.append("    {\"type\": \"anti-wa\",   \"input\": \"...\"},\n");
        sb.append("    {\"type\": \"anti-tle\",  \"input\": \"...\"},\n");
        sb.append("    {\"type\": \"stress\",    \"input\": \"...\"}\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("ĐỀ THI:\n");
        sb.append("Tiêu đề: ").append(problem.getTitle()).append("\n");
        if (problem.getDescription() != null && !problem.getDescription().isBlank()) {
            sb.append("Nội dung:\n").append(problem.getDescription()).append("\n");
        } else {
            sb.append("(Nội dung đề bài nằm trong ảnh đính kèm. Hãy đọc ảnh để hiểu đề.)\n");
        }
        if (problem.getContestType() != null && !problem.getContestType().isBlank()) {
            sb.append("Loại kỳ thi: ").append(problem.getContestType()).append("\n");
        }
        sb.append("Giới hạn thời gian: ").append(problem.getTimeLimit()).append("ms\n");
        sb.append("Giới hạn bộ nhớ: ").append(problem.getMemoryLimit()).append("MB\n");
        return sb.toString();
    }

    /**
     * Simplified prompt for retry when JSON parsing fails.
     * Removes complex instructions and focuses on core requirements.
     */
    private String buildSimplifiedPrompt(Problem problem) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sinh testcase cho bài toán lập trình thi đấu. Trả về JSON hợp lệ.\n\n");
        sb.append("Yêu cầu:\n");
        sb.append("1. Code Java AC tối ưu (key: \"ac_solution\") - PHẢI biên dịch được với javac, dùng class Main\n");
        sb.append("2. 8-12 testcase (key: \"inputs\") với các type: small, normal, edge, anti-wa, anti-tle, stress\n");
        sb.append("3. Anti-tle: dùng n=max constraint, giá trị ngẫu nhiên [-MAX, +MAX]\n");
        sb.append("4. Giải thích ngắn (key: \"explanation\")\n\n");
        if (problem.getDescription() == null || problem.getDescription().isBlank()) {
            sb.append("(Đề bài nằm trong ảnh đính kèm. Hãy đọc ảnh để hiểu đề.)\n\n");
        }
        sb.append("Định dạng JSON:\n");
        sb.append("{\"explanation\":\"...\",\"ac_solution\":\"...\",\"inputs\":[{\"type\":\"...\",\"input\":\"...\"}]}\n\n");
        sb.append("Đề thi:\n");
        sb.append("Tiêu đề: ").append(problem.getTitle()).append("\n");
        if (problem.getDescription() != null && !problem.getDescription().isBlank()) {
            sb.append("Nội dung: ").append(problem.getDescription()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Lenient JSON parser - handles malformed JSON
     */
    private JsonObject parseJsonObject(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonReader reader = new JsonReader(new StringReader(json));
            reader.setLenient(true);
            return gson.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            System.err.println("[AI] parseJsonObject failed: " + e.getMessage());
            return null;
        }
    }

    private String extractJsonBlock(String text) {
        if (text == null) return "";
        // Remove markdown code fences
        text = text.replaceAll("(?s)```(?:json)?\\s*", "").replaceAll("```\\s*$", "").trim();
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String extractCodeFromText(String text, String language) {
        if (text == null || text.isBlank()) return "";

        // Try markdown code blocks first
        String[] markers = {"```" + language, "```java", "```python", "```cpp", "```c", "```"};
        for (String marker : markers) {
            int idx = text.indexOf(marker);
            if (idx != -1) {
                int codeStart = text.indexOf('\n', idx);
                if (codeStart == -1) codeStart = idx + marker.length();
                else codeStart++;
                int endIdx = text.indexOf("```", codeStart);
                if (endIdx != -1) {
                    return text.substring(codeStart, endIdx).trim();
                }
            }
        }

        // Try JSON parsing with lenient mode
        try {
            String jsonBlock = extractJsonBlock(text);
            if (jsonBlock.startsWith("{")) {
                JsonObject obj = parseJsonObject(jsonBlock);
                if (obj != null && obj.has("code") && !obj.get("code").isJsonNull()) {
                    JsonElement codeEl = obj.get("code");
                    if (codeEl.isJsonPrimitive()) {
                        return codeEl.getAsString();
                    }
                }
            }
        } catch (Exception ignored) {}

        // Fallback: return text as-is
        return text.trim();
    }

    private String callGeminiAPI(String textPrompt, String imagePath) throws IOException {
        boolean isOpenAI = "openai".equalsIgnoreCase(API_FORMAT);
        String apiUrl = isOpenAI ? API_URL_TEMPLATE : API_URL_TEMPLATE + API_KEY;

        JsonObject requestBody = new JsonObject();

        if (isOpenAI) {
            JsonArray messages = new JsonArray();
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", "You must respond with valid JSON only. No markdown, no code fences, no extra text.");
            messages.add(systemMsg);

            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", textPrompt);
            messages.add(message);
            requestBody.add("messages", messages);
            requestBody.addProperty("model", API_MODEL);
            requestBody.addProperty("temperature", 0.1);
            requestBody.addProperty("max_tokens", 8192);
            requestBody.addProperty("stream", false);
            if (API_MODEL.contains("kimi")) {
                JsonObject chatTemplateKwargs = new JsonObject();
                chatTemplateKwargs.addProperty("thinking", true);
                requestBody.add("chat_template_kwargs", chatTemplateKwargs);
            }
        } else {
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", textPrompt);
            parts.add(textPart);

            if (imagePath != null && !imagePath.isBlank() && Files.exists(Paths.get(imagePath))) {
                Path attachmentPath = Paths.get(imagePath);
                byte[] imageBytes = Files.readAllBytes(attachmentPath);
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                String mimeType = detectMimeType(attachmentPath);
                JsonObject inlineData = new JsonObject();
                inlineData.addProperty("mimeType", mimeType);
                inlineData.addProperty("data", base64Image);
                JsonObject imagePart = new JsonObject();
                imagePart.add("inlineData", inlineData);
                parts.add(imagePart);
            }

            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.1);
            generationConfig.addProperty("maxOutputTokens", 32768);
            generationConfig.addProperty("responseMimeType", "application/json");
            requestBody.add("generationConfig", generationConfig);
        }

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request.Builder reqBuilder = new Request.Builder()
                .url(apiUrl)
                .post(body);
        if (isOpenAI) {
            reqBuilder.header("Authorization", "Bearer " + API_KEY);
            reqBuilder.header("Content-Type", "application/json");
            reqBuilder.header("x-opencode-client", "cli");
            reqBuilder.header("x-opencode-session", ZEN_SESSION_ID);
            reqBuilder.header("x-opencode-app", ZEN_APP_NAME);
            reqBuilder.header("User-Agent", "opencode/latest/1.3.15/cli");
        }

        try (Response response = client.newCall(reqBuilder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                if (response.code() == 429 && responseBody.contains("FreeUsageLimitError")) {
                    throw new IOException("HTTP 429: Đã hết hạn ngạch free trong ngày của OpenCode Zen. Hãy nạp credit tại https://opencode.ai/zen hoặc đợi 24h để làm mới.\n" + responseBody);
                }
                throw new IOException("HTTP " + response.code() + ": " + responseBody);
            }
            return responseBody;
        }
    }

    private String extractTextFromResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return "";
        }

        boolean isOpenAI = "openai".equalsIgnoreCase(API_FORMAT);

        // Handle SSE/streaming format: "data: {...}\n\ndata: {...}"
        if (jsonResponse.contains("data:")) {
            String content = extractFromSSE(jsonResponse);
            if (content != null && !content.isBlank()) {
                return content;
            }
        }

        // Try lenient JSON parsing
        try {
            JsonReader reader = new JsonReader(new StringReader(jsonResponse));
            reader.setLenient(true);
            JsonElement rootElement = gson.fromJson(reader, JsonElement.class);

            if (rootElement != null && rootElement.isJsonObject()) {
                JsonObject root = rootElement.getAsJsonObject();

                if (isOpenAI) {
                    JsonArray choices = root.getAsJsonArray("choices");
                    if (choices != null && !choices.isEmpty()) {
                        JsonObject choice = choices.get(0).getAsJsonObject();
                        JsonObject message = choice.getAsJsonObject("message");
                        if (message != null) {
                            if (message.has("content") && !message.get("content").isJsonNull()) {
                                String content = message.get("content").getAsString();
                                if (!content.isBlank()) return content;
                            }
                            if (message.has("reasoning_content") && !message.get("reasoning_content").isJsonNull()) {
                                String reasoning = message.get("reasoning_content").getAsString();
                                if (!reasoning.isBlank()) return reasoning;
                            }
                        }
                    }
                    if (root.has("error")) {
                        JsonObject error = root.getAsJsonObject("error");
                        String errMsg = error.has("message") ? error.get("message").getAsString() : error.toString();
                        throw new RuntimeException("API Error: " + errMsg);
                    }
                } else {
                    JsonArray candidates = root.getAsJsonArray("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        JsonObject candidate = candidates.get(0).getAsJsonObject();
                        JsonObject content = candidate.getAsJsonObject("content");
                        if (content != null) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts != null) {
                                StringBuilder sb = new StringBuilder();
                                for (JsonElement part : parts) {
                                    JsonObject p = part.getAsJsonObject();
                                    if (p.has("text")) {
                                        sb.append(p.get("text").getAsString());
                                    }
                                }
                                return sb.toString();
                            }
                        }
                    }
                }
            } else if (rootElement != null && rootElement.isJsonPrimitive()) {
                return rootElement.getAsString();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("[AI] Failed to parse API response with lenient parser: " + e.getMessage());
        }

        // Fallback: extract content using regex
        if (isOpenAI) {
            String content = extractContentByRegex(jsonResponse);
            if (content != null && !content.isBlank()) {
                return content;
            }
        }

        // Last resort: return raw response
        System.err.println("[AI] Could not parse response. Raw (first 300 chars): " +
                (jsonResponse.length() > 300 ? jsonResponse.substring(0, 300) : jsonResponse));
        return jsonResponse;
    }

    /**
     * Extract content from SSE (Server-Sent Events) streaming format
     */
    private String extractFromSSE(String sseResponse) {
        StringBuilder allContent = new StringBuilder();
        String[] lines = sseResponse.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) continue;
                if (data.isEmpty()) continue;
                try {
                    JsonReader reader = new JsonReader(new StringReader(data));
                    reader.setLenient(true);
                    JsonObject obj = gson.fromJson(reader, JsonObject.class);
                    if (obj != null) {
                        JsonArray choices = obj.getAsJsonArray("choices");
                        if (choices != null && !choices.isEmpty()) {
                            JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
                            if (delta != null && delta.has("content") && !delta.get("content").isJsonNull()) {
                                allContent.append(delta.get("content").getAsString());
                            }
                        }
                        // Also check for non-streaming format in SSE
                        JsonObject message = obj.getAsJsonObject("message");
                        if (message != null && message.has("content") && !message.get("content").isJsonNull()) {
                            return message.get("content").getAsString();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return allContent.length() > 0 ? allContent.toString() : null;
    }

    /**
     * Extract content field using regex as last resort
     */
    private String extractContentByRegex(String raw) {
        // Try to find "content": "..." pattern
        Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher matcher = pattern.matcher(raw);
        if (matcher.find()) {
            String content = matcher.group(1);
            // Unescape JSON string
            content = content.replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\t", "\t");
            return content;
        }
        return null;
    }

    private String detectMimeType(Path path) {
        try {
            String detected = Files.probeContentType(path);
            if (detected != null && !detected.isBlank()) {
                return detected;
            }
        } catch (IOException ignored) {}

        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".bmp")) return "image/bmp";
        return "image/png";
    }

    private static class JudgeInput {
        final String input;
        final String type;
        JudgeInput(String input, String type) { this.input = input; this.type = type; }
    }

    boolean validateTleEffectiveness(String input, int acExecutionTime, int problemTimeLimit) {
        try {
            String[] lines = input.trim().split("\n");
            if (lines.length > 0) {
                try {
                    String firstToken = lines[0].trim().split("\\s+")[0];
                    int n = Integer.parseInt(firstToken);

                    if (n < 500) {
                        System.out.println("[AI] TLE reject: Small input (n=" + n + ") for anti-tle test case (min n=500)");
                        return false;
                    }

                    double acTimeRatio = (double) acExecutionTime / problemTimeLimit;
                    if (n > 10000 && acTimeRatio < 0.05) {
                        System.out.println("[AI] TLE reject: Large input (n=" + n + ") but AC code runs too fast (" + acExecutionTime + "ms / " + problemTimeLimit + "ms = " + String.format("%.1f", acTimeRatio * 100) + "%). Threshold: 5%");
                        return false;
                    }

                    return true;
                } catch (NumberFormatException e) {
                    return true;
                }
            }

            return true;
        } catch (Exception e) {
            System.err.println("[AI] TLE validation error: " + e.getMessage());
            return true;
        }
    }

    private int extractN(String input) {
        try {
            String[] lines = input.trim().split("\n");
            if (lines.length > 0) {
                String firstToken = lines[0].trim().split("\\s+")[0];
                return Integer.parseInt(firstToken);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private List<Testcase> generateLargeTestcases(String acCode, int problemTimeLimit) {
        List<Testcase> largeTestcases = new ArrayList<>();
        java.util.Random rng = new java.util.Random(42);

        int[] sizes = {500, 1000};
        String[] types = {"anti-tle", "stress"};

        for (int i = 0; i < sizes.length; i++) {
            int n = sizes[i];
            String type = types[i];

            StringBuilder sb = new StringBuilder();
            sb.append(n).append("\n");
            for (int j = 0; j < n; j++) {
                if (j > 0) sb.append(" ");
                sb.append(rng.nextInt(1001) - 500);
            }
            sb.append("\n");
            String input = sb.toString();

            int timeLimit = problemTimeLimit * 2;
            JudgeResult jr = judgeEngine.judge(acCode, "java", input, "", timeLimit, 256);

            if (!"CE".equals(jr.getStatus()) && !"RE".equals(jr.getStatus())
                    && !"TLE".equals(jr.getStatus())
                    && jr.getActualOutput() != null && !jr.getActualOutput().isBlank()) {
                Testcase tc = new Testcase();
                tc.setInputData(input);
                tc.setExpectedOutput(jr.getActualOutput().trim());
                tc.setTestcaseType(type);
                tc.setAiGenerated(true);
                largeTestcases.add(tc);
                System.out.println("[AI] Auto-generated " + type + " testcase: n=" + n + ", AC time=" + jr.getExecutionTime() + "ms");
            } else {
                System.err.println("[AI] Failed to generate " + type + " testcase (n=" + n + "): " + jr.getStatus());
            }
        }

        return largeTestcases;
    }
}
