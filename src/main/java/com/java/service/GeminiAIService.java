package com.java.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.java.model.AIResponse;
import com.java.model.JudgeResult;
import com.java.model.Problem;
import com.java.model.Testcase;
import com.java.util.CodeFormatter;
import okhttp3.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

public class GeminiAIService implements AIService {
    private static final String API_URL_TEMPLATE;
    private static final String API_KEY;
    private static final String API_FORMAT; // "gemini" or "openai"
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private static final Gson gson = new Gson();

    private static final String API_MODEL;

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
    }

    @Override
    public AIResponse analyzeProblem(Problem problem) {
        AIResponse response = new AIResponse();
        if (API_KEY == null || API_KEY.isBlank() || API_KEY.contains("YOUR")) {
            response.setSuccess(false);
            response.setErrorMessage("Chưa cấu hình API Key trong config.properties (ai.api.key)");
            return response;
        }

        try {
            // Bước 1: Gọi AI — chỉ lấy inputs + code AC, KHÔNG lấy output
            String prompt = buildAnalyzePrompt(problem);
            String jsonResponse = callGeminiAPI(prompt, problem.getImagePath());
            String text = extractTextFromResponse(jsonResponse);

            // Bước 2: Parse JSON lấy inputs và ac_solution
            String jsonBlock = extractJsonBlock(text);
            JsonObject obj = gson.fromJson(jsonBlock, JsonObject.class);

            JsonElement expEl = obj.get("explanation");
            response.setExplanation(expEl != null && !expEl.isJsonNull() ? expEl.getAsString() : "");

            JsonElement solEl = obj.get("ac_solution");
            String acCode = (solEl != null && !solEl.isJsonNull()) ? CodeFormatter.formatGeneratedCode(solEl.getAsString(), "java") : "";
            response.setGeneratedSolution(acCode);

            // Bước 3: Chạy code AC để tính output chính xác cho từng input
            List<Testcase> testcases = new ArrayList<>();
            if (obj.has("inputs") && obj.get("inputs").isJsonArray()) {
                JudgeEngine engine = new JudgeEngine();
                JsonArray inputArray = obj.getAsJsonArray("inputs");
                for (JsonElement e : inputArray) {
                    JsonObject tcObj = e.getAsJsonObject();
                    String input = tcObj.has("input") ? tcObj.get("input").getAsString() : "";
                    String type  = tcObj.has("type")  ? tcObj.get("type").getAsString()  : "normal";
                    if (input.isBlank()) continue;

                    if (acCode.isBlank()) {
                        // Không có code AC → không thể tính output
                        response.setSuccess(false);
                        response.setErrorMessage("AI không sinh được code AC để tính output. Thử lại.");
                        return response;
                    }

                    // Chạy code AC với input này
                    JudgeResult jr = engine.judge(acCode, "java", input, "", 10000, 256);
                    if ("CE".equals(jr.getStatus())) {
                        response.setSuccess(false);
                        response.setErrorMessage("Code AC bị lỗi biên dịch (CE):\n" + jr.getErrorMessage()
                            + "\nHãy thử lại để AI sinh code AC khác.");
                        return response;
                    }
                    if ("RE".equals(jr.getStatus()) || "TLE".equals(jr.getStatus())
                            || jr.getActualOutput() == null || jr.getActualOutput().isBlank()) {
                        continue;
                    }

                    Testcase tc = new Testcase();
                    tc.setInputData(input);
                    tc.setExpectedOutput(jr.getActualOutput().trim());
                    tc.setTestcaseType(type);
                    tc.setAiGenerated(true);
                    testcases.add(tc);
                }
            }

            if (testcases.isEmpty()) {
                response.setSuccess(false);
                response.setErrorMessage("Không tạo được testcase nào. Kiểm tra lại đề bài và thử lại.");
                return response;
            }

            response.setTestcases(testcases);
            response.setSuccess(true);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("Lỗi: " + e.getMessage());
        }
        return response;
    }

    @Override
    public String generateSolution(Problem problem, String language, String expectedType) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String behavior = "GIẢI ĐÚNG HOÀN TOÀN (Accepted), tối ưu về thời gian và bộ nhớ";
                if ("WA".equals(expectedType)) behavior = "CỐ TÌNH VIẾT SAI LOGIC (Wrong Answer) ở một số trường hợp đặc biệt nhưng cấu trúc vẫn hợp lệ (in ra kết quả sai). BẮT BUỘC phải sai.";
                else if ("TLE".equals(expectedType)) behavior = "CỐ TÌNH VIẾT CODE CHẠY RẤT CHẬM (Time Limit Exceeded) bằng cách dùng vòng lặp lồng nhau vô ích hoặc đệ quy không tối ưu (ví dụ O(n^2) hoặc O(2^n)). BẮT BUỘC phải rất chậm.";
                
                String prompt = "Mày đóng vai một lập trình viên. Hãy viết code bằng ngôn ngữ " + language + " cho bài toán dưới đây.\n"
                              + "YÊU CẦU ĐẶC BIỆT 1: Mã này phải " + behavior + ".\n"
                              + "YÊU CẦU ĐẶC BIỆT 2: Được phép sử dụng TẤT CẢ các thư viện tiêu chuẩn (Standard Libraries) có sẵn của ngôn ngữ này.\n"
                              + "QUY TẮC BẮT BUỘC: Chỉ trả về định dạng JSON với duy nhất 1 key là \"code\" chứa toàn bộ source code.\n\n"
                              + "ĐỀ BÀI:\n" + problem.getTitle() + "\n" + problem.getDescription();
                String jsonResponse = callGeminiAPI(prompt, problem.getImagePath());
                JsonObject obj = gson.fromJson(extractTextFromResponse(jsonResponse), JsonObject.class);
                if (obj != null && obj.has("code")) {
                    return CodeFormatter.formatGeneratedCode(obj.get("code").getAsString(), language);
                }
                return CodeFormatter.formatGeneratedCode(extractTextFromResponse(jsonResponse), language);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("503") && i < maxRetries - 1) {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    continue; // Retry on 503
                }
                return "// Lỗi sinh code: " + e.getMessage();
            }
        }
        return "// Lỗi sinh code: API quá tải sau nhiều lần thử, vui lòng thử lại sau.";
    }

    @Override
    public String generateChecker(Problem problem) {
        try {
            String prompt = "Hãy viết script Python checker. Trả về định dạng JSON với 1 key duy nhất là \"code\".\n\n" + problem.getTitle() + "\n" + problem.getDescription();
            String jsonResponse = callGeminiAPI(prompt, problem.getImagePath());
            JsonObject obj = gson.fromJson(extractTextFromResponse(jsonResponse), JsonObject.class);
            if (obj != null && obj.has("code")) return obj.get("code").getAsString();
            return extractTextFromResponse(jsonResponse);
        } catch (Exception e) {
            return "# Lỗi sinh checker: " + e.getMessage();
        }
    }

    private String buildAnalyzePrompt(Problem problem) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là chuyên gia lp trình thi đấu. Hãy thực hiện 2 việc:\n");
        sb.append("1. Viết CODE JAVA giải đúng hoàn toàn bài toán (chỉ dùng java.util.Scanner, không import khác).\n");
        sb.append("2. Sinh 5 INPUT đa dạng: small, large, edge cases (n=1, tất cả âm, tất cả bằng nhau).\n\n");
        sb.append("QUY TẮc: Chỉ trả về JSON, KHÔNG thêm text ngoài JSON, KHÔNG có markdown code block.\n\n");
        sb.append("{\n");
        sb.append("  \"explanation\": \"Mô tả ngắn thuật toán\",\n");
        sb.append("  \"ac_solution\": \"import java.util.Scanner;\\npublic class Main { ... }\",\n");
        sb.append("  \"inputs\": [\n");
        sb.append("    {\"type\": \"small\", \"input\": \"dữ liệu input thực tế\"},\n");
        sb.append("    {\"type\": \"edge\",  \"input\": \"...\"},\n");
        sb.append("    {\"type\": \"large\", \"input\": \"...\"}\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("ĐỀ THI:\n");
        sb.append("Tiêu đề: ").append(problem.getTitle()).append("\n");
        sb.append("Nội dung:\n").append(problem.getDescription()).append("\n");
        if (problem.getContestType() != null && !problem.getContestType().isBlank()) {
            sb.append("Loại kỳ thi: ").append(problem.getContestType()).append("\n");
        }
        sb.append("Giới hạn thời gian: ").append(problem.getTimeLimit()).append("ms\n");
        sb.append("Giới hạn bộ nhớ: ").append(problem.getMemoryLimit()).append("MB\n");
        return sb.toString();
    }

    private String extractJsonBlock(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String callGeminiAPI(String textPrompt, String imagePath) throws IOException {
        boolean isOpenAI = "openai".equalsIgnoreCase(API_FORMAT);
        String apiUrl = isOpenAI ? API_URL_TEMPLATE : API_URL_TEMPLATE + API_KEY;

        JsonObject requestBody = new JsonObject();

        if (isOpenAI) {
            // OpenAI-compatible format (NVIDIA NIM, MiniMax, OpenAI, etc.)
            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", textPrompt);
            messages.add(message);
            requestBody.add("messages", messages);
            requestBody.addProperty("model", API_MODEL);
            requestBody.addProperty("temperature", 0.2);
            requestBody.addProperty("max_tokens", 32768);
        } else {
            // Google Gemini format
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", textPrompt);
            parts.add(textPart);

            if (imagePath != null && !imagePath.isBlank() && Files.exists(Paths.get(imagePath))) {
                byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                JsonObject inlineData = new JsonObject();
                inlineData.addProperty("mimeType", "image/png");
                inlineData.addProperty("data", base64Image);
                JsonObject imagePart = new JsonObject();
                imagePart.add("inlineData", inlineData);
                parts.add(imagePart);
            }

            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.2);
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
        }

        try (Response response = client.newCall(reqBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response " + response.code() + ": " + response.body().string());
            }
            return response.body().string();
        }
    }

    private String extractTextFromResponse(String jsonResponse) {
        JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);
        boolean isOpenAI = "openai".equalsIgnoreCase(API_FORMAT);

        if (isOpenAI) {
            // OpenAI format: choices[0].message.content
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) return "";
            JsonObject choice = choices.get(0).getAsJsonObject();
            JsonObject message = choice.getAsJsonObject("message");
            if (message == null || !message.has("content")) return "";
            return message.get("content").getAsString();
        } else {
            // Gemini format: candidates[0].content.parts[0].text
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) return "";
            JsonObject candidate = candidates.get(0).getAsJsonObject();
            JsonObject content = candidate.getAsJsonObject("content");
            if (content == null) return "";
            JsonArray parts = content.getAsJsonArray("parts");
            if (parts == null) return "";
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
