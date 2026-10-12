# SS8 — BÀI 4: API Controller tích hợp QuestionAnswerAdvisor

**Công nghệ:** Java 17 · Spring Boot 3.3 · Spring AI 1.0 · pgvector

API `/api/v1/crm/chat` cho phép khách chat với tài liệu CRM. `QuestionAnswerAdvisor` tự động hóa toàn bộ luồng RAG — không phải code thủ công retrieval/augment.

---

## Cấu trúc project

```
ss8/bt4/
├── pom.xml
└── src/main/
    ├── java/com/rikkei/crmchat/
    │   ├── CrmChatApplication.java
    │   ├── dto/ChatDtos.java                     # ChatRequest / ChatResponse / ErrorResponse
    │   ├── config/CorsConfig.java                # CORS tập trung, an toàn
    │   └── controller/CrmChatController.java     # ChatClient + QuestionAnswerAdvisor + Grounding Prompt
    └── resources/application.yml
```

## Điểm chính đã làm

1. **`CrmChatController`** dùng `ChatClient` cấu hình sẵn `QuestionAnswerAdvisor(vectorStore)`. Khi gọi `.call()`, advisor tự: truy vấn vector store (topK=4, threshold=0.5) → nhúng document vào prompt → gọi LLM. Ta không viết bước RAG thủ công.
2. **Grounding Prompt** (`defaultSystem`): ép trợ lý CRM chỉ trả lời dựa trên context, không có thì nói "không tìm thấy trong tài liệu", tuyệt đối không bịa.
3. **CORS an toàn:** chỉ cho phép origin tin cậy (`crm.rikkei.edu.vn`, `localhost:5173`), không dùng `*` — cả ở `@CrossOrigin` lẫn `CorsConfig` tập trung.
4. **Bắt ngoại lệ:** nếu VectorStore/LLM gián đoạn → trả HTTP 503 + `ErrorResponse` thân thiện, không lộ stacktrace.

## Cách chạy & minh chứng

```bash
export OPENAI_API_KEY=sk-...
export DB_HOST=localhost DB_NAME=rikkei_crm DB_USER=postgres DB_PASSWORD=postgres
cd ss8/bt4 && mvn spring-boot:run
```

```bash
curl -X POST localhost:8080/api/v1/crm/chat -H "Content-Type: application/json" \
  -d '{"question":"Chính sách đổi trả hàng của Rikkei Retail là gì?"}'
```

### Log console kỳ vọng (để nộp)

```
[CRM-CHAT] Query: Chính sách đổi trả hàng của Rikkei Retail là gì?
o.s.a.c.c.advisor.vectorstore.QuestionAnswerAdvisor - Retrieved 4 documents for query
   -> [doc-102] "Chính sách đổi trả trong vòng 7 ngày..."
   -> [doc-058] "Điều kiện hoàn tiền..."
[CRM-CHAT] Answer: Theo tài liệu CRM, chính sách đổi trả cho phép ...
```

Bật `logging.level.org.springframework.ai=INFO` để thấy advisor tự nhúng context (số document truy xuất).

## Ghi chú
- Đặt tại `ss8/bt4`.
- Cần PostgreSQL + pgvector và bảng `vector_store` đã nạp tài liệu CRM (Bài 1 của Session 08 lo phần ETL ingest). Nếu chưa có dữ liệu, advisor không tìm được context và trợ lý sẽ trả lời "không tìm thấy trong tài liệu".
- Đề yêu cầu **push GitHub và nộp link repo riêng** — tôi không chạy git/mạng trong môi trường này; bạn tạo repo, push folder `ss8/bt4`, và dán log chạy thật vào bài.
- Package của `QuestionAnswerAdvisor` (`...advisor.vectorstore`) và API `SearchRequest` có thể khác nhẹ giữa các bản Spring AI — đối chiếu phiên bản lớp bạn dùng.
