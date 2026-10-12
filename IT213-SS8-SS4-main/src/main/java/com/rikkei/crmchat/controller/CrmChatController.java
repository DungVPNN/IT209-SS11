package com.rikkei.crmchat.controller;

import com.rikkei.crmchat.dto.ChatDtos.ChatRequest;
import com.rikkei.crmchat.dto.ChatDtos.ChatResponse;
import com.rikkei.crmchat.dto.ChatDtos.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller cho phép khách chat với tài liệu CRM.
 *
 * Điểm mấu chốt:
 *  - Dùng QuestionAnswerAdvisor để TỰ ĐỘNG hóa luồng RAG: advisor tự truy vấn
 *    VectorStore, nhúng các document liên quan vào prompt, rồi gọi LLM. Ta KHÔNG
 *    phải code thủ công từng bước retrieval/augment.
 *  - Grounding Prompt (defaultSystem): ép AI chỉ trả lời dựa trên context được
 *    cung cấp, tuyệt đối không bịa.
 *  - CORS an toàn: chỉ cho phép origin tin cậy (không dùng "*").
 *  - Bắt exception khi VectorStore/LLM gián đoạn -> trả 503 thay vì stacktrace.
 */
@RestController
@RequestMapping("/api/v1/crm")
@CrossOrigin(origins = {"https://crm.rikkei.edu.vn", "http://localhost:5173"},
        methods = {RequestMethod.POST}, maxAge = 3600)
public class CrmChatController {

    private static final Logger log = LoggerFactory.getLogger(CrmChatController.class);

    private static final String GROUNDING_SYSTEM_PROMPT = """
            Bạn là Trợ lý CRM của Rikkei. Nhiệm vụ: trả lời câu hỏi của khách hàng CHỈ dựa trên
            thông tin trong phần context tài liệu được cung cấp kèm theo.
            QUY TẮC:
            - TUYỆT ĐỐI không bịa ra thông tin không có trong context.
            - Nếu context không chứa thông tin để trả lời, hãy nói rõ: "Xin lỗi, tôi không tìm thấy
              thông tin này trong tài liệu CRM hiện có" và đề nghị khách liên hệ nhân viên hỗ trợ.
            - Trả lời ngắn gọn, lịch sự, bằng tiếng Việt.
            """;

    private final ChatClient chatClient;

    public CrmChatController(ChatClient.Builder builder, VectorStore vectorStore) {
        // QuestionAnswerAdvisor tự động truy vấn vector store và nhúng context vào prompt
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(4)
                        .similarityThreshold(0.5)
                        .build())
                .build();

        this.chatClient = builder
                .defaultSystem(GROUNDING_SYSTEM_PROMPT)
                .defaultAdvisors(qaAdvisor)
                .build();
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_REQUEST", "Câu hỏi không được để trống."));
        }

        try {
            log.info("[CRM-CHAT] Query: {}", request.question());

            String answer = chatClient.prompt()
                    .user(request.question())
                    .call()
                    .content();

            log.info("[CRM-CHAT] Answer: {}", answer);
            return ResponseEntity.ok(new ChatResponse(answer));

        } catch (Exception ex) {
            // VectorStore hoặc LLM gián đoạn -> không để lộ stacktrace, trả 503
            log.error("[CRM-CHAT] Lỗi khi xử lý RAG: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("AI_SERVICE_UNAVAILABLE",
                            "Hệ thống AI hoặc kho tài liệu đang gián đoạn. Vui lòng thử lại sau."));
        }
    }
}
